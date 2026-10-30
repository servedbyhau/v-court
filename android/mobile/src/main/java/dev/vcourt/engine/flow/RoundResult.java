package dev.vcourt.engine.flow;

/** The outcome of one booking round. */
public final class RoundResult {
    public final String slot;
    public final boolean ok;
    public final String detail;
    public final double seconds;
    /** The court the confirmation page named, if read: the one booked. */
    public final String court;
    /** Whether the round may have booked although it failed: the outcome is unknown. */
    public final boolean unknown;

    public RoundResult(String slot, boolean ok, String detail, double seconds, String court,
            boolean unknown) {
        this.slot = slot;
        this.ok = ok;
        this.detail = detail;
        this.seconds = seconds;
        this.court = court;
        this.unknown = unknown;
    }
}
