package dev.vcourt.mobile;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import java.util.List;

/** Lists the latest runs: when, what triggered them, their outcome and each round. */
public final class HistoryActivity extends Activity {
    private LinearLayout body;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(32, 40, 32, 32);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(body);
        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        body.removeAllViews();
        body.addView(Ui.title(this, "Run history"));
        List<HistoryStore.Run> runs = new HistoryStore(this).recent(50);
        if (runs.isEmpty()) {
            body.addView(Ui.body(this, "No run yet."));
        }
        for (HistoryStore.Run run : runs) {
            body.addView(Ui.section(this, run.started + " · " + run.trigger
                    + (run.dryRun ? " · dry run" : "")));
            body.addView(Ui.body(this, (run.target == null ? "" : "For " + run.target + ": ")
                    + (run.outcome == null ? "did not finish" : run.outcome)
                    + (run.detail == null || run.detail.equals("null") ? "" : " — " + run.detail)));
            for (String round : run.rounds) {
                body.addView(Ui.body(this, "  " + round));
            }
        }
    }
}
