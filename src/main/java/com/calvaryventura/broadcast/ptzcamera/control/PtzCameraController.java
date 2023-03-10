package com.calvaryventura.broadcast.ptzcamera.control;

import java.util.function.Consumer;

/**
 * Class responsible for translating camera motion commands via method calls
 * into the actual PTZ string commands, and sending them out to the HTTP interface.
 */
public class PtzCameraController
{
    private static final int PAN_MAX_SPEED   = 17;
    private static final int TILT_MAX_SPEED  = 17;
    private static final int ZOOM_MAX_SPEED  = 7;
    private static final int FOCUS_MAX_SPEED = 7;
    private final PtzCameraViscaTcpNetworkInterface viscaTcpNetworkInterface;

    /**
     * @param displayName human-readable name designator of this camera
     * @param ipAddress address of the PTZ camera
     * @param port port of the PTZ camera's VISCA interface
     */
    public PtzCameraController(String displayName, String ipAddress, int port)
    {
        this.viscaTcpNetworkInterface = new PtzCameraViscaTcpNetworkInterface(displayName, ipAddress, port);
    }

    /**
     * @param cameraConnectionStatus notifies the parent of the camera being connected to the TCP VISCA port
     */
    public void onCameraConnectionStatus(Consumer<Boolean> cameraConnectionStatus)
    {
        this.viscaTcpNetworkInterface.addConnectionListener(cameraConnectionStatus);
    }

    /**
     * @param panSpeed  (+) is right, (-) is left, 0 is stop
     * @param tiltSpeed (+) is up, (-) is down, 0 is stop
     */
    public boolean panAndTilt(double panSpeed, double tiltSpeed)
    {
        final byte cmd1;
        final byte cmd2;
        if (panSpeed == 0 && tiltSpeed > 0) // UP
        {
            cmd1 = 3;
            cmd2 = 1;
        } else if (panSpeed == 0 && tiltSpeed < 0) // DOWN
        {
            cmd1 = 3;
            cmd2 = 2;
        } else if (panSpeed > 0 && tiltSpeed == 0) // RIGHT
        {
            cmd1 = 2;
            cmd2 = 3;
        } else if (panSpeed < 0 && tiltSpeed == 0) // LEFT
        {
            cmd1 = 1;
            cmd2 = 3;
        } else if (panSpeed < 0 && tiltSpeed > 0) // UPLEFT
        {
            cmd1 = 1;
            cmd2 = 1;
        } else if (panSpeed > 0 && tiltSpeed > 0) // UPRIGHT
        {
            cmd1 = 2;
            cmd2 = 1;
        } else if (panSpeed < 0 && tiltSpeed < 0) // DOWNLEFT
        {
            cmd1 = 1;
            cmd2 = 2;
        } else if (panSpeed > 0 && tiltSpeed < 0) // DOWNRIGHT
        {
            cmd1 = 2;
            cmd2 = 2;
        } else // STOP, pan and tilt speeds are both 0
        {
            cmd1 = 3;
            cmd2 = 3;
        }
        final byte pan = (byte) (Math.abs(panSpeed) * PAN_MAX_SPEED);
        final byte tilt = (byte) (Math.abs(tiltSpeed) * TILT_MAX_SPEED);
        final byte[] panTiltCommand = new byte[]{(byte) 0x81, 0x01, 0x06, 0x01, pan, tilt, cmd1, cmd2, (byte) 0xFF};
        return this.viscaTcpNetworkInterface.send(panTiltCommand);
    }

    /**
     * @param zoomSpeed speed of focusing (+) is in, (-) is out, 0 is stop
     * @return successful command execution
     */
    public boolean changeZoom(double zoomSpeed)
    {
        final byte speed = (byte) ((byte) (Math.abs(zoomSpeed) * ZOOM_MAX_SPEED) & 0x0F);
        final byte zoom = (byte) (zoomSpeed > 0 ? (0x20 | speed) : zoomSpeed < 0 ? (0x30 | speed) : 0x00);
        final byte[] zoomCommand = new byte[]{(byte) 0x81, 0x01, 0x04, 0x07, zoom, (byte) 0xFF};
        return this.viscaTcpNetworkInterface.send(zoomCommand);
    }

    /**
     * @param focusSpeed speed of focusing (+) is in, (-) is out, 0 is stop
     * @return successful command execution
     */
    public boolean changeFocus(double focusSpeed)
    {
        final byte speed = (byte) ((byte) (Math.abs(focusSpeed) * FOCUS_MAX_SPEED) & 0x0F);
        final byte focus = (byte) (focusSpeed > 0 ? (0x20 | speed) : focusSpeed < 0 ? (0x30 | speed) : 0x00);
        final byte[] focusCommand = new byte[]{(byte) 0x81, 0x01, 0x04, 0x08, focus, (byte) 0xFF};
        return this.viscaTcpNetworkInterface.send(focusCommand);
    }

    /**
     * @param presetIndex index of the preset to move the camera to
     * @return successful command execution
     */
    public boolean setPreset(int presetIndex)
    {
        presetIndex &= 0x0F; // we can only ever have up to 16 presets, and the first four bits must be 0's
        final byte[] setPresetCommand = new byte[]{(byte) 0x81, 0x01, 0x04, 0x3F, 0x01, (byte) presetIndex, (byte) 0xFF};
        return this.viscaTcpNetworkInterface.send(setPresetCommand);
    }

    /**
     * @param presetIndex index of the preset to move the camera to
     * @return successful command execution
     */
    public boolean callPreset(int presetIndex)
    {
        presetIndex &= 0x0F; // we can only ever have up to 16 presets, and the first four bits must be 0's
        final byte[] setPresetCommand = new byte[]{(byte) 0x81, 0x01, 0x04, 0x3F, 0x02, (byte) presetIndex, (byte) 0xFF};
        return this.viscaTcpNetworkInterface.send(setPresetCommand);
    }
}
