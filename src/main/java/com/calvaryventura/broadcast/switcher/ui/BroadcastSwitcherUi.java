package com.calvaryventura.broadcast.switcher.ui;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * TODO
 */
public class BroadcastSwitcherUi extends JPanel
{
    private Consumer<Integer> previewSourceChanged;
    private Consumer<Integer> programSourceChanged;
    private Consumer<Boolean> fadeToBlackPressed;
    private Consumer<Boolean> lyricsPressed;
    private Consumer<Boolean> autoPressed;
    private Consumer<Boolean> cutPressed;

    /**
     * Initializes the HTTP connection to the Blackmagic switcher.
     */
    public BroadcastSwitcherUi()
    {
        this.initComponents();
        this.buttonToggleLyrics.addActionListener(e -> this.lyricsPressed.accept(this.buttonToggleLyrics.isSelected()));
        this.buttonFadeToBlack.addActionListener(e -> this.fadeToBlackPressed.accept(true));
        this.buttonCut.addActionListener(e -> this.cutPressed.accept(true));
        this.buttonFade.addActionListener(e -> this.autoPressed.accept(true));
        this.buttonOverheadPreview.addActionListener(e -> this.previewSourceChanged.accept(6));
        this.buttonWallPreview.addActionListener(e -> this.previewSourceChanged.accept(5));
        this.buttonBoothPreview.addActionListener(e -> this.previewSourceChanged.accept(4));
        this.buttonProclaimPreview.addActionListener(e -> this.previewSourceChanged.accept(3));
        this.buttonOverheadProgram.addActionListener(e -> this.programSourceChanged.accept(6));
        this.buttonWallProgram.addActionListener(e -> this.programSourceChanged.accept(5));
        this.buttonBoothProgram.addActionListener(e -> this.programSourceChanged.accept(4));
        this.buttonProclaimProgram.addActionListener(e -> this.programSourceChanged.accept(3));
    }

    public void setPreviewSourceChanged(Consumer<Integer> previewSourceChanged)
    {
        this.previewSourceChanged = previewSourceChanged;
    }

    public void setProgramSourceChanged(Consumer<Integer> programSourceChanged)
    {
        this.programSourceChanged = programSourceChanged;
    }

    public void setFadeToBlackPressed(Consumer<Boolean> fadeToBlackPressed)
    {
        this.fadeToBlackPressed = fadeToBlackPressed;
    }

    public void setLyricsPressed(Consumer<Boolean> lyricsPressed)
    {
        this.lyricsPressed = lyricsPressed;
    }

    public void setAutoPressed(Consumer<Boolean> autoPressed)
    {
        this.autoPressed = autoPressed;
    }

