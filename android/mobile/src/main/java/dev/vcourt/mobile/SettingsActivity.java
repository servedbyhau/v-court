package dev.vcourt.mobile;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import dev.vcourt.engine.booking.BookingSettings;
import dev.vcourt.engine.booking.Catalog;
import dev.vcourt.engine.booking.Sports;

/**
 * Edits the booking settings: what to book (area, sport, court, slots) and when (opening
 * time, wake-up, warm-up). Saving validates first and shows what is wrong instead.
 */
public final class SettingsActivity extends Activity {
    private static final List<String> SUBDIVISIONS = List.of("", "origami", "bs");

    private SettingsStore store;
    private BookingSettings settings;
    private Spinner subdivision;
    private Spinner sport;
    private Spinner courtChoices;
    private EditText court;
    private EditText slots;
    private EditText startAt;
    private EditText wakeAt;
    private EditText warmUp;
    private EditText timeout;
    private CheckBox english;
    private TextView problems;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new SettingsStore(this);
        settings = store.load();
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(32, 40, 32, 32);

        body.addView(Ui.title(this, "Booking settings"));
        subdivision = spinner(body, "Area (empty: the account's)", SUBDIVISIONS,
                SUBDIVISIONS.indexOf(settings.subdivision));
        List<String> sports = new ArrayList<>(Sports.VIETNAMESE.keySet());
        sport = spinner(body, "Sport", sports, Math.max(0, sports.indexOf(settings.sport)));
        courtChoices = spinner(body, "Court from the catalog", List.of(), 0);
        court = field(body, "Court, as the app names it", settings.court,
                InputType.TYPE_CLASS_TEXT);
        AdapterView.OnItemSelectedListener refresh = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                fillCourts();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        subdivision.setOnItemSelectedListener(refresh);
        sport.setOnItemSelectedListener(refresh);
        courtChoices.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Object chosen = parent.getItemAtPosition(position);
                if (chosen != null && !chosen.toString().isEmpty()) {
                    court.setText(chosen.toString());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        slots = field(body, "Slots, one per line, in order (e.g. 18:00 - 19:00)",
                String.join("\n", settings.slots),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        startAt = field(body, "Opening time (HH:MM)", settings.startAt,
                InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        wakeAt = field(body, "Wake up and prepare at (HH:MM)", settings.wakeAt,
                InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        warmUp = field(body, "Open the utilities list this many seconds before",
                format(settings.warmUpSeconds), InputType.TYPE_CLASS_NUMBER);
        timeout = field(body, "Longest wait for a screen, seconds", format(settings.timeout),
                InputType.TYPE_CLASS_NUMBER);
        english = new CheckBox(this);
        english.setText("Notifications in English");
        english.setChecked("en".equals(settings.notificationLanguage));
        body.addView(english);

        Button save = new Button(this);
        save.setText("Save");
        save.setOnClickListener(view -> save());
        body.addView(save);
        problems = new TextView(this);
        problems.setTextColor(0xffb00020);
        body.addView(problems);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(body);
        setContentView(scroll);
        fillCourts();
    }

    /** Offer the catalog's courts of the chosen sport, in the chosen area or all areas. */
    private void fillCourts() {
        String chosenArea = SUBDIVISIONS.get(subdivision.getSelectedItemPosition());
        String chosenSport = (String) sport.getSelectedItem();
        Set<String> names = new LinkedHashSet<>();
        names.add("");
        for (Catalog.Subdivision area : Catalog.SUBDIVISIONS) {
            if (!chosenArea.isEmpty() && !area.id.equals(chosenArea)) {
                continue;
            }
            Catalog.Sport found = area.sport(chosenSport);
            if (found != null) {
                for (Catalog.Court each : found.courts()) {
                    names.add(each.name);
                }
            }
        }
        List<String> list = new ArrayList<>(names);
        courtChoices.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, list));
        courtChoices.setSelection(Math.max(0, list.indexOf(court.getText().toString())));
    }

    private void save() {
        BookingSettings edited = settings.copy();
        edited.subdivision = SUBDIVISIONS.get(subdivision.getSelectedItemPosition());
        edited.sport = (String) sport.getSelectedItem();
        edited.court = court.getText().toString().strip();
        List<String> lines = new ArrayList<>();
        for (String line : Arrays.asList(slots.getText().toString().split("\n"))) {
            if (!line.strip().isEmpty()) {
                lines.add(line.strip());
            }
        }
        edited.slots = lines;
        edited.startAt = startAt.getText().toString().strip();
        edited.wakeAt = wakeAt.getText().toString().strip();
        edited.warmUpSeconds = number(warmUp, -1);
        edited.timeout = number(timeout, -1);
        edited.notificationLanguage = english.isChecked() ? "en" : "vi";
        List<String> wrong = store.save(edited);
        if (!wrong.isEmpty()) {
            problems.setText("Not saved: " + String.join("; ", wrong));
            return;
        }
        settings = edited;
        SettingsChanged.notify(this);
        finish();
    }

    private Spinner spinner(LinearLayout body, String caption, List<String> items, int chosen) {
        body.addView(Ui.caption(this, caption));
        Spinner spinner = new Spinner(this);
        spinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, items));
        if (chosen >= 0 && chosen < items.size()) {
            spinner.setSelection(chosen);
        }
        body.addView(spinner);
        return spinner;
    }

    private EditText field(LinearLayout body, String caption, String value, int type) {
        body.addView(Ui.caption(this, caption));
        EditText edit = new EditText(this);
        edit.setInputType(type);
        edit.setText(value);
        body.addView(edit);
        return edit;
    }

    private static double number(EditText edit, double fallback) {
        try {
            return Double.parseDouble(edit.getText().toString().strip());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String format(double value) {
        return value == Math.rint(value) ? Long.toString((long) value) : Double.toString(value);
    }
}
