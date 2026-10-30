package dev.vcourt.mobile;

import java.util.List;

import dev.vcourt.engine.flow.RoundResult;

/**
 * Words a run's outcome for the notification and the history, in the language the settings
 * chose. Booked means the ticket showed; an unknown outcome is named as such, never as a
 * failure alone, since the app may have booked.
 */
final class RunSummary {
    final String outcome;
    final String title;
    final String text;

    private RunSummary(String outcome, String title, String text) {
        this.outcome = outcome;
        this.title = title;
        this.text = text;
    }

    static RunSummary of(List<RoundResult> results, boolean dryRun, String error,
            boolean english) {
        int ok = 0;
        int unknown = 0;
        StringBuilder lines = new StringBuilder();
        for (RoundResult result : results) {
            if (result.ok) {
                ok++;
            } else if (result.unknown) {
                unknown++;
            }
            String mark = result.ok ? "✓" : result.unknown ? "?" : "✗";
            lines.append(mark).append(' ').append(result.slot).append(": ")
                    .append(result.detail).append('\n');
        }
        if (error != null) {
            lines.append(error);
        }
        String text = lines.toString().trim();
        int total = results.size();
        String outcome;
        String title;
        if (error != null && total == 0) {
            outcome = "error";
            title = english ? "V Court: the run failed" : "V Court: lần chạy bị lỗi";
        } else if (unknown > 0) {
            outcome = "unknown";
            title = english ? "V Court: check the app, outcome unknown"
                    : "V Court: kiểm tra app, chưa rõ kết quả";
        } else if (ok == total && total > 0) {
            outcome = dryRun ? "dry_run" : "booked";
            title = dryRun ? (english ? "V Court: dry run passed" : "V Court: chạy thử thành công")
                    : (english ? "V Court: booked " + ok + "/" + total
                            : "V Court: đã đặt " + ok + "/" + total);
        } else if (ok > 0) {
            outcome = "partial";
            title = english ? "V Court: booked " + ok + "/" + total
                    : "V Court: đặt được " + ok + "/" + total;
        } else {
            outcome = "failed";
            title = english ? "V Court: nothing booked" : "V Court: không đặt được";
        }
        return new RunSummary(outcome, title, text);
    }
}
