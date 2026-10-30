package dev.vcourt.engine.driver;

/** The driver's answer to {@code hello}: its protocol, version and Android SDK level. */
public final class Hello {
    private final int protocol;
    private final String version;
    private final int sdk;

    public Hello(int protocol, String version, int sdk) {
        this.protocol = protocol;
        this.version = version;
        this.sdk = sdk;
    }

    public int protocol() {
        return protocol;
    }

    /** Return the driver's version name, the hash of the sources it was built from. */
    public String version() {
        return version;
    }

    public int sdk() {
        return sdk;
    }

    @Override
    public String toString() {
        return "Driver " + version + " (protocol " + protocol + ", SDK " + sdk + ")";
    }
}
