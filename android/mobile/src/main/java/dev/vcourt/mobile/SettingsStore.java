package dev.vcourt.mobile;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.vcourt.engine.booking.BookingSettings;
import dev.vcourt.engine.booking.Labels;

/**
 * Stores the booking settings and the schedule switch as one JSON document in private
 * preferences. Loading never validates and falls back to the defaults key by key, so a stored
 * setting never stops the morning run; saving validates first, so a bad edit never reaches it.
 * Only labels that differ from the defaults are stored, so a later fix to a default reaches a
 * stored file too (the desktop's {@code save_config} rule).
 */
final class SettingsStore {
    private static final String TAG = "VCourtSettings";
    private static final String PREFERENCES = "settings";
    private static final String KEY = "booking";
    private static final String SCHEDULED = "scheduled";

    private final SharedPreferences preferences;

    SettingsStore(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    /** Return the stored settings, or the defaults where nothing (readable) is stored. */
    BookingSettings load() {
        BookingSettings settings = new BookingSettings();
        String stored = preferences.getString(KEY, null);
        if (stored == null) {
            return settings;
        }
        try {
            apply(settings, new JSONObject(stored));
        } catch (JSONException e) {
            Log.w(TAG, "Stored settings unreadable; using the defaults", e);
        }
        return settings;
    }

    /**
     * Validate and store {@code settings}; return what is wrong instead when anything is.
     */
    List<String> save(BookingSettings settings) {
        List<String> problems = settings.validate();
        if (!problems.isEmpty()) {
            return problems;
        }
        try {
            preferences.edit().putString(KEY, toJson(settings).toString()).apply();
        } catch (JSONException e) {
            problems.add("could not store the settings: " + e.getMessage());
        }
        return problems;
    }

    /** Tell whether the daily run is switched on. */
    boolean scheduled() {
        return preferences.getBoolean(SCHEDULED, false);
    }

    void setScheduled(boolean on) {
        preferences.edit().putBoolean(SCHEDULED, on).apply();
    }

    static JSONObject toJson(BookingSettings s) throws JSONException {
        BookingSettings defaults = new BookingSettings();
        JSONObject json = new JSONObject()
                .put("package", s.appPackage)
                .put("slots", new JSONArray(s.slots))
                .put("subdivision", s.subdivision)
                .put("sport", s.sport)
                .put("court", s.court)
                .put("venue_keyword", s.venueKeyword)
                .put("timeout", s.timeout)
                .put("max_swipes", s.maxSwipes)
                .put("boot_timeout", s.bootTimeout)
                .put("restart_app", s.restartApp)
                .put("start_at", s.startAt)
                .put("wake_at", s.wakeAt)
                .put("ntp_server", s.ntpServer)
                .put("open_retry_seconds", s.openRetrySeconds)
                .put("warm_up_seconds", s.warmUpSeconds)
                .put("notification_language", s.notificationLanguage)
                .put("next_month_xy", new JSONArray(s.nextMonthXy))
                .put("prev_month_xy", new JSONArray(s.prevMonthXy));
        JSONObject labels = new JSONObject();
        for (Map.Entry<String, String> label : s.labels.asMap().entrySet()) {
            if (!label.getValue().equals(defaults.labels.get(label.getKey()))) {
                labels.put(label.getKey(), label.getValue());
            }
        }
        if (labels.length() > 0) {
            json.put("labels", labels);
        }
        return json;
    }

    /** Apply each readable key of {@code json}; a key that does not read keeps its default. */
    static void apply(BookingSettings s, JSONObject json) {
        s.appPackage = json.optString("package", s.appPackage);
        JSONArray slots = json.optJSONArray("slots");
        if (slots != null) {
            List<String> read = new ArrayList<>();
            for (int i = 0; i < slots.length(); i++) {
                read.add(slots.optString(i));
            }
            s.slots = read;
        }
        s.subdivision = json.optString("subdivision", s.subdivision);
        s.sport = json.optString("sport", s.sport);
        s.court = json.optString("court", s.court);
        s.venueKeyword = json.optString("venue_keyword", s.venueKeyword);
        s.timeout = json.optDouble("timeout", s.timeout);
        s.maxSwipes = json.optInt("max_swipes", s.maxSwipes);
        s.bootTimeout = json.optDouble("boot_timeout", s.bootTimeout);
        s.restartApp = json.optBoolean("restart_app", s.restartApp);
        s.startAt = json.optString("start_at", s.startAt);
        s.wakeAt = json.optString("wake_at", s.wakeAt);
        s.ntpServer = json.optString("ntp_server", s.ntpServer);
        s.openRetrySeconds = json.optDouble("open_retry_seconds", s.openRetrySeconds);
        s.warmUpSeconds = json.optDouble("warm_up_seconds", s.warmUpSeconds);
        s.notificationLanguage = json.optString("notification_language",
                s.notificationLanguage);
        s.nextMonthXy = point(json.optJSONArray("next_month_xy"), s.nextMonthXy);
        s.prevMonthXy = point(json.optJSONArray("prev_month_xy"), s.prevMonthXy);
        JSONObject labels = json.optJSONObject("labels");
        if (labels != null) {
            Map<String, String> changes = new LinkedHashMap<>();
            for (Iterator<String> keys = labels.keys(); keys.hasNext();) {
                String key = keys.next();
                if (Labels.KEYS.contains(key)) { // an unknown label is dropped, not fatal
                    changes.put(key, labels.optString(key));
                }
            }
            s.labels = Labels.of(s.labels, changes);
        }
    }

    private static double[] point(JSONArray array, double[] fallback) {
        if (array == null || array.length() != 2) {
            return fallback;
        }
        return new double[] {array.optDouble(0, fallback[0]), array.optDouble(1, fallback[1])};
    }
}
