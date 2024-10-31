package com.calvaryventura.broadcast.main.console.withpresets;

import com.calvaryventura.broadcast.main.console.BroadcastTouchscreenInputDevice;
import com.calvaryventura.broadcast.ptzcamera.control.PtzCameraController;
import com.calvaryventura.broadcast.ptzcamera.ui.IPtzCameraControllerUiCallback;
import com.calvaryventura.broadcast.ptzcamera.ui.PtzCameraControllerUi;
import com.calvaryventura.broadcast.settings.BroadcastSettings;
import com.calvaryventura.broadcast.switcher.control.BlackmagicAtemSwitcherUserLayer;
import com.calvaryventura.broadcast.uiwidgets.TitledBorderCreator;
import org.apache.log4j.BasicConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

/**
 * Main UI and entry point. Major connections are made in this class.
 * The switcher UI is kept separate from the switcher controller, and
 * same with the PTZ cameras. So all these are joined together in this
 * class. This is where most of the user logic lies.
 */
public class BroadcastControlMainConsoleWithPresets extends JFrame
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final BlackmagicAtemSwitcherUserLayer switcherCommandSender = new BlackmagicAtemSwitcherUserLayer();
    private final List<PtzCameraController> ptzCameraControllers = new ArrayList<>();
    private final PtzCameraControllerUi ptzCameraUi;
    private boolean muteOn = false;

    /**
     * Main entry point for application.
     */
    public static void main(String[] args)
    {
        BasicConfigurator.configure(); // logger
        logger.info("Starting Calvary Ventura Broadcast Control Interface...");
        new BroadcastControlMainConsoleWithPresets();
    }

    /**
     * Initializes the major UI panels, etc.
     */
    private BroadcastControlMainConsoleWithPresets()
    {
        // UI initialization
        this.initComponents();
        this.setLocationRelativeTo(null);
        this.setTitle(BroadcastSettings.getInst().getProgramTitle());
        TitledBorderCreator.setBorderColor(Color.CYAN.darker());

        // make all scroll bars wider, so they are easier to grab on a touchscreen
        UIManager.put("ScrollBar.width", 30);

        // connect LEFT camera UI panel actions
        final IPtzCameraControllerUiCallback ptzCameraUiCallback = new IPtzCameraControllerUiCallback()
        {
            @Override
            public boolean setPressed(int ptzCameraIdx, int presetIdx)
            {
                return ptzCameraControllers.get(ptzCameraIdx).savePreset(presetIdx);
            }

            @Override
            public void callPressed(int ptzCameraIdx, int presetIdx)
            {
                // attempt to move the camera, also show this camera in the preview window
                Executors.newSingleThreadExecutor().submit(() -> { // TODO is this OK???
                    final boolean cameraMoveOk = ptzCameraControllers.get(ptzCameraIdx).moveToPreset(presetIdx);
                    if (cameraMoveOk)
                    {
                        switcherCommandSender.setPreviewVideo(BroadcastSettings.getInst().getPtzCameraSwitcherInputIndexes().get(ptzCameraIdx));
                        SwingUtilities.invokeLater(() -> updatePreviewProgramColorsOnCameraUis());
                    }
                });
            }

            @Override
            public boolean panTilt(int ptzCameraIdx, double pan, double tilt)
            {
                return ptzCameraControllers.get(ptzCameraIdx).panAndTilt(pan, tilt);
            }

            @Override
            public boolean zoom(int ptzCameraIdx, double zoom)
            {
                return ptzCameraControllers.get(ptzCameraIdx).changeZoom(zoom);
            }
        };

        // create the camera control UI and add it to the main GUI
        this.ptzCameraUi = new PtzCameraControllerUi(new ArrayList<>(BroadcastSettings.getInst().getPtzCameraNamesIps().keySet()), ptzCameraUiCallback);
        this.ptzCameraUi.setBorder(TitledBorderCreator.createTitledBorder("Camera Presets"));
        this.parentPtzCamerasPanel.add(this.ptzCameraUi, BorderLayout.CENTER);

        // initialize PTZ camera(s) callbacks
        BroadcastSettings.getInst().getPtzCameraNamesIps().forEach((ptzCameraName, ptzCameraSocketAddress) -> // TODO maybe one day combine this getter with "settings.getPtzCameraSwitcherInputIndexes()" so everything can be in one structure from the settings
        {
            // for each PTZ camera, create the controller and create the UI
            ptzCameraControllers.add(new PtzCameraController(ptzCameraName, ptzCameraSocketAddress, conn -> this.ptzCameraUi.setCameraConnectionStatus(ptzCameraName, conn)));
        });

        // set up the video switcher control UI implementation
        logger.info("Starting connection to video switcher, multiview={}", BroadcastSettings.getInst().isVideoSwitcherMultiviewEnabled() ? "enabled" : "disabled");
        this.videoSwitcherPanel.setBorder(TitledBorderCreator.createTitledBorder("Video Switcher"));

        // mute button
        this.buttonToggleMute.addActionListener(e -> {
            if (this.switcherCommandSender.setMasterAudioLevel(this.muteOn ? 1.0 : 0.0)) // send opposite to the current mute state
            {
                this.muteOn = !this.muteOn;
                this.buttonToggleMute.setBackground(this.muteOn ? Color.RED : Color.DARK_GRAY);
            }
        });

        // lyrics button (upstream keyer)
        this.buttonToggleLyrics.addActionListener(e -> this.switcherCommandSender.toggleKeyerOnAirEnabled());

        // connections for the switcher's status to get updated on the UI control panel
        this.switcherCommandSender.addUpstreamKeyOnAirConsumer(lyricsOn -> this.buttonToggleLyrics.setBackground(lyricsOn ? Color.RED : Color.DARK_GRAY));
        this.switcherCommandSender.addTransitionInProgressConsumer(active -> this.labelTransitionInProgress.setForeground(active ? Color.YELLOW : Color.BLACK));
        this.switcherCommandSender.addConnectionStatusConsumer(connected -> {
            this.labelConnectionStatus.setText(connected ? "Switcher connected :)" : "Switcher not connected :(");
            this.labelConnectionStatus.setForeground(connected ? Color.GREEN.darker() : Color.RED.darker());
            if (!connected)
            {
                this.labelProgram.setText("Program: ---");
                this.labelPreview.setText("Preview: ---");
            }
        });
        this.switcherCommandSender.addPreviewVideoSourceChangedConsumer(previewIdx -> {
            // for the love of God make this simplified and its own camera structure!!
            BroadcastSettings.getInst().getSwitcherVideoNamesAndIndexes().entrySet().stream().filter(entry -> entry.getValue().equals(previewIdx)).findFirst().ifPresent(entry -> this.labelPreview.setText("Preview: " + entry.getKey()));
            this.updatePreviewProgramColorsOnCameraUis(); // reflect on UI when switcher changes its source
        });
        this.switcherCommandSender.addProgramVideoSourceChangedConsumer(programIdx -> {
            BroadcastSettings.getInst().getSwitcherVideoNamesAndIndexes().entrySet().stream().filter(entry -> entry.getValue().equals(programIdx)).findFirst().ifPresent(entry -> this.labelProgram.setText("Program: " + entry.getKey()));
            this.updatePreviewProgramColorsOnCameraUis(); // reflect on UI when switcher changes its source
        });

        // after UI initialization is done, finally start the connection to the switcher
        this.switcherCommandSender.initialize(BroadcastSettings.getInst().getSwitcherIp());

        // set up for monitoring the multiview display's touchscreen, so we can command the video switcher
        new BroadcastTouchscreenInputDevice().initializeTouchscreenCallbacks((xPercent, yPercent) -> {
            logger.info("Touchscreen! X={}%, Y={}%", xPercent, yPercent);
            this.handleMultiviewPanelTouchscreenEvent(xPercent, yPercent, this.switcherCommandSender);
        });

        // finally, show the frame maximized!
        this.setVisible(true);
        SwingUtilities.invokeLater(() -> this.setExtendedState(JFrame.MAXIMIZED_BOTH));
    }

    /**
     * The PTZ camera UI panels (for LEFT and RIGHT cameras) can have their active colors
     * updated to reflect the state of the video switcher. This method gets called whenever
     * the switcher goes to a new state, so we can reflect preview/program states on the camera UI's.
     */
    private void updatePreviewProgramColorsOnCameraUis()
    {
        // pull current preview/program sources
        final int switcherPreviewIdx = this.switcherCommandSender.getCurrentVideoPreviewIdx();
        final int switcherProgramIdx = this.switcherCommandSender.getCurrentVideoProgramIdx();

        // update all PTZ camera UI panels to potentially show the current video switcher's PREVIEW/PROGRAM state
        final int ptzCameraIdxPreview = IntStream.range(0, BroadcastSettings.getInst().getPtzCameraSwitcherInputIndexes().size()).boxed()
                .filter(ptzCameraIdx -> BroadcastSettings.getInst().getPtzCameraSwitcherInputIndexes().get(ptzCameraIdx) == switcherPreviewIdx).findFirst().orElse(-1);
        final int ptzCameraIdxProgram = IntStream.range(0, BroadcastSettings.getInst().getPtzCameraSwitcherInputIndexes().size()).boxed()
                .filter(ptzCameraIdx -> BroadcastSettings.getInst().getPtzCameraSwitcherInputIndexes().get(ptzCameraIdx) == switcherProgramIdx).findFirst().orElse(-1);
        this.ptzCameraUi.setActivePresetsColored(ptzCameraIdxPreview, ptzCameraIdxProgram);
    }

    /**
     * Specify the X and Y percents of the touchscreen press event,
     * and this method commands the switcher to perform one of the transition actions.
     *
     * @param xPercent              x percent of the touch event 0.0-1.0 (referenced to the upper-left corner)
     * @param yPercent              y percent of the touch event 0.0-1.0 (referenced to the upper-left corner)
     * @param switcherCommandSender handle to the video switcher for sending the transition commands
     */
    private void handleMultiviewPanelTouchscreenEvent(double xPercent, double yPercent, BlackmagicAtemSwitcherUserLayer switcherCommandSender)
    {
        // based on the mouse percent INTO the playing video's rectangle, determine which grid box WITHIN the video we clicked inside (starts at 0 for X and Y and referenced from the upper-left corner)
        final BroadcastSettings settings = BroadcastSettings.getInst();
        final int xGridBoxMouseLoc = (int) (xPercent * settings.getVideoSwitcherMultiviewNumColumnDivisions());
        final int yGridBoxMouseLoc = (int) (yPercent * settings.getVideoSwitcherMultiviewNumRowDivisions());
        final Point mouseClickGridBox = new Point(xGridBoxMouseLoc, yGridBoxMouseLoc);

        // find which multiview pane the user is clicking inside
        if (settings.getVideoSwitcherMultiviewPreviewPaneGridBoxes().stream().anyMatch(gridPoint -> gridPoint.equals(mouseClickGridBox)))
        {
            logger.info("Performing FADE");
            switcherCommandSender.performAuto(); // pressing in the "PREVIEW" pane triggers a fade transition
        } else if (settings.getVideoSwitcherMultiviewProgramPaneGridBoxes().stream().anyMatch(gridPoint -> gridPoint.equals(mouseClickGridBox)))
        {
            logger.info("Performing CUT");
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
                        logger.info("Changing to preview video input idx={} name={}", switcherSourceIdx, switcherSourceName);
                        switcherCommandSender.setPreviewVideo(switcherSourceIdx);
                    });
        }
    }

    /**
     * JFormDesigner Auto-Generated Code.
     */
    @SuppressWarnings("all")
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        JPanel panel1 = new JPanel();
        this.parentPtzCamerasPanel = new JPanel();
        this.videoSwitcherPanel = new JPanel();
        this.buttonToggleLyrics = new JButton();
        this.buttonToggleMute = new JButton();
        JPanel panel3 = new JPanel();
        this.labelPreview = new JLabel();
        this.labelProgram = new JLabel();
        JPanel panel2 = new JPanel();
        this.labelTransitionInProgress = new JLabel();
        this.labelConnectionStatus = new JLabel();

        //======== this ========
        setTitle("Default Title Overwritten by Config File");
        setFont(new Font(Font.DIALOG, Font.PLAIN, 14));
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setBackground(Color.black);
        setMinimumSize(new Dimension(400, 300));
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
                this.parentPtzCamerasPanel.setOpaque(false);
                this.parentPtzCamerasPanel.setBorder(BorderFactory.createEmptyBorder());
                this.parentPtzCamerasPanel.setPreferredSize(new Dimension(673, 10));
                this.parentPtzCamerasPanel.setMinimumSize(new Dimension(0, 0));
                this.parentPtzCamerasPanel.setRequestFocusEnabled(false);
                this.parentPtzCamerasPanel.setName("parentPtzCamerasPanel");
                this.parentPtzCamerasPanel.setLayout(new GridLayout(1, 0, 10, 0));
            }
            panel1.add(this.parentPtzCamerasPanel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));

            //======== videoSwitcherPanel ========
            {
                this.videoSwitcherPanel.setOpaque(false);
                this.videoSwitcherPanel.setName("videoSwitcherPanel");
                this.videoSwitcherPanel.setLayout(new GridBagLayout());
                ((GridBagLayout)this.videoSwitcherPanel.getLayout()).columnWidths = new int[] {0, 0, 0, 0, 0};
                ((GridBagLayout)this.videoSwitcherPanel.getLayout()).rowHeights = new int[] {0, 0};
                ((GridBagLayout)this.videoSwitcherPanel.getLayout()).columnWeights = new double[] {0.0, 0.0, 0.0, 1.0, 1.0E-4};
                ((GridBagLayout)this.videoSwitcherPanel.getLayout()).rowWeights = new double[] {0.0, 1.0E-4};

                //---- buttonToggleLyrics ----
                this.buttonToggleLyrics.setText("<html>TOGGLE<br>LYRICS</html>");
                this.buttonToggleLyrics.setForeground(Color.cyan);
                this.buttonToggleLyrics.setBackground(Color.darkGray);
                this.buttonToggleLyrics.setFont(new Font("Segoe UI", Font.BOLD, 16));
                this.buttonToggleLyrics.setName("buttonToggleLyrics");
                this.videoSwitcherPanel.add(this.buttonToggleLyrics, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 15), 0, 0));

                //---- buttonToggleMute ----
                this.buttonToggleMute.setText("MUTE");
                this.buttonToggleMute.setForeground(Color.cyan);
                this.buttonToggleMute.setBackground(Color.darkGray);
                this.buttonToggleMute.setFont(new Font("Segoe UI", Font.BOLD, 16));
                this.buttonToggleMute.setName("buttonToggleMute");
                this.videoSwitcherPanel.add(this.buttonToggleMute, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 15), 0, 0));

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
                    this.labelPreview.setBackground(Color.black);
                    this.labelPreview.setFont(new Font("Segoe UI", Font.BOLD, 14));
                    this.labelPreview.setHorizontalAlignment(SwingConstants.LEFT);
                    this.labelPreview.setText("Preview:");
                    this.labelPreview.setForeground(Color.white);
                    this.labelPreview.setName("labelPreview");
                    panel3.add(this.labelPreview, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 5, 0), 0, 0));

                    //---- labelProgram ----
                    this.labelProgram.setText("Program:");
                    this.labelProgram.setForeground(Color.white);
                    this.labelProgram.setBackground(Color.black);
                    this.labelProgram.setFont(new Font("Segoe UI", Font.BOLD, 14));
                    this.labelProgram.setHorizontalAlignment(SwingConstants.LEFT);
                    this.labelProgram.setMaximumSize(new Dimension(150, 15));
                    this.labelProgram.setName("labelProgram");
                    panel3.add(this.labelProgram, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 0), 0, 0));
                }
                this.videoSwitcherPanel.add(panel3, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 15), 0, 0));

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
                    this.labelTransitionInProgress.setBackground(Color.black);
                    this.labelTransitionInProgress.setFont(new Font("Segoe UI", Font.BOLD, 14));
                    this.labelTransitionInProgress.setHorizontalAlignment(SwingConstants.LEFT);
                    this.labelTransitionInProgress.setText("Transition In Progress...");
                    this.labelTransitionInProgress.setName("labelTransitionInProgress");
                    panel2.add(this.labelTransitionInProgress, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 5, 0), 0, 0));

                    //---- labelConnectionStatus ----
                    this.labelConnectionStatus.setText("Switcher not connected :(");
                    this.labelConnectionStatus.setForeground(Color.red);
                    this.labelConnectionStatus.setBackground(Color.black);
                    this.labelConnectionStatus.setFont(new Font("Segoe UI", Font.BOLD, 14));
                    this.labelConnectionStatus.setHorizontalAlignment(SwingConstants.LEFT);
                    this.labelConnectionStatus.setMaximumSize(new Dimension(150, 15));
                    this.labelConnectionStatus.setName("labelConnectionStatus");
                    panel2.add(this.labelConnectionStatus, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 0), 0, 0));
                }
                this.videoSwitcherPanel.add(panel2, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.EAST, GridBagConstraints.VERTICAL,
                    new Insets(0, 0, 0, 0), 0, 0));
            }
            panel1.add(this.videoSwitcherPanel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));
        }
        contentPane.add(panel1, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel parentPtzCamerasPanel;
    private JPanel videoSwitcherPanel;
    private JButton buttonToggleLyrics;
    private JButton buttonToggleMute;
    private JLabel labelPreview;
    private JLabel labelProgram;
    private JLabel labelTransitionInProgress;
    private JLabel labelConnectionStatus;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
