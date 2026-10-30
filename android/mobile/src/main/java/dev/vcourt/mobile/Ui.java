package dev.vcourt.mobile;

import android.content.Context;
import android.graphics.Typeface;
import android.widget.TextView;

/** The few view styles V Court's screens share; the screens are built in code. */
final class Ui {
    private Ui() {
    }

    static TextView title(Context context, String text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(24);
        view.setPadding(0, 0, 0, 16);
        return view;
    }

    static TextView section(Context context, String text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(18);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setPadding(0, 32, 0, 8);
        return view;
    }

    static TextView caption(Context context, String text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(13);
        view.setPadding(0, 16, 0, 0);
        return view;
    }

    static TextView body(Context context, String text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(15);
        return view;
    }
}
