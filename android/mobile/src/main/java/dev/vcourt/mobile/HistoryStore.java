package dev.vcourt.mobile;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

import dev.vcourt.engine.events.Event;
import dev.vcourt.engine.events.EventKind;
import dev.vcourt.engine.events.EventSink;

/**
 * The run history on the phone: one row per run and per round, written from the run's events.
 * As on the desktop, history never costs a booking: the {@code Reporter} swallows a failing
 * sink. Personal data stays out: the rows hold slots, courts, outcomes and timings only.
 */
final class HistoryStore extends SQLiteOpenHelper implements EventSink {
    private static final int VERSION = 1;
    private static final int KEEP_RUNS = 200;

    /** One run as the history screen shows it. */
    static final class Run {
        long id;
        String started;
        String trigger;
        boolean dryRun;
        String target;
        String outcome;
        String detail;
        final List<String> rounds = new ArrayList<>();
    }

    private long currentRun = -1;

    HistoryStore(Context context) {
        super(context.getApplicationContext(), "history.db", null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE runs (id INTEGER PRIMARY KEY AUTOINCREMENT, started TEXT,"
                + " finished TEXT, cause TEXT, dry_run INTEGER, target TEXT, court TEXT,"
                + " outcome TEXT, detail TEXT)");
        db.execSQL("CREATE TABLE rounds (id INTEGER PRIMARY KEY AUTOINCREMENT, run_id INTEGER,"
                + " slot TEXT, ok INTEGER, unknown INTEGER, detail TEXT, seconds REAL,"
                + " court TEXT, ticket_status TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Version 1 is the first; later versions migrate here.
    }

    @Override
    public synchronized void emit(Event event) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues row = new ContentValues();
        switch (event.kind) {
            case RUN_STARTED:
                row.put("started", event.time.withNano(0).toString());
                row.put("cause", String.valueOf(event.data.get("trigger")));
                row.put("dry_run", Boolean.TRUE.equals(event.data.get("dry_run")) ? 1 : 0);
                currentRun = db.insert("runs", null, row);
                prune(db);
                break;
            case BOOKING_STARTED:
                row.put("target", String.valueOf(event.data.get("date")));
                row.put("court", String.valueOf(event.data.get("court")));
                update(db, row);
                break;
            case SLOT_BOOKED:
            case SLOT_FAILED:
                if (currentRun < 0) {
                    return;
                }
                row.put("run_id", currentRun);
                row.put("slot", String.valueOf(event.data.get("slot")));
                row.put("ok", event.kind == EventKind.SLOT_BOOKED ? 1 : 0);
                row.put("unknown", Boolean.TRUE.equals(event.data.get("unknown")) ? 1 : 0);
                row.put("detail", event.kind == EventKind.SLOT_BOOKED
                        ? (Boolean.TRUE.equals(event.data.get("dry_run")) ? "dry run passed"
                                : "booked")
                        : String.valueOf(event.data.get("reason")));
                Object seconds = event.data.get("seconds");
                row.put("seconds", seconds instanceof Number ? ((Number) seconds).doubleValue()
                        : 0);
                row.put("court", (String) event.data.get("shown_court"));
                row.put("ticket_status", (String) event.data.get("ticket_status"));
                db.insert("rounds", null, row);
                break;
            case RUN_FINISHED:
                row.put("finished", event.time.withNano(0).toString());
                row.put("outcome", String.valueOf(event.data.get("outcome")));
                row.put("detail", String.valueOf(event.data.get("detail")));
                update(db, row);
                currentRun = -1;
                break;
            default:
                break;
        }
    }

    private void update(SQLiteDatabase db, ContentValues row) {
        if (currentRun >= 0) {
            db.update("runs", row, "id = ?", new String[] {Long.toString(currentRun)});
        }
    }

    private static void prune(SQLiteDatabase db) {
        db.execSQL("DELETE FROM rounds WHERE run_id NOT IN (SELECT id FROM runs ORDER BY id"
                + " DESC LIMIT " + KEEP_RUNS + ")");
        db.execSQL("DELETE FROM runs WHERE id NOT IN (SELECT id FROM runs ORDER BY id DESC"
                + " LIMIT " + KEEP_RUNS + ")");
    }

    /** Return the newest {@code limit} runs, newest first, with a line per round. */
    synchronized List<Run> recent(int limit) {
        List<Run> runs = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT id, started, cause, dry_run, target, outcome,"
                + " detail FROM runs ORDER BY id DESC LIMIT " + limit, null)) {
            while (c.moveToNext()) {
                Run run = new Run();
                run.id = c.getLong(0);
                run.started = c.getString(1);
                run.trigger = c.getString(2);
                run.dryRun = c.getInt(3) != 0;
                run.target = c.getString(4);
                run.outcome = c.getString(5);
                run.detail = c.getString(6);
                runs.add(run);
            }
        }
        for (Run run : runs) {
            try (Cursor c = db.rawQuery("SELECT slot, ok, unknown, detail, seconds, court FROM"
                    + " rounds WHERE run_id = ? ORDER BY id", new String[] {
                        Long.toString(run.id)})) {
                while (c.moveToNext()) {
                    String mark = c.getInt(1) != 0 ? "✓" : c.getInt(2) != 0 ? "?" : "✗";
                    String court = c.isNull(5) ? "" : " on " + c.getString(5);
                    run.rounds.add(mark + " " + c.getString(0) + court + ": " + c.getString(3)
                            + String.format(" (%.1f s)", c.getDouble(4)));
                }
            }
        }
        return runs;
    }
}
