package com.calvaryventura.broadcast.mainstrippeddown;

import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.SwingConstants;
import com.calvaryventura.broadcast.ptzcamera.control.PtzCameraController;
import com.calvaryventura.broadcast.settings.BroadcastSettings;
import com.calvaryventura.broadcast.switcher.control.BlackmagicAtemSwitcherUserLayer;
import com.calvaryventura.broadcast.uiwidgets.DirectionalTouchUi;
import com.calvaryventura.broadcast.uiwidgets.HorizontalZoomTouchUi;
import com.calvaryventura.broadcast.uiwidgets.PopupVolumeUi;
import com.calvaryventura.broadcast.uiwidgets.TitledBorderCreator;
import org.apache.log4j.BasicConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.invoke.MethodHandles;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

/**
 * Main UI and entry point. Major connections are made in this class.
 * The switcher UI is kept separate from the switcher controller, and
 * same with the PTZ cameras. So all these are joined together in this
 * class. This is where most of the user logic lies.
 */
public class BroadcastControlMainStrippedDown extends JFrame
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final BlackmagicAtemSwitcherUserLayer switcherCommandSender = new BlackmagicAtemSwitcherUserLayer();

    /**
     * Main entry point for application.
     */
    public static void main(String[] args)
    {
        BasicConfigurator.configure(); // logger
        logger.info("Starting Calvary Ventura Broadcast Control Interface. (Stripped down version, no camera presets.)");
        new BroadcastControlMainStrippedDown();
    }

    /**
     * Initializes the major UI panels, etc.
     */
    private BroadcastControlMainStrippedDown()
    {
        // UI initialization
        this.initComponents();
        this.setLocationRelativeTo(null);
        this.setTitle(BroadcastSettings.getInst().getProgramTitle());

        // initialize PTZ camera(s) callbacks
        final List<PtzCameraEntryPanel> ptzCameraEntryPanels = new ArrayList<>();
        BroadcastSettings.getInst().getPtzCameraNamesIps().forEach((ptzCameraName, ptzCameraSocketAddress) -> // TODO maybe one day combine this getter with "settings.getPtzCameraSwitcherInputIndexes()" so everything can be in one structure from the settings
        {
            // for each PTZ camera, create the controller and create the UI
            final PtzCameraEntryPanel ptzCameraControlEntry = new PtzCameraEntryPanel(ptzCameraName, ptzCameraSocketAddress);
            ptzCameraEntryPanels.add(ptzCameraControlEntry);
            this.parentPtzCamerasPanel.add(ptzCameraControlEntry.uiPanel);
        });

        // set up the video switcher control UI implementation
        logger.info("Starting connection to video switcher, multiview={}", BroadcastSettings.getInst().isVideoSwitcherMultiviewEnabled() ? "enabled" : "disabled");
        this.videoSwitcherPanel.setBorder(TitledBorderCreator.createTitledBorder("Video Switcher"));

        final PopupVolumeUi popupVolumeUi = new PopupVolumeUi();
        popupVolumeUi.setPopupShownHiddenAction(this.switcherCommandSender::enableSendingLiveAudioLevels);
        popupVolumeUi.setFaderMovedAction(this.switcherCommandSender::setMasterAudioLevel);
        this.buttonVolume.addActionListener(e -> popupVolumeUi.showVolumePopup());

        this.buttonToggleLyrics.addActionListener(e -> switcherCommandSender.toggleKeyerOnAirEnabled());

        // connections for the switcher's status to get updated on the UI control panel
        this.switcherCommandSender.addUpstreamKeyOnAirConsumer(lyricsOn -> this.buttonToggleLyrics.setBackground(lyricsOn ? Color.RED : Color.DARK_GRAY));
        this.switcherCommandSender.addLiveAudioLevelDbConsumer(popupVolumeUi::setLiveVolumeMeterLevel);
        this.switcherCommandSender.addTransitionInProgressConsumer(active -> this.labelTransitionInProgress.setForeground(active ? Color.YELLOW : Color.BLACK));
        this.switcherCommandSender.addConnectionStatusConsumer(connected -> {
            this.labelConnectionStatus.setText(connected ? "Switcher connected :)" : "Switcher not connected :(");
            this.labelConnectionStatus.setForeground(connected ? Color.GREEN : Color.RED);
            if (!connected)
            {
                this.labelProgram.setText("Program: ---");
                this.labelPreview.setText("Preview: ---");
            }
        });
        this.switcherCommandSender.addPreviewVideoSourceChangedConsumer(previewIdx -> {
            // for the love of God make this simplified and its own camera structure!!
            BroadcastSettings.getInst().getSwitcherVideoNamesAndIndexes().entrySet().stream().filter(entry -> entry.getValue().equals(previewIdx)).findFirst().ifPresent(entry -> this.labelPreview.setText("Preview: " + entry.getKey()));
        });
        this.switcherCommandSender.addProgramVideoSourceChangedConsumer(programIdx -> {
            BroadcastSettings.getInst().getSwitcherVideoNamesAndIndexes().entrySet().stream().filter(entry -> entry.getValue().equals(programIdx)).findFirst().ifPresent(entry -> this.labelProgram.setText("Program: " + entry.getKey()));

            // pull current preview/program sources
            final int switcherProgramIdx = this.switcherCommandSender.getCurrentVideoProgramIdx(); // TODO same one right as above??

            // update all PTZ camera UI panels to potentially show the current video switcher's PREVIEW/PROGRAM state
            final int ptzCameraIdxProgram = IntStream.range(0, BroadcastSettings.getInst().getPtzCameraSwitcherInputIndexes().size()).boxed()
                    .filter(ptzCameraIdx -> BroadcastSettings.getInst().getPtzCameraSwitcherInputIndexes().get(ptzCameraIdx) == switcherProgramIdx).findFirst().orElse(-1);
            IntStream.range(0, ptzCameraEntryPanels.size()).boxed().forEach(i -> ptzCameraEntryPanels.get(i).setCameraActive(i == ptzCameraIdxProgram));
        });

        // after UI initialization is done, finally start the connection to the switcher
        this.switcherCommandSender.initialize(BroadcastSettings.getInst().getSwitcherIp());

        // TODO start here.. add
        // https://forums.raspberrypi.com/viewtopic.php?t=271194
        // https://stackoverflow.com/questions/28841139/how-to-get-coordinates-of-touchscreen-rawdata-using-linux
        // https://stackoverflow.com/questions/6990978/how-can-i-know-which-of-the-dev-input-eventx-x-0-7-have-the-linux-input-stre
        //this.switcherCommandSender.setPreviewVideo()
        //this.switcherCommandSender.performCut();
        //this.switcherCommandSender.performAuto();

        // finally, show the frame maximized!
        this.setVisible(true);
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    /**
     * TODO
     */
    private static class PtzCameraEntryPanel
    {
        private final JPanel uiPanel = new JPanel(new BorderLayout());
        private final JLabel connectionStatusLabel = new JLabel();
        private final DirectionalTouchUi directionalTouchUi = new DirectionalTouchUi();
        private final HorizontalZoomTouchUi horizontalZoomTouchUi = new HorizontalZoomTouchUi();

        /**
         * TODO
         *
         * @param ptzCameraName
         * @param ipAddress
         * @return
         */
        private PtzCameraEntryPanel(String ptzCameraName, InetSocketAddress ipAddress)
        {
            // create the controller and a status label on the top
            this.connectionStatusLabel.setText(ptzCameraName);
            final PtzCameraController controller = new PtzCameraController(ptzCameraName, ipAddress, conn -> {
                this.connectionStatusLabel.setText(conn ? "Connected" : "Not connected");
                this.connectionStatusLabel.setForeground(conn ? Color.GREEN.darker() : Color.RED.darker());
            });

            // wrap the directional panel in a new JPanel so we can add empty border space above and below
            final JPanel directionalPanel = new JPanel(new BorderLayout());
            directionalPanel.setOpaque(false);
            directionalPanel.add(this.directionalTouchUi, BorderLayout.CENTER);
            directionalPanel.setBorder(new EmptyBorder(5, 0, 15, 0));

            // initialize callbacks from the UI elements for pan/tilt and zoom
            this.directionalTouchUi.addXYOutputConsumer(controller::panAndTilt);
            this.horizontalZoomTouchUi.addValueChangedConsumer(controller::changeZoom);

            // create a panel and add these items into it
            this.uiPanel.setOpaque(false);
            this.uiPanel.setBorder(TitledBorderCreator.createTitledBorder(ptzCameraName));
            this.horizontalZoomTouchUi.setPreferredSize(new Dimension(0, 60));
            this.uiPanel.add(this.connectionStatusLabel, BorderLayout.NORTH);
            this.uiPanel.add(directionalPanel, BorderLayout.CENTER);
            this.uiPanel.add(this.horizontalZoomTouchUi, BorderLayout.SOUTH);
        }

        /**
         * TODO
         * @param active
         */
        private void setCameraActive(boolean active)
        {
            this.directionalTouchUi.setDisplayMessageColor(active ? Color.RED : null);
            this.directionalTouchUi.setDisplayMessage(active ? "ACTIVE" : "PAN & TILT");
        }
    }

    /**
     * Since the video plays in a VLC rendered canvas, we can't control where exactly the video shows up.
     * All we know is (1) the video player gives the overall WxH of the video (before resizing),
     * (2) the canvas scales down the video to fully fit either the width or the height dimension, whichever
     * one is smaller, (3) we know where the mouse clicks happen within the physical bounds of the whole canvas.
     * From all this, we can calculate the expected bounds of the actual playing video inside the canvas,
     * after the automatic resizing to fit the canvas. From there we determine which X/Y box the mouse click
     * occurred, based on the divisions specified in the config file. Then we map an X/Y box to a video source,
     * and finally perform the appropriate action on that video source/box being selected.
     */
    private void initializeMouseSelectionOnMultiviewPanel(JPanel videoCanvas, BlackmagicAtemSwitcherUserLayer switcherCommandSender)
    {
        final BroadcastSettings settings = BroadcastSettings.getInst();
        videoCanvas.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                // based on the mouse percent INTO the playing video's rectangle, determine which grid box WITHIN the video we clicked inside (starts at 0 for X and Y and referenced from the upper-left corner)
                final double xPercent = (double) e.getX() / videoCanvas.getWidth();
                final double yPercent = (double) e.getY() / videoCanvas.getHeight();
                final int xGridBoxMouseLoc = (int) (xPercent * settings.getVideoSwitcherMultiviewNumColumnDivisions());
                final int yGridBoxMouseLoc = (int) (yPercent * settings.getVideoSwitcherMultiviewNumRowDivisions());
                final Point mouseClickGridBox = new Point(xGridBoxMouseLoc, yGridBoxMouseLoc);

                // find which multiview pane the user is clicking inside
                if (e.getClickCount() == 1 && settings.getVideoSwitcherMultiviewPreviewPaneGridBoxes().stream().anyMatch(gridPoint -> gridPoint.equals(mouseClickGridBox)))
                {
                    switcherCommandSender.performAuto(); // pressing in the "PREVIEW" pane triggers a fade transition
                } else if (e.getClickCount() == 1 && settings.getVideoSwitcherMultiviewProgramPaneGridBoxes().stream().anyMatch(gridPoint -> gridPoint.equals(mouseClickGridBox)))
                {
                    switcherCommandSender.performCut(); // pressing in the "PROGRAM" pane triggers a cut transition
                } else
                {
                    IntStream.range(0, settings.getVideoSwitcherMultiviewInputsGridBoxes().size()).boxed()
                            .filter(inputIdx -> settings.getVideoSwitcherMultiviewInputsGridBoxes().get(inputIdx).equals(mouseClickGridBox))
                            .findFirst().ifPresent(gridBoxIdx ->
                            {
                                // map the index of the source grid box to the ACTUAL HDMI input and HDMI name for the switcher's input channel
                                final Map<String, Integer> switcherVideoNamesAndIndexes = settings.getSwitcherVideoNamesAndIndexes();
                                final int switcherSourceIdx = new ArrayList<>(switcherVideoNamesAndIndexes.values()).get(gridBoxIdx);
                                final String switcherSourceName = new ArrayList<>(switcherVideoNamesAndIndexes.keySet()).get(gridBoxIdx);
                                if (e.getClickCount() == 1)
                                {
                                    logger.info("Changing to preview video input idx={} name={}", switcherSourceIdx, switcherSourceName);
                                    switcherCommandSender.setPreviewVideo(switcherSourceIdx);
                                } else if (e.getClickCount() == 2)
                                {
                                    logger.info("Changing to program video input idx={} name={}", switcherSourceIdx, switcherSourceName);
                                    switcherCommandSender.setPreviewVideo(switcherSourceIdx);
                                    switcherCommandSender.performAuto();
                                }
                            });
                }
            }
        });
    }

    /**
     * JFormDesigner Auto-Generated Code.
     */
    @SuppressWarnings("all")
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        panel1 = new JPanel();
        parentPtzCamerasPanel = new JPanel();
        videoSwitcherPanel = new JPanel();
        buttonToggleLyrics = new JButton();
        buttonVolume = new JButton();
        JPanel panel3 = new JPanel();
        labelPreview = new JLabel();
        labelProgram = new JLabel();
        JPanel panel2 = new JPanel();
        labelTransitionInProgress = new JLabel();
        labelConnectionStatus = new JLabel();

        //======== this ========
        setTitle("Default Title Overwritten by Config File");
        setFont(new Font(Font.DIALOG, Font.PLAIN, 14));
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setBackground(Color.black);
        setMinimumSize(new Dimension(400, 600));
        setIconImage(new ImageIcon(getClass().getResource("/icons/camera_fullsize.png")).getImage());
        setName("this");
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== panel1 ========
        {
            panel1.setBackground(Color.black);
            panel1.setName("panel1");
            panel1.setLayout(new GridBagLayout());
            ((GridBagLayout)panel1.getLayout()).columnWidths = new int[] {0, 0};
            ((GridBagLayout)panel1.getLayout()).rowHeights = new int[] {0, 0, 0};
            ((GridBagLayout)panel1.getLayout()).columnWeights = new double[] {1.0, 1.0E-4};
            ((GridBagLayout)panel1.getLayout()).rowWeights = new double[] {1.0, 0.0, 1.0E-4};

            //======== parentPtzCamerasPanel ========
            {
                parentPtzCamerasPanel.setOpaque(false);
                parentPtzCamerasPanel.setBorder(BorderFactory.createEmptyBorder());
                parentPtzCamerasPanel.setPreferredSize(new Dimension(673, 10));
                parentPtzCamerasPanel.setMinimumSize(new Dimension(0, 0));
                parentPtzCamerasPanel.setRequestFocusEnabled(false);
                parentPtzCamerasPanel.setName("parentPtzCamerasPanel");
                parentPtzCamerasPanel.setLayout(new GridLayout(1, 0, 10, 0));
            }
            panel1.add(parentPtzCamerasPanel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 10, 0), 0, 0));

            //======== videoSwitcherPanel ========
            {
                videoSwitcherPanel.setOpaque(false);
                videoSwitcherPanel.setName("videoSwitcherPanel");
                videoSwitcherPanel.setLayout(new GridBagLayout());
                ((GridBagLayout)videoSwitcherPanel.getLayout()).columnWidths = new int[] {0, 0, 0, 0, 0};
                ((GridBagLayout)videoSwitcherPanel.getLayout()).rowHeights = new int[] {0, 0};
                ((GridBagLayout)videoSwitcherPanel.getLayout()).columnWeights = new double[] {0.0, 0.0, 0.0, 1.0, 1.0E-4};
                ((GridBagLayout)videoSwitcherPanel.getLayout()).rowWeights = new double[] {0.0, 1.0E-4};

                //---- buttonToggleLyrics ----
                buttonToggleLyrics.setText("<html>Toggle<br>Lyrics</html>");
                buttonToggleLyrics.setForeground(Color.cyan);
                buttonToggleLyrics.setBackground(Color.darkGray);
                buttonToggleLyrics.setFont(new Font("Segoe UI", Font.BOLD, 20));
                buttonToggleLyrics.setName("buttonToggleLyrics");
                videoSwitcherPanel.add(buttonToggleLyrics, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 10), 0, 0));

                //---- buttonVolume ----
                buttonVolume.setText("Volume");
                buttonVolume.setForeground(Color.cyan);
                buttonVolume.setBackground(Color.darkGray);
                buttonVolume.setFont(new Font("Segoe UI", Font.BOLD, 20));
                buttonVolume.setPreferredSize(new Dimension(120, 50));
                buttonVolume.setName("buttonVolume");
                videoSwitcherPanel.add(buttonVolume, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 10), 0, 0));

                //======== panel3 ========
                {
                    panel3.setOpaque(false);
                    panel3.setName("panel3");
                    panel3.setLayout(new GridBagLayout());
                    ((GridBagLayout)panel3.getLayout()).columnWidths = new int[] {0, 0};
                    ((GridBagLayout)panel3.getLayout()).rowHeights = new int[] {0, 0, 0};
                    ((GridBagLayout)panel3.getLayout()).columnWeights = new double[] {1.0, 1.0E-4};
                    ((GridBagLayout)panel3.getLayout()).rowWeights = new double[] {1.0, 1.0, 1.0E-4};

                    //---- labelPreview ----
                    labelPreview.setBackground(Color.black);
                    labelPreview.setFont(new Font("Segoe UI", Font.BOLD, 14));
                    labelPreview.setHorizontalAlignment(SwingConstants.LEFT);
                    labelPreview.setText("Preview:");
                    labelPreview.setForeground(Color.white);
                    labelPreview.setName("labelPreview");
                    panel3.add(labelPreview, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 5, 0), 0, 0));

                    //---- labelProgram ----
                    labelProgram.setText("Program:");
                    labelProgram.setForeground(Color.white);
                    labelProgram.setBackground(Color.black);
                    labelProgram.setFont(new Font("Segoe UI", Font.BOLD, 14));
                    labelProgram.setHorizontalAlignment(SwingConstants.LEFT);
                    labelProgram.setMaximumSize(new Dimension(150, 15));
                    labelProgram.setName("labelProgram");
                    panel3.add(labelProgram, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 0), 0, 0));
                }
                videoSwitcherPanel.add(panel3, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 10), 0, 0));

                //======== panel2 ========
                {
                    panel2.setOpaque(false);
                    panel2.setName("panel2");
                    panel2.setLayout(new GridBagLayout());
                    ((GridBagLayout)panel2.getLayout()).columnWidths = new int[] {0, 0};
                    ((GridBagLayout)panel2.getLayout()).rowHeights = new int[] {0, 0, 0};
                    ((GridBagLayout)panel2.getLayout()).columnWeights = new double[] {1.0, 1.0E-4};
                    ((GridBagLayout)panel2.getLayout()).rowWeights = new double[] {1.0, 1.0, 1.0E-4};

                    //---- labelTransitionInProgress ----
                    labelTransitionInProgress.setBackground(Color.black);
                    labelTransitionInProgress.setFont(new Font("Segoe UI", Font.BOLD, 14));
                    labelTransitionInProgress.setHorizontalAlignment(SwingConstants.LEFT);
                    labelTransitionInProgress.setText("Transition In Progress...");
                    labelTransitionInProgress.setName("labelTransitionInProgress");
                    panel2.add(labelTransitionInProgress, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 5, 0), 0, 0));

                    //---- labelConnectionStatus ----
                    labelConnectionStatus.setText("Switcher not connected :(");
                    labelConnectionStatus.setForeground(Color.red);
                    labelConnectionStatus.setBackground(Color.black);
                    labelConnectionStatus.setFont(new Font("Segoe UI", Font.BOLD, 14));
                    labelConnectionStatus.setHorizontalAlignment(SwingConstants.LEFT);
                    labelConnectionStatus.setMaximumSize(new Dimension(150, 15));
                    labelConnectionStatus.setName("labelConnectionStatus");
                    panel2.add(labelConnectionStatus, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 0), 0, 0));
                }
                videoSwitcherPanel.add(panel2, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.EAST, GridBagConstraints.VERTICAL,
                    new Insets(0, 0, 0, 0), 0, 0));
            }
            panel1.add(videoSwitcherPanel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));
        }
        contentPane.add(panel1, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel panel1;
    private JPanel parentPtzCamerasPanel;
    private JPanel videoSwitcherPanel;
    private JButton buttonToggleLyrics;
    private JButton buttonVolume;
    private JLabel labelPreview;
    private JLabel labelProgram;
    private JLabel labelTransitionInProgress;
    private JLabel labelConnectionStatus;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
