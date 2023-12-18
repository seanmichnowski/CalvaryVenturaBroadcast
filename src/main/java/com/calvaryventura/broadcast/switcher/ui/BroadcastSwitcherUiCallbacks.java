package com.calvaryventura.broadcast.switcher.ui;

/**
 * Callbacks coming FROM the GUI for the broadcast video switcher,
 * notifies the parent when the user is interacting with this GUI.
 */
public interface BroadcastSwitcherUiCallbacks
{
    void onPreviewSourceChanged(int previewSourceChanged);

    void onProgramSourceChanged(int programSourceChanged);

    void onFadeToBlack();

    void onLyricsEnabled();

    void onFadePressed();

    void onCutPressed();
}
