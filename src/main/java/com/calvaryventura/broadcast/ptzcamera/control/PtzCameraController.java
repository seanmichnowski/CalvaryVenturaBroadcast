package com.calvaryventura.broadcast.ptzcamera.control;

import java.net.InetSocketAddress;
import java.util.function.Consumer;

/**
 * Class responsible for translating camera motion commands via method calls
 * into the actual PTZ string commands, and sending them out to the HTTP interface.
 */
public class PtzCameraController
{
    private static final int PAN_MAX_SPEED   = 10; // can be up to 17
    private static final int TILT_MAX_SPEED  = 10; // can be up to 17
    private static final int ZOOM_MAX_SPEED  = 7;
    private final PtzCameraViscaTcpNetworkInterface viscaTcpNetworkInterface;

    /**
     * @param displayName human-readable name designator of this camera
     * @param socketAddress PTZ camera IP/port for its network VISCA interface
     * @param cameraConnectionStatus notifies the parent of the camera being connected to the TCP VISCA port
     */
    public PtzCameraController(String displayName, InetSocketAddress socketAddress, Consumer<Boolean> cameraConnectionStatus)
    {
        this.viscaTcpNetworkInterface = new PtzCameraViscaTcpNetworkInterface(displayName, socketAddress.getHostString(), socketAddress.getPort(), cameraConnectionStatus);
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
     * @param presetIndex index of the preset to move the camera to
     * @return successful command execution
     */
    public boolean savePreset(int presetIndex)
    {
        presetIndex &= 0x0F; // we can only ever have up to 16 presets, and the first four bits must be 0's
        final byte[] setPresetCommand = new byte[]{(byte) 0x81, 0x01, 0x04, 0x3F, 0x01, (byte) presetIndex, (byte) 0xFF};
        return this.viscaTcpNetworkInterface.send(setPresetCommand);
    }

    /**
     * @param presetIndex index of the preset to move the camera to
     * @return successful command execution
     */
    public boolean moveToPreset(int presetIndex)
    {
        presetIndex &= 0x0F; // we can only ever have up to 16 presets, and the first four bits must be 0's
        final byte[] setPresetCommand = new byte[]{(byte) 0x81, 0x01, 0x04, 0x3F, 0x02, (byte) presetIndex, (byte) 0xFF};
        return this.viscaTcpNetworkInterface.send(setPresetCommand);
    }
}
