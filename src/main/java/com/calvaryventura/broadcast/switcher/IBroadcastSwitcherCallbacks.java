package com.calvaryventura.broadcast.switcher;

/**
 * TODO
 */
public interface IBroadcastSwitcherCallbacks
{
    /**
     * TODO
     * @param active
     */
    void fadeToBlackActive(boolean active);

    /**
     * Upstream key.
     * TODO
     * @param active
     */
    void lyricsActive(boolean active);

    /**
     * TODO
     * @param inputIdx
     */
    void activePreviewInputChanged(int inputIdx);

    /**
     * TODO
     * @param inputIdx
     */
    void activeProgramInputChanged(int inputIdx);
}
