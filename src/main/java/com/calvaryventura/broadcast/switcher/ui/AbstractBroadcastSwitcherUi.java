package com.calvaryventura.broadcast.switcher.ui;

import javax.swing.JPanel;
import java.util.Map;

/**
 * Simple control panel for the video switcher. Contains functions like FadeToBlack, CUT, FADE, etc.
 * Call {@link #setVideoSourceNamesAndSwitcherIndexes(Map)} early in the initialization process
 * which will dynamically create buttons for program and preview, one button for each video source.
 */
public class AbstractBroadcastSwitcherUi extends JPanel
{
    // callbacks for actions the user produces when interacting with the GUI
    protected BroadcastSwitcherUiCallbacks callbacks;

    /**
     * @param callbacks actions the user produces when interacting with the GUI
     */
    public void setCallbacks(BroadcastSwitcherUiCallbacks callbacks)
    {
        this.callbacks = callbacks;
    }

    /**
     * Call this early in the initialization process to set the program/preview buttons in this UI.
     * A program and a preview button are created for each element in this map. Pressing these
     * buttons in the UI triggers that corresponding video source index to be fired in a callback.
     *
     * @param videoSourceNamesAndSwitcherIndexes lists the [name, index] of buttons to create
     */
    public void setVideoSourceNamesAndSwitcherIndexes(Map<String, Integer> videoSourceNamesAndSwitcherIndexes)
    {
    }

    /**
     * @param connected indication if we have established connection with the switcher
     */
    public void setSwitcherConnectionStatus(boolean connected)
    {
    }

    /**
     * @param active indication the transition is in progress
     */
    public void setFadeTransitionInProgressStatus(boolean active)
    {
    }

    /**
     * @param active indication the fade to black is active or inactive
     * @param inTransition indicates we are fading, show the button in yellow
     */
    public void setFadeToBlackStatus(boolean active, boolean inTransition)
    {
    }

    /**
     * @param isMuted indication the master switcher audio is muted
     */
    public void setMuteStatus(boolean isMuted)
    {
    }

    /**
     * @param active indication the lyrics are displayed on-screen
     */
    public void setLyricsStatus(boolean active)
    {
    }

    /**
     * @param previewSourceActive index of the active preview video source
     */
    public void setPreviewSourceStatus(int previewSourceActive)
    {
    }

    /**
     * @param programSourceActive index of the active program video source
     */
    public void setProgramSourceStatus(int programSourceActive)
    {
    }
}
