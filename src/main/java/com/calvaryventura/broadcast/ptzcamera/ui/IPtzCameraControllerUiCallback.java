package com.calvaryventura.broadcast.ptzcamera.ui;

/**
 * Simple enum which defines the various states
 * for each of the camera preset entries.
 */
public interface IPtzCameraControllerUiCallback
{
    /**
     * Set...
     * @param ptzCameraIdx index of the PTZ camera we are manipulating
     * @param presetIdx
     * @return
     */
    boolean setPressed(int ptzCameraIdx, int presetIdx);

    /**
     * Preview...
     * @param ptzCameraIdx index of the PTZ camera we are manipulating
     * @param presetIdx
     */
    void callPressed(int ptzCameraIdx, int presetIdx);

    /**
     * Moving...
     * @param ptzCameraIdx index of the PTZ camera we are manipulating
     * @param pan
     * @param tilt
     * @return
     */
    boolean panTilt(int ptzCameraIdx, double pan, double tilt);

    /**
     * Zoom...
     * @param ptzCameraIdx index of the PTZ camera we are manipulating
     * @param zoom
     * @return
     */
    boolean zoom(int ptzCameraIdx, double zoom);
}
