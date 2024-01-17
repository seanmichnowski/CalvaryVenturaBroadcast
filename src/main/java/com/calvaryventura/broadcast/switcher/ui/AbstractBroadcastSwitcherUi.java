package com.calvaryventura.broadcast.switcher.ui;

import com.calvaryventura.broadcast.settings.BroadcastSettings;
import com.calvaryventura.broadcast.uiwidgets.PopupVolumeUi;

import javax.swing.JPanel;
import java.util.Map;

/**
 * Simple control panel for the video switcher. Contains functions like FadeToBlack, CUT, FADE, etc.
 * Call {@link #setVideoSourceNamesAndSwitcherIndexes(Map)} early in the initialization process
 * which will dynamically create buttons for program and preview, one button for each video source.
 */
public class AbstractBroadcastSwitcherUi extends JPanel
{
    private final PopupVolumeUi popupVolumeUi = new PopupVolumeUi();

    // callbacks for actions the user produces when interacting with the GUI
    protected BroadcastSwitcherUiCallbacks callbacks;
    protected BroadcastSettings broadcastSettings;

    /**
     * @param settings general settings for this program
     */
    protected AbstractBroadcastSwitcherUi(BroadcastSettings settings)
    {
        this.broadcastSettings = settings;

        // initialize the volume meter limits based on settings
        this.popupVolumeUi.setVolumeMeterLimits(settings.getMinAudioLevelDb(), settings.getWarnAudioLevelDb(),
                settings.getHighAudioLevelDb(), settings.getMaxAudioLevelDb());
    }

    /**
     * @param callbacks actions the user produces when interacting with the GUI
     */
    public void setCallbacks(BroadcastSwitcherUiCallbacks callbacks)
    {
        this.callbacks = callbacks;

        // connect some callbacks to the popup volume meter
        this.popupVolumeUi.setFaderMovedAction(faderPercent -> this.callbacks.setAudioLevelPercent(faderPercent));
        this.popupVolumeUi.setPopupShownHiddenAction(isShowing -> this.callbacks.setSwitcherSendingLiveAudio(isShowing));
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
     * @param leftLevelDb master live audio level LEFT
     * @param rightLevelDb master live audio level RIGHT
     */
    public void setLiveAudioLevel(double leftLevelDb, double rightLevelDb)
    {
        this.popupVolumeUi.setLiveVolumeMeterLevel(leftLevelDb, rightLevelDb);
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

    /**
     * When the "Volume" button is pressed, show the volume popup.
     */
    protected void showVolumePopup()
    {
        this.popupVolumeUi.showVolumePopup();
    }
}
