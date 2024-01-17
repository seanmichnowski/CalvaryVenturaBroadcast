package com.calvaryventura.broadcast.uiwidgets;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * This class shows a popup volume fader.
 * It can optionally show current volume in dB.
 * When the user moves the fader, the callback is fired.
 * If the user clicks anywhere outside the popup, no
 * callback is fired and the popup is closed.
 */
public class PopupVolumeUi
{
    private static final Dimension POPUP_SIZE = new Dimension(50, 300);
    private static final Border POPUP_BORDER = new CompoundBorder(new CompoundBorder(new LineBorder(Color.CYAN, 3), new LineBorder(Color.DARK_GRAY, 5)), new EmptyBorder(4, 0, 4, 0));
    private static final int VOLUME_FADER_HEIGHT_PIX = 20;
    private final JFrame volumePopupFrame;
    private final JComponent volumeComponent;
    private double faderPercent0to1 = 0.65;
    private double minVolumeMeterLevelDb;
    private double midWarningVolumeMeterLevelDb;
    private double highWarningVolumeMeterLevelDb;
    private double maxVolumeMeterLevelDb;
    private double liveLeftVolumeMeterLevelDb = -100;  // some initial low audio dB value
    private double liveRightVolumeMeterLevelDb = -100; // some initial low audio dB value
    private Consumer<Double> faderMovedAction;
    private Consumer<Boolean> popupShownHiddenAction;
    private boolean drawVolumePercentageText;

    /**
     * @param faderMovedAction fired when the user moves the volume fader, value ranges 0.0 to 1.0
     */
    public void setFaderMovedAction(Consumer<Double> faderMovedAction)
    {
        this.faderMovedAction = faderMovedAction;
    }

    /**
     * @param shownHiddenAction fired when the popup begins to show and also gets hidden (boolean true on showing)
     */
    public void setPopupShownHiddenAction(Consumer<Boolean> shownHiddenAction)
    {
        this.popupShownHiddenAction = shownHiddenAction;
    }

    /**
     * Sets the ranges for the live audio meter.
     *
     * @param minDb minimum value for the live audio meter (lower boundary)
     * @param midWarningDb threshold where above this we draw yellow in the meter
     * @param highWarningDb threshold where above this we draw red in the meter
     * @param maxDb maximum value for the live audio meter (upper boundary)
     */
    public void setVolumeMeterLimits(double minDb, double midWarningDb, double highWarningDb, double maxDb)
    {
        this.minVolumeMeterLevelDb = minDb;
        this.midWarningVolumeMeterLevelDb = midWarningDb;
        this.highWarningVolumeMeterLevelDb = highWarningDb;
        this.maxVolumeMeterLevelDb = maxDb;
    }

    /**
     * Call this periodically to show the live volume level.
     *
     * @param leftDb live audio level in dB for the left channel
     * @param rightDb live audio level in dB for the right channel
     */
    public void setLiveVolumeMeterLevel(double leftDb, double rightDb)
    {
        this.liveLeftVolumeMeterLevelDb = leftDb;
        this.liveRightVolumeMeterLevelDb = rightDb;
        this.volumeComponent.repaint();
    }

    /**
     * Creates the popup but doesn't show it. See documentation {@link PopupVolumeUi}.
     * To show the popup, use the method {@link #showVolumePopup(double)}.
     */
    public PopupVolumeUi()
    {
        // create the volume UI component and override the drawing method
        this.volumeComponent = new JComponent()
        {
            /**
             * {@inheritDoc}
             */
            @Override
            protected void paintComponent(Graphics g)
            {
                final Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
                drawCustom(g2d);
            }
        };

        // set up the parent panel for the combobox
        final JPanel popupPanel = new JPanel();
        popupPanel.setLayout(new BorderLayout());
        popupPanel.setBackground(Color.DARK_GRAY);
        popupPanel.setPreferredSize(POPUP_SIZE);
        popupPanel.setBorder(POPUP_BORDER);
        popupPanel.add(this.volumeComponent, BorderLayout.CENTER);

        // create an UNDECORATED frame to be our popup
        this.volumePopupFrame = new JFrame("Volume");
        this.volumePopupFrame.setIconImage(JFrame.getFrames()[0].getIconImage());
        this.volumePopupFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        this.volumePopupFrame.setAlwaysOnTop(true);
        this.volumePopupFrame.setUndecorated(true);
        this.volumePopupFrame.add(popupPanel);
        this.volumePopupFrame.pack();

        // when the user clicks anywhere OUTSIDE the popup menu, close the whole frame!
        this.volumeComponent.setFocusable(true);
        this.volumeComponent.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                popupShownHiddenAction.accept(true);
            }

