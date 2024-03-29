package com.calvaryventura.broadcast.ptzcamera.ui;

/**
 * Simple enum which defines the various states
 * for each of the camera preset entries.
 */
public interface IPtzCameraUiCallbacks
{
    /**
     * Set...
     * @param presetIdx
     * @return
     */
    boolean setPressed(int presetIdx);

    /**
     * Preview...
     * @param presetIdx
     * @return
     */
    boolean callPressed(int presetIdx);

    /**
     * Moving...
     * @param pan
     * @param tilt
     * @return
     */
    boolean panTilt(double pan, double tilt);

    /**
     * Zoom...
     * @param zoom
     * @return
     */
    boolean zoom(double zoom);
}
