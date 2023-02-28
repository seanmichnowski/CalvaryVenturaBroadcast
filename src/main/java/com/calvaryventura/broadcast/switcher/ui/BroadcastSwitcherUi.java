package com.calvaryventura.broadcast.switcher.ui;

import com.calvaryventura.broadcast.switcher.IBroadcastSwitcherCallbacks;

import javax.swing.*;
import java.awt.*;

/**
 * TODO
 */
public class BroadcastSwitcherUi extends JPanel
{
    private IBroadcastSwitcherCallbacks callback;

    /**
     * Initializes the HTTP connection to the Blackmagic switcher.
     */
    public BroadcastSwitcherUi()
    {
        this.initComponents();

        this.buttonProclaim.addActionListener(e -> this.sendCommandAndSetButtonColor(this.buttonProclaim, 20));
        this.buttonBooth.addActionListener(e -> this.sendCommandAndSetButtonColor(this.buttonBooth, 21));
        this.buttonWall.addActionListener(e -> this.sendCommandAndSetButtonColor(this.buttonWall, 23));
        this.buttonOverhead.addActionListener(e -> this.sendCommandAndSetButtonColor(this.buttonOverhead, 22));
        this.buttonToggleLyrics.addActionListener(e -> this.callback.lyricsActive(true));
        this.buttonFadeToBlack.addActionListener(e -> this.callback.fadeToBlackActive(true));


    }

    /**
     * @param callback mechanism to pass UI events OUT of this class
     */
    public void setCallback(IBroadcastSwitcherCallbacks callback)
    {
        this.callback = callback;
    }

    /**
     * TODO
     * @param connected
     */
    public void setSwitcherConnectionStatus(boolean connected)
    {
    }

    /**
     * TODO
     * @param active
     */
    public void setFadeToBlackStatus(boolean active)
    {
    }

    /**
     * TODO
     * @param active
     */
    public void setLyricsStatus(boolean active)
    {
    }

    /**
     * TODO
     * @param previewSourceActive
     */
    public void setPreviewSourceActive(int previewSourceActive)
    {
        // TODO button.setBackground(Color.GREEN);
    }

    /**
     * TODO
     * @param programSourceActive
     */
    public void setProgramSourceActive(int programSourceActive)
    {
        // TODO button.setBackground(Color.RED.darker());
    }

    /**
     * @param button        the button to set to a RED background if the command succeeds
     * @param buttonCommand the blackmagic/companion stream-deck button number to send
     */
    private void sendCommandAndSetButtonColor(JButton button, int buttonCommand)
    {
        this.buttonWall.setBackground(Color.BLACK);
        this.buttonBooth.setBackground(Color.BLACK);
        this.buttonOverhead.setBackground(Color.BLACK);
        this.buttonProclaim.setBackground(Color.BLACK);
        this.callback.activeProgramInputChanged(buttonCommand);
    }

    /**
     * JFormDesigner Auto-Generated Code.
     */
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        buttonToggleLyrics = new JButton();
        buttonFadeToBlack = new JButton();
        JPanel hSpacer1 = new JPanel(null);
        buttonProclaim = new JButton();
        buttonBooth = new JButton();
        buttonWall = new JButton();
        buttonOverhead = new JButton();
        labelStatus = new JLabel();

        //======== this ========
        setBackground(Color.black);
        setName("this");
        setLayout(new GridBagLayout());
        ((GridBagLayout)getLayout()).columnWidths = new int[] {0, 0, 0, 0, 0, 0, 0, 0};
        ((GridBagLayout)getLayout()).rowHeights = new int[] {0, 0, 0};
        ((GridBagLayout)getLayout()).columnWeights = new double[] {0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 1.0E-4};
        ((GridBagLayout)getLayout()).rowWeights = new double[] {0.1, 0.0, 1.0E-4};

        //---- buttonToggleLyrics ----
        buttonToggleLyrics.setText("<html>TOGGLE<br>LYRICS</html>");
        buttonToggleLyrics.setOpaque(false);
        buttonToggleLyrics.setForeground(Color.cyan);
        buttonToggleLyrics.setBackground(Color.black);
        buttonToggleLyrics.setFont(new Font("Segoe UI", Font.BOLD, 16));
        buttonToggleLyrics.setName("buttonToggleLyrics");
        add(buttonToggleLyrics, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 20), 0, 0));

        //---- buttonFadeToBlack ----
        buttonFadeToBlack.setText("<html>Fade to<br>Black</html>");
        buttonFadeToBlack.setOpaque(false);
        buttonFadeToBlack.setForeground(Color.cyan);
        buttonFadeToBlack.setBackground(Color.black);
        buttonFadeToBlack.setFont(new Font("Segoe UI", Font.BOLD, 16));
        buttonFadeToBlack.setName("buttonFadeToBlack");
        add(buttonFadeToBlack, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 20), 0, 0));

        //---- hSpacer1 ----
        hSpacer1.setMinimumSize(new Dimension(50, 12));
        hSpacer1.setPreferredSize(new Dimension(50, 10));
        hSpacer1.setOpaque(false);
        hSpacer1.setName("hSpacer1");
        add(hSpacer1, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 20), 0, 0));

        //---- buttonProclaim ----
        buttonProclaim.setText("Proclaim");
        buttonProclaim.setForeground(Color.cyan);
        buttonProclaim.setBackground(Color.black);
        buttonProclaim.setFont(new Font("Segoe UI", Font.BOLD, 16));
        buttonProclaim.setName("buttonProclaim");
        add(buttonProclaim, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 20), 0, 0));

        //---- buttonBooth ----
        buttonBooth.setText("Booth");
        buttonBooth.setForeground(Color.cyan);
        buttonBooth.setBackground(Color.black);
        buttonBooth.setFont(new Font("Segoe UI", Font.BOLD, 16));
        buttonBooth.setName("buttonBooth");
        add(buttonBooth, new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 20), 0, 0));

        //---- buttonWall ----
        buttonWall.setText("Wall");
        buttonWall.setForeground(Color.cyan);
        buttonWall.setBackground(Color.black);
        buttonWall.setFont(new Font("Segoe UI", Font.BOLD, 16));
        buttonWall.setName("buttonWall");
        add(buttonWall, new GridBagConstraints(5, 0, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 20), 0, 0));

        //---- buttonOverhead ----
        buttonOverhead.setText("Overhead");
        buttonOverhead.setForeground(Color.cyan);
        buttonOverhead.setBackground(Color.black);
        buttonOverhead.setFont(new Font("Segoe UI", Font.BOLD, 16));
        buttonOverhead.setName("buttonOverhead");
        add(buttonOverhead, new GridBagConstraints(6, 0, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));

        //---- labelStatus ----
        labelStatus.setFont(new Font("Segoe UI", Font.BOLD, 14));
        labelStatus.setText(" ");
        labelStatus.setName("labelStatus");
        add(labelStatus, new GridBagConstraints(0, 1, 7, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JButton buttonToggleLyrics;
    private JButton buttonFadeToBlack;
    private JButton buttonProclaim;
    private JButton buttonBooth;
    private JButton buttonWall;
    private JButton buttonOverhead;
    private JLabel labelStatus;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