            @Override
            public void focusLost(FocusEvent e)
            {
                volumePopupFrame.setVisible(false);
                popupShownHiddenAction.accept(false);
            }
        });

        // listen for mouse drags on the volume component
        this.volumeComponent.addMouseMotionListener(new MouseAdapter()
        {
            public void mouseDragged(MouseEvent e)
            {
                volumeComponentMouseDragHandler(e);
            }
        });

        // listen for mouse press events, which trigger drawing the volume fader's percent
        this.volumeComponent.addMouseListener(new MouseAdapter()
        {
            public void mousePressed(MouseEvent e)
            {
                drawVolumePercentageText = true;
                volumeComponent.repaint();
            }

            public void mouseReleased(MouseEvent e)
            {
                drawVolumePercentageText = false;
                volumeComponent.repaint();
            }
        });
    }

    /**
     * Showing the popup is in an undecorated frame and DOES NOT BLOCK.
     *
     * @param initialFaderPercent specify a value 0.0 to 1.0 for the initial fader position
     */
    public void showVolumePopup(double initialFaderPercent)
    {
        // set the volume fader to the specified value
        this.faderPercent0to1 = initialFaderPercent;
        this.showVolumePopup();
    }

    /**
     * Showing the popup is in an undecorated frame and DOES NOT BLOCK.
     */
    public void showVolumePopup()
    {
        // show the popup at the current mouse location
        final Point p = MouseInfo.getPointerInfo().getLocation();
        this.volumePopupFrame.setLocation(p.x, p.y);
        this.volumePopupFrame.setVisible(true);
    }

    /**
     * This method responds to the mouse drag event on the
     * gui component {@link #volumeComponent}. The volume component
     * is a vertical display and slider, so use the mouse Y value.
     *
     * @param e the mouse event we are to process
     */
    private void volumeComponentMouseDragHandler(MouseEvent e)
    {
        final double percent = (double) e.getY() / this.volumeComponent.getHeight();
        final double percentBounded = Math.min(1.0, Math.max(0.0, percent));
        final double proposedFaderPercent = 1.0 - (double) (int) (percentBounded * 100) / 100; // invert since UI draws +Y down, and only keep hundreths place
        if (Math.abs(proposedFaderPercent - this.faderPercent0to1) > 0.01)
        {
            this.faderPercent0to1 = proposedFaderPercent;
            this.faderMovedAction.accept(this.faderPercent0to1);
            this.volumeComponent.repaint();
        }
    }

    /**
     * Entry point for drawing the heat map.
     *
     * @param g2d the Java graphics object
     */
    private void drawCustom(Graphics2D g2d)
    {
        // draw the volume fader behind the volume bars
        final int volumeFaderPixY = (int) ((1.0 - this.faderPercent0to1) * this.volumeComponent.getHeight()); // invert percent since +Y draws down
        final int volumeFaderPixYBounded = Math.min(this.volumeComponent.getHeight() - VOLUME_FADER_HEIGHT_PIX / 2, Math.max(VOLUME_FADER_HEIGHT_PIX / 2, volumeFaderPixY));
        g2d.setColor(Color.PINK);
        g2d.setStroke(new BasicStroke((float) VOLUME_FADER_HEIGHT_PIX));
        g2d.drawLine(0, volumeFaderPixYBounded, this.volumeComponent.getWidth(), volumeFaderPixYBounded);

        // convert volume meters into pixels
        final double totalVolumeMeterRangeDb = this.maxVolumeMeterLevelDb - this.minVolumeMeterLevelDb;
        final double midWarningPercentY = (this.midWarningVolumeMeterLevelDb - this.minVolumeMeterLevelDb) / totalVolumeMeterRangeDb;
        final double highWarningPercentY = (this.highWarningVolumeMeterLevelDb - this.minVolumeMeterLevelDb) / totalVolumeMeterRangeDb;
        final double liveLeftPercentY = (this.liveLeftVolumeMeterLevelDb - this.minVolumeMeterLevelDb) / totalVolumeMeterRangeDb;
        final double liveRightPercentY = (this.liveRightVolumeMeterLevelDb - this.minVolumeMeterLevelDb) / totalVolumeMeterRangeDb;
        final int midWarningPixY = (int) ((1.0 - midWarningPercentY) * this.volumeComponent.getHeight());   // invert percent since +Y draws down
        final int highWarningPixY = (int) ((1.0 - highWarningPercentY) * this.volumeComponent.getHeight()); // invert percent since +Y draws down
        final int liveLeftPixY = (int) ((1.0 - liveLeftPercentY) * this.volumeComponent.getHeight());       // invert percent since +Y draws down
        final int liveRightPixY = (int) ((1.0 - liveRightPercentY) * this.volumeComponent.getHeight());     // invert percent since +Y draws down

        // draw the volume meters
        final int leftMeterPixX = this.volumeComponent.getWidth() / 3;
        final int rightMeterPixX = 2 * this.volumeComponent.getWidth() / 3;
        g2d.setStroke(new BasicStroke((float) this.volumeComponent.getWidth() / 4));
        g2d.setColor(Color.GREEN);
        g2d.drawLine(leftMeterPixX, this.volumeComponent.getHeight(), leftMeterPixX, liveLeftPixY);
        g2d.drawLine(rightMeterPixX, this.volumeComponent.getHeight(), rightMeterPixX, liveRightPixY);
        if (liveLeftPercentY > midWarningPercentY)
        {
            g2d.setColor(Color.YELLOW);
            g2d.drawLine(leftMeterPixX, midWarningPixY, leftMeterPixX, liveLeftPixY);
        }
        if (liveRightPercentY > midWarningPercentY)
        {
            g2d.setColor(Color.YELLOW);
            g2d.drawLine(rightMeterPixX, midWarningPixY, rightMeterPixX, liveRightPixY);
        }
        if (liveLeftPercentY > highWarningPercentY)
        {
            g2d.setColor(Color.RED);
            g2d.drawLine(leftMeterPixX, highWarningPixY, leftMeterPixX, liveLeftPixY);
        }
        if (liveRightPercentY > highWarningPercentY)
        {
            g2d.setColor(Color.RED);
            g2d.drawLine(rightMeterPixX, highWarningPixY, rightMeterPixX, liveRightPixY);
        }

        // now, draw little pieces of the volume fader ON TOP of the volume bars
        final int volumeFaderLittlePieceThicknessPix = Math.max(2, VOLUME_FADER_HEIGHT_PIX / 8);
        g2d.setStroke(new BasicStroke(volumeFaderLittlePieceThicknessPix));
        g2d.setColor(Color.PINK);
        final int upperVolumeFaderPixY = volumeFaderPixYBounded - VOLUME_FADER_HEIGHT_PIX / 2 + volumeFaderLittlePieceThicknessPix / 2;
        final int lowerVolumeFaderPixY = volumeFaderPixYBounded + VOLUME_FADER_HEIGHT_PIX / 2 - volumeFaderLittlePieceThicknessPix / 2;
        g2d.drawLine(0, upperVolumeFaderPixY, this.volumeComponent.getWidth(), upperVolumeFaderPixY);
        g2d.drawLine(0, lowerVolumeFaderPixY, this.volumeComponent.getWidth(), lowerVolumeFaderPixY);

        // draw the volume percentage underneath the volume fader
        if (this.drawVolumePercentageText)
        {
            final int fontHeight = 14;
            g2d.setFont(new Font("Arial", Font.BOLD, fontHeight));
            final String s = (int) (this.faderPercent0to1 * 100) + "%";
            final int volumeTextPixY = volumeFaderPixYBounded + VOLUME_FADER_HEIGHT_PIX / 2 + fontHeight;
            g2d.drawString(s, 0, volumeTextPixY);
        }
    }

    /**
     * Simple test of the popup.
     *
     * @param args no arguments, standalone test
     */
    public static void main(String[] args)
    {
        // create the popup and configure it to reasonable audio level limits
        final PopupVolumeUi uut = new PopupVolumeUi();
        uut.setFaderMovedAction(volume -> System.out.printf("New volume: %f\n", volume));
        uut.setVolumeMeterLimits(-80, -20, -10, 0);

        // on popup showing, send periodic example audio meter data
        uut.setPopupShownHiddenAction(shownHidden -> {
            if (shownHidden)
            {
                System.out.println("Popup is showing! Starting periodic example volume meter levels...");
                final AtomicInteger phase = new AtomicInteger();
                Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(() -> {
                    final double leftDb = -40 + 40 * Math.cos(phase.doubleValue() / 50);   // range -80 to 0
                    final double rightDb = -40 + 40 * Math.sin(phase.doubleValue() / 50);  // range -80 to 0
                    uut.setLiveVolumeMeterLevel(leftDb, rightDb);
                    phase.getAndIncrement();
                }, 1000, 100, TimeUnit.MILLISECONDS);
            } else
            {
                System.out.println("Popup was closed! Exiting test utility.");
                System.exit(0);
            }
        });

        // finally, show the popup
        uut.showVolumePopup(0.5);
    }
}
