package com.calvaryventura.broadcast.ptz.commander;

import javax.swing.*;

/**
 * Class responsible for translating camera motion commands via method calls
 * into the actual PTZ string commands, and sending them out to the HTTP interface.
 */
public class PtzCameraCommandSender
{
    private static final int PAN_MAX_SPEED   = 6; // 24 highest based on camera's web application
    private static final int TILT_MAX_SPEED  = 5; // 20 highest
    private static final int ZOOM_MAX_SPEED  = 5;  // 7 is max based on camera's web application
    private static final int FOCUS_MAX_SPEED = 5;  // 7 is max based on camera's web application
    private final PtzCameraHttpInterface cameraInterface;

    /**
     * @param ipAddress address of the camera the instance of this class is controlling
     * @param connectionStatusLabel label to update with status of the connection
     */
    public PtzCameraCommandSender(String ipAddress, JLabel connectionStatusLabel)
    {
        this.cameraInterface = new PtzCameraHttpInterface(ipAddress, connectionStatusLabel);
        this.cameraInterface.sendHttpGetCommand("ptzcmd&ptzstop&0&0");
    }

    /**
     * @param panSpeed  (+) is right, (-) is left, 0 is stop
     * @param tiltSpeed (+) is up, (-) is down, 0 is stop
     */
    public boolean panAndTilt(double panSpeed, double tiltSpeed)
    {
        if (panSpeed == 0 && tiltSpeed == 0)
        {
            return cameraInterface.sendHttpGetCommand("ptzcmd&ptzstop&0&0");
        } else
        {
            final String directionPan = panSpeed > 0 ? "right" : panSpeed < 0 ? "left" : "";
            final String directionTilt = tiltSpeed > 0 ? "up" : tiltSpeed < 0 ? "down" : "";
            final int pan = (int) (Math.abs(panSpeed) * PAN_MAX_SPEED);
            final int tilt = (int) (Math.abs(tiltSpeed) * TILT_MAX_SPEED);
            final String command = String.format("ptzcmd&%s&%d&%d", directionPan + directionTilt, pan, tilt);
            return cameraInterface.sendHttpGetCommand(command);
        }
    }

    /**
     * @param zoomSpeed speed of focusing (+) is in, (-) is out, 0 is stop
     * @return successful command execution
     */
    public boolean changeZoom(double zoomSpeed)
    {
        if (zoomSpeed != 0)
        {
            final String direction = zoomSpeed > 0 ? "zoomin" : "zoomout";
            final int speed = (int) (Math.abs(zoomSpeed) * ZOOM_MAX_SPEED);
            final String command = String.format("ptzcmd&%s&%d", direction, speed);
            return cameraInterface.sendHttpGetCommand(command);
        } else
        {
            return cameraInterface.sendHttpGetCommand("ptzcmd&zoomstop&0");
        }
    }

    /**
     * @param focusSpeed speed of focusing (+) is in, (-) is out, 0 is stop
     * @return successful command execution
     */
    public boolean changeFocus(double focusSpeed)
    {
        if (focusSpeed != 0)
        {
            final String direction = focusSpeed > 0 ? "focusin" : "focusout";
            final int speed = (int) (Math.abs(focusSpeed) * FOCUS_MAX_SPEED);
            final String command = String.format("ptzcmd&%s&%d", direction, speed);
            return cameraInterface.sendHttpGetCommand(command);
        } else
        {
            return cameraInterface.sendHttpGetCommand("ptzcmd&focusstop&0");
        }
    }

    /**
     * @param presetIndex index of the preset to move the camera to
     * @return successful command execution
     */
    public boolean setPreset(int presetIndex)
    {
        return cameraInterface.sendHttpGetCommand(String.format("ptzcmd&posset&%d", presetIndex));
    }

    /**
     * @param presetIndex index of the preset to move the camera to
     * @return successful command execution
     */
    public boolean callPreset(int presetIndex)
    {
        return cameraInterface.sendHttpGetCommand(String.format("ptzcmd&poscall&%d", presetIndex));
    }
}
