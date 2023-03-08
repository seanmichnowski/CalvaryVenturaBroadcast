package com.calvaryventura.broadcast.switcher.ui;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Simple control panel for the video switcher. Contains functions like FadeToBlack, CUT, etc.
 * Call {@link #setVideoSourceNamesAndSwitcherIndexes(Map)} early in the initialization process
 * which will dynamically create buttons for program and preview, one button for each video source.
 */
public class BroadcastSwitcherUi extends JPanel
{
    // these are fired when the user presses UI buttons to change the video switcher
    private Consumer<Integer> previewSourceChanged;
    private Consumer<Integer> programSourceChanged;
    private Consumer<Boolean> fadeToBlackPressed;
    private Consumer<Boolean> lyricsPressed;
    private Consumer<Boolean> autoPressed;
    private Consumer<Boolean> cutPressed;

    // each entry represents one video input source for the UI
    private final List<BroadcastSwitcherUiVideoEntry> videoInputSourceEntries = new ArrayList<>();

    /**
     * Creates the basic UI elements and callbacks.
     */
    public BroadcastSwitcherUi()
    {
        this.initComponents();
        this.buttonToggleLyrics.addActionListener(e -> this.lyricsPressed.accept(this.buttonToggleLyrics.isSelected()));
        this.buttonFadeToBlack.addActionListener(e -> this.fadeToBlackPressed.accept(true));
        this.buttonCut.addActionListener(e -> this.cutPressed.accept(true));
        this.buttonFade.addActionListener(e -> this.autoPressed.accept(true));
    }

    /**
     * Call this early in the initialization process to set the program/preview buttons in this UI.
     * A program and a preview button is created for each element in this map. Pressing these
     * buttons in the UI triggers that corresponding video source index to be fired in a callback.
     *
     * @param videoSourceNamesAndSwitcherIndexes lists the [name, index] of each type of buttons to create
     */
    public void setVideoSourceNamesAndSwitcherIndexes(Map<String, Integer> videoSourceNamesAndSwitcherIndexes)
    {
        // for each of the video inputs, create a structure with program and preview buttons
        videoSourceNamesAndSwitcherIndexes.forEach((name, index) ->
                this.videoInputSourceEntries.add(new BroadcastSwitcherUiVideoEntry(name, index)));

        // add these buttons to the overall UI
        this.videoInputSourceEntries.forEach(entry -> this.panelProgPrevButtonHolder.add(entry.createPreviewProgramButtonsPanel()));
        this.panelProgPrevButtonHolder.revalidate();
    }

    public void onPreviewSourceChanged(Consumer<Integer> previewSourceChanged)
    {
        this.previewSourceChanged = previewSourceChanged;
    }

    public void onProgramSourceChanged(Consumer<Integer> programSourceChanged)
    {
        this.programSourceChanged = programSourceChanged;
    }

    public void onFadeToBlackPressed(Consumer<Boolean> fadeToBlackPressed)
    {
        this.fadeToBlackPressed = fadeToBlackPressed;
    }

    public void onLyricsPressed(Consumer<Boolean> lyricsPressed)
    {
        this.lyricsPressed = lyricsPressed;
    }

    public void onAutoPressed(Consumer<Boolean> autoPressed)
    {
        this.autoPressed = autoPressed;
    }

    public void onCutPressed(Consumer<Boolean> cutPressed)
    {
        this.cutPressed = cutPressed;
    }

    /**
     * TODO
     * @param connected
     */
    public void setSwitcherConnectionStatus(boolean connected)
    {
    }

    /**
     * @param active indication the transition is in progress
     */
    public void setFadeTransitionInProgressStatus(boolean active)
    {
        this.buttonFade.setBackground(active ? Color.YELLOW : Color.BLACK);
    }

    /**
     * @param active indication the fade to black is active
     */
    public void setFadeToBlackStatus(boolean active)
    {
        this.buttonFadeToBlack.setBackground(active ? Color.RED : Color.BLACK);
    }

    /**
     * @param active indication the lyrics are displayed on-screen
     */
    public void setLyricsStatus(boolean active)
    {
        this.buttonToggleLyrics.setBackground(active ? Color.RED : Color.BLACK);
    }

    /**
     * @param previewSourceActive index of the active preview video source
     */
    public void setPreviewSourceStatus(int previewSourceActive)
    {
        this.videoInputSourceEntries.forEach(entry -> entry.setPreviewVideoSourceActive(previewSourceActive));
    }

    /**
     * @param programSourceActive index of the active program video source
     */
    public void setProgramSourceStatus(int programSourceActive)
    {
        this.videoInputSourceEntries.forEach(entry -> entry.setProgramSourceStatus(programSourceActive));
    }

    /**
     * Internal class which holds two buttons for each video source entry.
     * The buttons are for preview and program. This class sets up the callback
     * actions for the buttons, and also highlights the button background
     * colors when the broadcast switcher status has that button's video
     * source index active.
     */
    private class BroadcastSwitcherUiVideoEntry
    {
        private final int videoSourceIndex;
        private final JButton previewButton;
        private final JButton programButton;

        /**
         * @param name             name of this video source input
         * @param videoSourceIndex physical hardware connection index of this video source
         */
        private BroadcastSwitcherUiVideoEntry(String name, int videoSourceIndex)
        {
            this.videoSourceIndex = videoSourceIndex;

            // create the preview button
            this.previewButton = new JButton(name);
            this.previewButton.setBackground(Color.BLACK);
            this.previewButton.setForeground(Color.CYAN);
            this.previewButton.setFont(new Font("Segoe", Font.BOLD, 16));
            this.previewButton.setBorder(new CompoundBorder(new EmptyBorder(0,5,5,5), new LineBorder(Color.CYAN)));
            this.previewButton.addActionListener(e -> previewSourceChanged.accept(videoSourceIndex));

            // create the program button
            this.programButton = new JButton(name);
            this.programButton.setBackground(Color.BLACK);
            this.programButton.setForeground(Color.CYAN);
            this.programButton.setFont(new Font("Segoe", Font.BOLD, 16));
            this.programButton.setBorder(new CompoundBorder(new EmptyBorder(5,5,0,5), new LineBorder(Color.CYAN)));
            this.programButton.addActionListener(e -> programSourceChanged.accept(videoSourceIndex));
        }

        /**
         * @return creates a panel which holds these two buttons (preview on top)
         */
        private JPanel createPreviewProgramButtonsPanel()
        {
            final JPanel prevProgPanel = new JPanel(new GridLayout(2, 1));
            prevProgPanel.setOpaque(false);
            prevProgPanel.add(this.previewButton);
            prevProgPanel.add(this.programButton);
            return prevProgPanel;
        }

        /**
         * @param previewSourceActive index of the active preview video source
         */
        private void setPreviewVideoSourceActive(int previewSourceActive)
        {
            this.previewButton.setBackground(previewSourceActive == this.videoSourceIndex ? Color.GREEN : Color.BLACK);
        }

        /**
         * @param programSourceActive index of the active program video source
         */
        private void setProgramSourceStatus(int programSourceActive)
        {
            this.programButton.setBackground(programSourceActive == this.videoSourceIndex ? Color.RED : Color.BLACK);
        }
    }

    /**
     * JFormDesigner Auto-Generated Code.
     */
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        JPanel panel1 = new JPanel();
        buttonToggleLyrics = new JToggleButton();
        buttonFadeToBlack = new JButton();
        buttonCut = new JButton();
        buttonFade = new JButton();
        JPanel hSpacer1 = new JPanel(null);
        JPanel panel2 = new JPanel();
        JLabel labelPreview = new JLabel();
        panelProgPrevButtonHolder = new JPanel();
        JLabel labelProgram = new JLabel();

        //======== this ========
        setBackground(Color.black);
        setName("this");
        setLayout(new GridBagLayout());
        ((GridBagLayout)getLayout()).columnWidths = new int[] {0, 0, 0, 0};
        ((GridBagLayout)getLayout()).rowHeights = new int[] {0, 0};
        ((GridBagLayout)getLayout()).columnWeights = new double[] {0.0, 0.0, 1.0, 1.0E-4};
        ((GridBagLayout)getLayout()).rowWeights = new double[] {0.1, 1.0E-4};

        //======== panel1 ========
        {
            panel1.setOpaque(false);
            panel1.setName("panel1");
            panel1.setLayout(new GridLayout(2, 2, 10, 10));

            //---- buttonToggleLyrics ----
            buttonToggleLyrics.setText("<html>Toggle<br>Lyrics</html>");
            buttonToggleLyrics.setOpaque(false);
            buttonToggleLyrics.setForeground(Color.cyan);
            buttonToggleLyrics.setBackground(Color.black);
            buttonToggleLyrics.setFont(new Font("Segoe UI", Font.BOLD, 16));
            buttonToggleLyrics.setName("buttonToggleLyrics");
            panel1.add(buttonToggleLyrics);

            //---- buttonFadeToBlack ----
            buttonFadeToBlack.setText("<html>Fade to<br>Black</html>");
            buttonFadeToBlack.setOpaque(false);
            buttonFadeToBlack.setForeground(Color.cyan);
            buttonFadeToBlack.setBackground(Color.black);
            buttonFadeToBlack.setFont(new Font("Segoe UI", Font.BOLD, 16));
            buttonFadeToBlack.setName("buttonFadeToBlack");
            panel1.add(buttonFadeToBlack);

            //---- buttonCut ----
            buttonCut.setText("CUT");
            buttonCut.setOpaque(false);
            buttonCut.setForeground(Color.cyan);
            buttonCut.setBackground(Color.black);
            buttonCut.setFont(new Font("Segoe UI", Font.BOLD, 16));
            buttonCut.setName("buttonCut");
            panel1.add(buttonCut);

            //---- buttonFade ----
            buttonFade.setText("FADE");
            buttonFade.setOpaque(false);
            buttonFade.setForeground(Color.cyan);
            buttonFade.setBackground(Color.black);
            buttonFade.setFont(new Font("Segoe UI", Font.BOLD, 16));
            buttonFade.setName("buttonFade");
            panel1.add(buttonFade);
        }
        add(panel1, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 5), 0, 0));

        //---- hSpacer1 ----
        hSpacer1.setMinimumSize(new Dimension(40, 12));
        hSpacer1.setPreferredSize(new Dimension(40, 10));
        hSpacer1.setOpaque(false);
        hSpacer1.setName("hSpacer1");
        add(hSpacer1, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 5), 0, 0));

        //======== panel2 ========
        {
            panel2.setOpaque(false);
            panel2.setName("panel2");
            panel2.setLayout(new GridBagLayout());
            ((GridBagLayout)panel2.getLayout()).columnWidths = new int[] {0, 0, 0};
            ((GridBagLayout)panel2.getLayout()).rowHeights = new int[] {0, 0, 0};
            ((GridBagLayout)panel2.getLayout()).columnWeights = new double[] {0.0, 1.0, 1.0E-4};
            ((GridBagLayout)panel2.getLayout()).rowWeights = new double[] {1.0, 1.0, 1.0E-4};

            //---- labelPreview ----
            labelPreview.setText("<html><u>Preview<br>selection:</u></html>");
            labelPreview.setForeground(Color.cyan);
            labelPreview.setBackground(Color.black);
            labelPreview.setFont(new Font("Segoe UI", Font.BOLD, 16));
            labelPreview.setHorizontalAlignment(SwingConstants.RIGHT);
            labelPreview.setName("labelPreview");
            panel2.add(labelPreview, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 10, 10), 0, 0));

            //======== panelProgPrevButtonHolder ========
            {
                panelProgPrevButtonHolder.setOpaque(false);
                panelProgPrevButtonHolder.setName("panelProgPrevButtonHolder");
                panelProgPrevButtonHolder.setLayout(new BoxLayout(panelProgPrevButtonHolder, BoxLayout.X_AXIS));
            }
            panel2.add(panelProgPrevButtonHolder, new GridBagConstraints(1, 0, 1, 2, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));

            //---- labelProgram ----
            labelProgram.setText("<html><u>Program<br>selection:</u></html>");
            labelProgram.setForeground(Color.cyan);
            labelProgram.setBackground(Color.black);
            labelProgram.setFont(new Font("Segoe UI", Font.BOLD, 16));
            labelProgram.setHorizontalAlignment(SwingConstants.RIGHT);
            labelProgram.setName("labelProgram");
            panel2.add(labelProgram, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 10), 0, 0));
        }
        add(panel2, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JToggleButton buttonToggleLyrics;
    private JButton buttonFadeToBlack;
    private JButton buttonCut;
    private JButton buttonFade;
    private JPanel panelProgPrevButtonHolder;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