    public void setCutPressed(Consumer<Boolean> cutPressed)
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
        this.buttonProclaimPreview.setBackground(previewSourceActive == 3 ? Color.GREEN : Color.BLACK);
        this.buttonBoothPreview.setBackground(previewSourceActive == 4 ? Color.GREEN : Color.BLACK);
        this.buttonWallPreview.setBackground(previewSourceActive == 5 ? Color.GREEN : Color.BLACK);
        this.buttonOverheadPreview.setBackground(previewSourceActive == 6 ? Color.GREEN : Color.BLACK);
    }

    /**
     * @param programSourceActive index of the active program video source
     */
    public void setProgramSourceStatus(int programSourceActive)
    {
        this.buttonProclaimProgram.setBackground(programSourceActive == 3 ? Color.RED : Color.BLACK);
        this.buttonBoothProgram.setBackground(programSourceActive == 4 ? Color.RED : Color.BLACK);
        this.buttonWallProgram.setBackground(programSourceActive == 5 ? Color.RED : Color.BLACK);
        this.buttonOverheadProgram.setBackground(programSourceActive == 6 ? Color.RED : Color.BLACK);
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
        buttonProclaimPreview = new JButton();
        buttonBoothPreview = new JButton();
        buttonWallPreview = new JButton();
        buttonOverheadPreview = new JButton();
        JLabel labelProgram = new JLabel();
        buttonProclaimProgram = new JButton();
        buttonBoothProgram = new JButton();
        buttonWallProgram = new JButton();
        buttonOverheadProgram = new JButton();

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
            new Insets(0, 0, 0, 20), 0, 0));

        //---- hSpacer1 ----
        hSpacer1.setMinimumSize(new Dimension(50, 12));
        hSpacer1.setPreferredSize(new Dimension(50, 10));
        hSpacer1.setOpaque(false);
        hSpacer1.setName("hSpacer1");
        add(hSpacer1, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 20), 0, 0));

        //======== panel2 ========
        {
            panel2.setOpaque(false);
            panel2.setName("panel2");
            panel2.setLayout(new GridLayout(2, 4, 10, 10));

            //---- labelPreview ----
            labelPreview.setText("<html><u>Preview<br>selection:</u></html>");
            labelPreview.setForeground(Color.cyan);
            labelPreview.setBackground(Color.black);
            labelPreview.setFont(new Font("Segoe UI", Font.BOLD, 16));
            labelPreview.setHorizontalAlignment(SwingConstants.RIGHT);
            labelPreview.setName("labelPreview");
            panel2.add(labelPreview);

            //---- buttonProclaimPreview ----
            buttonProclaimPreview.setText("Proclaim");
            buttonProclaimPreview.setForeground(Color.cyan);
            buttonProclaimPreview.setBackground(Color.black);
            buttonProclaimPreview.setFont(new Font("Segoe UI", Font.BOLD, 16));
            buttonProclaimPreview.setName("buttonProclaimPreview");
            panel2.add(buttonProclaimPreview);

            //---- buttonBoothPreview ----
            buttonBoothPreview.setText("Booth");
            buttonBoothPreview.setForeground(Color.cyan);
            buttonBoothPreview.setBackground(Color.black);
            buttonBoothPreview.setFont(new Font("Segoe UI", Font.BOLD, 16));
            buttonBoothPreview.setName("buttonBoothPreview");
            panel2.add(buttonBoothPreview);

            //---- buttonWallPreview ----
            buttonWallPreview.setText("Wall");
            buttonWallPreview.setForeground(Color.cyan);
            buttonWallPreview.setBackground(Color.black);
            buttonWallPreview.setFont(new Font("Segoe UI", Font.BOLD, 16));
            buttonWallPreview.setName("buttonWallPreview");
            panel2.add(buttonWallPreview);

            //---- buttonOverheadPreview ----
            buttonOverheadPreview.setText("Overhead");
            buttonOverheadPreview.setForeground(Color.cyan);
            buttonOverheadPreview.setBackground(Color.black);
            buttonOverheadPreview.setFont(new Font("Segoe UI", Font.BOLD, 16));
            buttonOverheadPreview.setName("buttonOverheadPreview");
            panel2.add(buttonOverheadPreview);

            //---- labelProgram ----
            labelProgram.setText("<html><u>Program<br>selection:</u></html>");
            labelProgram.setForeground(Color.cyan);
            labelProgram.setBackground(Color.black);
            labelProgram.setFont(new Font("Segoe UI", Font.BOLD, 16));
            labelProgram.setHorizontalAlignment(SwingConstants.RIGHT);
            labelProgram.setName("labelProgram");
            panel2.add(labelProgram);

            //---- buttonProclaimProgram ----
            buttonProclaimProgram.setText("Proclaim");
            buttonProclaimProgram.setForeground(Color.cyan);
            buttonProclaimProgram.setBackground(Color.black);
            buttonProclaimProgram.setFont(new Font("Segoe UI", Font.BOLD, 16));
            buttonProclaimProgram.setName("buttonProclaimProgram");
            panel2.add(buttonProclaimProgram);

            //---- buttonBoothProgram ----
            buttonBoothProgram.setText("Booth");
            buttonBoothProgram.setForeground(Color.cyan);
            buttonBoothProgram.setBackground(Color.black);
            buttonBoothProgram.setFont(new Font("Segoe UI", Font.BOLD, 16));
            buttonBoothProgram.setName("buttonBoothProgram");
            panel2.add(buttonBoothProgram);

            //---- buttonWallProgram ----
            buttonWallProgram.setText("Wall");
            buttonWallProgram.setForeground(Color.cyan);
            buttonWallProgram.setBackground(Color.black);
            buttonWallProgram.setFont(new Font("Segoe UI", Font.BOLD, 16));
            buttonWallProgram.setName("buttonWallProgram");
            panel2.add(buttonWallProgram);

            //---- buttonOverheadProgram ----
            buttonOverheadProgram.setText("Overhead");
            buttonOverheadProgram.setForeground(Color.cyan);
            buttonOverheadProgram.setBackground(Color.black);
            buttonOverheadProgram.setFont(new Font("Segoe UI", Font.BOLD, 16));
            buttonOverheadProgram.setName("buttonOverheadProgram");
            panel2.add(buttonOverheadProgram);
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
    private JButton buttonProclaimPreview;
    private JButton buttonBoothPreview;
    private JButton buttonWallPreview;
    private JButton buttonOverheadPreview;
    private JButton buttonProclaimProgram;
    private JButton buttonBoothProgram;
    private JButton buttonWallProgram;
    private JButton buttonOverheadProgram;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
