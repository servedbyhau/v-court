package dev.vcourt.mobile;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import rikka.shizuku.Shizuku;

/** Minimal native shell for checking Shizuku readiness before the driver session exists. */
public final class MainActivity extends Activity {
    private TextView status;
    private final Shizuku.OnRequestPermissionResultListener permissionListener =
            (requestCode, grantResult) -> runOnUiThread(this::showStatus);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(32, 40, 32, 32);

        TextView title = new TextView(this);
        title.setText("V Court");
        title.setTextSize(26);
        body.addView(title);

        status = new TextView(this);
        status.setTextSize(16);
        status.setPadding(0, 24, 0, 24);
        body.addView(status);

        Button authorize = new Button(this);
        authorize.setText("Authorize Shizuku");
        authorize.setOnClickListener(view -> {
            if (Shizuku.pingBinder()
                    && Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(1);
            }
            showStatus();
        });
        body.addView(authorize);
        setContentView(body);
        Shizuku.addRequestPermissionResultListener(permissionListener);
        showStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        showStatus();
    }

    @Override
    protected void onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permissionListener);
        super.onDestroy();
    }

    private void showStatus() {
        if (status == null) return;
        if (!Shizuku.pingBinder()) {
            status.setText("Shizuku is not running. Start it before authorizing V Court.");
        } else if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            status.setText("Shizuku permission is required.");
        } else {
            status.setText("Shizuku is ready. Driver session setup is the next step.");
        }
    }
}
