package dev.vcourt.mobile;

import dev.vcourt.engine.driver.ClickResult;
import dev.vcourt.engine.driver.DriverClient;
import dev.vcourt.engine.driver.DriverException;
import dev.vcourt.engine.driver.DriverNotRespondingException;
import dev.vcourt.engine.flow.Device;
import dev.vcourt.engine.flow.DeviceException;
import dev.vcourt.engine.flow.DeviceNotRespondingException;

/**
 * The booking flow's {@link Device}, carried out by the driver of an open session, as the
 * desktop's {@code DriverDevice}: screens, clicks, Back and swipes go to the driver; app
 * starts, stops and the quiet settings go through the Shizuku shell service. Every driver
 * command is bounded by {@link DriverClient#COMMAND_TIMEOUT_MILLIS}, the shell commands by
 * {@link ShellService}'s own limit; a timeout ends the run after its round.
 */
final class DriverDevice implements Device {
    private final DriverClient client;
    private final ShizukuDriverLauncher shell;

    DriverDevice(DriverClient client, ShizukuDriverLauncher shell) {
        this.client = client;
        this.shell = shell;
    }

    @Override
    public String dump() throws DeviceException {
        try {
            return client.dump();
        } catch (DriverException e) {
            throw wrap(e);
        }
    }

    /**
     * Click the element labelled {@code label} where it is now. One no longer shown is not
     * clicked: whatever is at its old place is another element, and the flow reads again.
     */
    @Override
    public void click(int x, int y, String label) throws DeviceException {
        try {
            ClickResult result = label.isEmpty() ? client.clickAt(x, y)
                    : client.click(label, x, y);
            if (!result.clicked()) {
                java.util.logging.Logger.getLogger(DriverDevice.class.getName())
                        .fine((label.isEmpty() ? "No element" : "'" + label + "' is gone")
                                + "; nothing clicked");
            }
        } catch (DriverException e) {
            throw wrap(e);
        }
    }

    @Override
    public void back() throws DeviceException {
        try {
            client.back();
        } catch (DriverException e) {
            throw wrap(e);
        }
    }

    @Override
    public void swipe(int x1, int y1, int x2, int y2, long millis) throws DeviceException {
        try {
            client.swipe(x1, y1, x2, y2, millis);
        } catch (DriverException e) {
            throw wrap(e);
        }
    }

    @Override
    public void startApp(String pkg) throws DeviceException {
        shell(ShellService.START_APP, pkg);
    }

    @Override
    public void stopApp(String pkg) throws DeviceException {
        shell(ShellService.STOP_APP, pkg);
    }

    @Override
    public String currentPackage() throws DeviceException {
        try {
            return client.currentPackage();
        } catch (DriverException e) {
            throw wrap(e);
        }
    }

    @Override
    public void quiet() throws DeviceException {
        shell(ShellService.QUIET, null);
    }

    private void shell(int op, String pkg) throws DeviceException {
        try {
            shell.appCommand(op, pkg);
        } catch (DriverException e) {
            throw new DeviceException(e.getMessage(), e);
        }
    }

    private static DeviceException wrap(DriverException e) {
        if (e instanceof DriverNotRespondingException) {
            return new DeviceNotRespondingException(e.getMessage(), e);
        }
        return new DeviceException(e.getMessage(), e);
    }
}
