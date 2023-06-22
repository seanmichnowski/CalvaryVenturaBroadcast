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
    // local states
    private boolean fadeToBlackOnStatus;
    private boolean lyricsOnStatus;

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
        this.buttonToggleLyrics.addActionListener(e -> {
            this.lyricsOnStatus = !this.lyricsOnStatus;
            this.lyricsPressed.accept(this.lyricsOnStatus);
        });
        this.buttonFadeToBlack.addActionListener(e -> {
            this.fadeToBlackOnStatus = !this.fadeToBlackOnStatus;
            this.fadeToBlackPressed.accept(!this.fadeToBlackOnStatus);
        });
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
     * @param connected indication if we have established connection with the switcher
     */
    public void setSwitcherConnectionStatus(boolean connected)
    {
        this.labelConnectionStatus.setText(connected ? "Switcher connected :)" : "Switcher not connected :(");
        this.labelConnectionStatus.setForeground(connected ? Color.GREEN : Color.RED);

        // stop showing the color background on switcher buttons if we are not connected
        if (!connected)
        {
            this.videoInputSourceEntries.forEach(videoEntry -> {
                videoEntry.setPreviewVideoSourceActive(-1);
                videoEntry.setProgramSourceStatus(-1);
            });
        }
    }

    /**
     * @param active indication the transition is in progress
     */
    public void setFadeTransitionInProgressStatus(boolean active)
    {
        this.buttonFade.setBackground(active ? Color.YELLOW : Color.DARK_GRAY);
    }

    /**
     * @param active indication the fade to black is active or inactive
     * @param inTransition indicates we are fading, show the button in yellow
     */
    public void setFadeToBlackOnStatus(boolean active, boolean inTransition)
    {
        this.buttonFadeToBlack.setBackground(inTransition ? Color.YELLOW : active ? Color.RED : Color.DARK_GRAY);
        this.fadeToBlackOnStatus = active;
    }

    /**
     * @param active indication the lyrics are displayed on-screen
     */
    public void setLyricsStatus(boolean active)
    {
        this.buttonToggleLyrics.setBackground(active ? Color.RED : Color.DARK_GRAY);
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
            this.previewButton.setBackground(Color.DARK_GRAY);
            this.previewButton.setForeground(Color.WHITE);
            this.previewButton.setFont(new Font("Segoe", Font.BOLD, 18));
            this.previewButton.setBorder(new CompoundBorder(new EmptyBorder(5,5,5,5), new LineBorder(Color.GREEN)));
            this.previewButton.addActionListener(e -> previewSourceChanged.accept(videoSourceIndex));

            // create the program button
            this.programButton = new JButton(name);
            this.programButton.setBackground(Color.DARK_GRAY);
            this.programButton.setForeground(Color.WHITE);
            this.programButton.setFont(new Font("Segoe", Font.BOLD, 18));
            this.programButton.setBorder(new CompoundBorder(new EmptyBorder(5,5,5,5), new LineBorder(Color.RED)));
            this.programButton.addActionListener(e -> programSourceChanged.accept(videoSourceIndex));
        }

        /**
         * @return creates a panel which holds these two buttons (preview on top)
         */
        private JPanel createPreviewProgramButtonsPanel()
        {
            final JPanel prevProgPanel = new JPanel(new GridLayout(2, 1, 0, 10));
            prevProgPanel.setBorder(new EmptyBorder(0, 5, 0, 5));
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
            this.previewButton.setBackground(previewSourceActive == this.videoSourceIndex ? Color.GREEN : Color.DARK_GRAY);
        }

        /**
         * @param programSourceActive index of the active program video source
         */
        private void setProgramSourceStatus(int programSourceActive)
        {
            this.programButton.setBackground(programSourceActive == this.videoSourceIndex ? Color.RED : Color.DARK_GRAY);
        }
    }

    /**
     * JFormDesigner Auto-Generated Code.
     */
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        JPanel panel1 = new JPanel();
        buttonToggleLyrics = new JButton();
        buttonFadeToBlack = new JButton();
        buttonCut = new JButton();
        buttonFade = new JButton();
        JPanel panel2 = new JPanel();
        JLabel labelPreview = new JLabel();
        labelPreview.setUI(new VerticalLabelUI(false));
        panelProgPrevButtonHolder = new JPanel();
        JLabel labelProgram = new JLabel();
        labelProgram.setUI(new VerticalLabelUI(false));
        labelConnectionStatus = new JLabel();

        //======== this ========
        setBackground(Color.black);
        setName("this");
        setLayout(new GridBagLayout());
        ((GridBagLayout)getLayout()).columnWidths = new int[] {0, 20, 0, 0};
        ((GridBagLayout)getLayout()).rowHeights = new int[] {0, 0, 0};
        ((GridBagLayout)getLayout()).columnWeights = new double[] {0.1, 0.0, 1.0, 1.0E-4};
        ((GridBagLayout)getLayout()).rowWeights = new double[] {0.1, 0.0, 1.0E-4};

        //======== panel1 ========
        {
            panel1.setOpaque(false);
            panel1.setName("panel1");
            panel1.setLayout(new GridLayout(2, 2, 10, 10));

            //---- buttonToggleLyrics ----
            buttonToggleLyrics.setText("<html>Toggle<br>Lyrics</html>");
            buttonToggleLyrics.setForeground(Color.cyan);
            buttonToggleLyrics.setBackground(Color.darkGray);
            buttonToggleLyrics.setFont(new Font("Segoe UI", Font.BOLD, 20));
            buttonToggleLyrics.setName("buttonToggleLyrics");
            panel1.add(buttonToggleLyrics);

            //---- buttonFadeToBlack ----
            buttonFadeToBlack.setText("<html>Fade to<br>Black</html>");
            buttonFadeToBlack.setForeground(Color.cyan);
            buttonFadeToBlack.setBackground(Color.darkGray);
            buttonFadeToBlack.setFont(new Font("Segoe UI", Font.BOLD, 20));
            buttonFadeToBlack.setPreferredSize(new Dimension(120, 50));
            buttonFadeToBlack.setName("buttonFadeToBlack");
            panel1.add(buttonFadeToBlack);

            //---- buttonCut ----
            buttonCut.setText("CUT");
            buttonCut.setForeground(Color.cyan);
            buttonCut.setBackground(Color.darkGray);
            buttonCut.setFont(new Font("Segoe UI", Font.BOLD, 24));
            buttonCut.setName("buttonCut");
            panel1.add(buttonCut);

            //---- buttonFade ----
            buttonFade.setText("FADE");
            buttonFade.setForeground(Color.cyan);
            buttonFade.setBackground(Color.darkGray);
            buttonFade.setFont(new Font("Segoe UI", Font.BOLD, 24));
            buttonFade.setName("buttonFade");
            panel1.add(buttonFade);
        }
        add(panel1, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
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
            labelPreview.setText("Preview");
            labelPreview.setForeground(Color.green);
            labelPreview.setBackground(Color.black);
            labelPreview.setFont(new Font("Segoe UI", Font.BOLD, 16));
            labelPreview.setHorizontalAlignment(SwingConstants.CENTER);
            labelPreview.setName("labelPreview");
            panel2.add(labelPreview, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 10, 0), 0, 0));

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
            labelProgram.setText("Program");
            labelProgram.setForeground(Color.red);
            labelProgram.setBackground(Color.black);
            labelProgram.setFont(new Font("Segoe UI", Font.BOLD, 16));
            labelProgram.setHorizontalAlignment(SwingConstants.CENTER);
            labelProgram.setName("labelProgram");
            panel2.add(labelProgram, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));
        }
        add(panel2, new GridBagConstraints(2, 0, 1, 2, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));

        //---- labelConnectionStatus ----
        labelConnectionStatus.setText("Switcher not connected :(");
        labelConnectionStatus.setForeground(Color.red);
        labelConnectionStatus.setBackground(Color.black);
        labelConnectionStatus.setFont(new Font("Segoe UI", Font.BOLD, 14));
        labelConnectionStatus.setHorizontalAlignment(SwingConstants.LEFT);
        labelConnectionStatus.setName("labelConnectionStatus");
        add(labelConnectionStatus, new GridBagConstraints(0, 1, 2, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 5), 0, 0));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JButton buttonToggleLyrics;
    private JButton buttonFadeToBlack;
    private JButton buttonCut;
    private JButton buttonFade;
    private JPanel panelProgPrevButtonHolder;
    private JLabel labelConnectionStatus;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
