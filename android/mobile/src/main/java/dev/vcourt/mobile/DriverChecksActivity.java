package dev.vcourt.mobile;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import rikka.shizuku.Shizuku;

/**
 * Runs the driver checks, the one-APK proof ({@link OneApkProof}) and the Vinhomes read check
 * ({@link VinhomesReadCheck}), against a test target on this same screen: phase A's device
 * evidence, and a diagnosis when a run cannot reach the driver.
 */
public final class DriverChecksActivity extends Activity {
    private TextView status;
    private Button run;
    private Button read;
    private Button target;
    private TextView report;
    private final AtomicInteger taps = new AtomicInteger();
    private final ExecutorService proofs = Executors.newSingleThreadExecutor();
    private final Shizuku.OnRequestPermissionResultListener permissionListener =
            (requestCode, grantResult) -> runOnUiThread(this::showStatus);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(32, 40, 32, 32);

        TextView title = new TextView(this);
        title.setText("Driver checks");
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

        run = new Button(this);
        run.setText("Run one-APK proof");
        run.setOnClickListener(view -> startCheck(
                new OneApkProof(this, taps::get, this::showReportLine)));
        body.addView(run);

        read = new Button(this);
        read.setText("Read Vinhomes screen");
        read.setOnClickListener(view -> startCheck(
                new VinhomesReadCheck(this, taps::get, this::showReportLine)));
        body.addView(read);

        // The proof finds this button by its content description, as the booking flow finds
        // the target app's elements, and checks that its tap arrived.
        target = new Button(this);
        target.setContentDescription(DriverContract.TEST_TARGET);
        target.setOnClickListener(view -> showTaps(taps.incrementAndGet()));
        showTaps(0);
        body.addView(target);

        report = new TextView(this);
        report.setTypeface(Typeface.MONOSPACE);
        report.setTextSize(12);
        report.setTextIsSelectable(true);
        report.setPadding(0, 24, 0, 0);
        body.addView(report);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(body);
        setContentView(scroll);
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
        proofs.shutdown();
        super.onDestroy();
    }

    private void startCheck(Runnable check) {
        run.setEnabled(false);
        read.setEnabled(false);
        report.setText("");
        proofs.execute(() -> {
            check.run();
            runOnUiThread(() -> {
                run.setEnabled(true);
                read.setEnabled(true);
            });
        });
    }

    private void showReportLine(String line) {
        runOnUiThread(() -> report.append(line + "\n"));
    }

    private void showTaps(int count) {
        target.setText("Test target (taps: " + count + ")");
    }

    private void showStatus() {
        if (status == null) return;
        if (!Shizuku.pingBinder()) {
            status.setText("Shizuku is not running. Start it before authorizing V Court.");
        } else if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            status.setText("Shizuku permission is required.");
        } else {
            status.setText("Shizuku is ready. Run a check to start the driver.");
        }
    }
}
