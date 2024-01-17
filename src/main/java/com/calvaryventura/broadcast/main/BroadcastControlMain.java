package com.calvaryventura.broadcast.main;

import java.awt.*;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.*;
import javax.swing.border.*;

import com.calvaryventura.broadcast.ptzcamera.control.PtzCameraController;
import com.calvaryventura.broadcast.ptzcamera.ui.IPtzCameraUiCallbacks;
import com.calvaryventura.broadcast.ptzcamera.ui.PtzCameraUi;
import com.calvaryventura.broadcast.settings.BroadcastSettings;
import com.calvaryventura.broadcast.switcher.control.BlackmagicAtemSwitcherUserLayer;
import com.calvaryventura.broadcast.switcher.ui.AbstractBroadcastSwitcherUi;
import com.calvaryventura.broadcast.switcher.ui.BroadcastSwitcherUiCallbacks;
import com.calvaryventura.broadcast.switcher.ui.withmultiview.BroadcastSwitcherMultiviewControlPanelUi;
import com.calvaryventura.broadcast.switcher.ui.withoutmultiview.BroadcastSwitcherControlPanelUi;
import com.calvaryventura.broadcast.uiwidgets.SplitPaneBarColorizer;
import org.apache.log4j.BasicConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main UI and entry point. Major connections are made in this class.
 * The switcher UI is kept separate from the switcher controller, and
 * same with the PTZ cameras. So all these are joined together in this
 * class. This is where most of the user logic lies.
 */
public class BroadcastControlMain extends JFrame
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final BlackmagicAtemSwitcherUserLayer switcherCommandSender = new BlackmagicAtemSwitcherUserLayer();
    private final PtzCameraController leftCameraController;
    private final PtzCameraController rightCameraController;
    private final BroadcastSettings settings;

    /**
     * Main entry point for application.
     */
    public static void main(String[] args)
    {
        BasicConfigurator.configure(); // logger
        logger.info("Starting Calvary Ventura Broadcast Control Interface...");
        new BroadcastControlMain();
    }

    /**
     * Initializes the major UI panels, etc.
     */
    private BroadcastControlMain()
    {
        // read the settings file
        this.settings = BroadcastSettings.getInst();

        // UI initialization
        this.initComponents();
        this.setLocationRelativeTo(null);
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.setTitle(this.settings.getProgramTitle());
        this.setVisible(true);

        // add a custom colored UI to the split pane bar instead of the original boring one
        SplitPaneBarColorizer.setSplitPaneBarStriped(this.parentSplitPane, Color.GREEN);

        // make all scroll bars wider so they are easier to grab on a touchscreen
        UIManager.put("ScrollBar.width", 30);

        // initialize LEFT/RIGHT camera command senders
        this.leftCameraController = new PtzCameraController("LEFT", this.settings.getLeftCameraIp(), 5678);
        this.rightCameraController = new PtzCameraController("RIGHT", this.settings.getRightCameraIp(), 5678);
        this.leftCameraController.onCameraConnectionStatus(connected -> this.leftCameraControlPanel.setCameraConnectionStatus(connected));
        this.rightCameraController.onCameraConnectionStatus(connected -> this.rightCameraControlPanel.setCameraConnectionStatus(connected));

        // connect LEFT camera UI panel actions
        this.leftCameraControlPanel.setCallback(new IPtzCameraUiCallbacks()
        {
            @Override
            public boolean setPressed(int presetIdx)
            {
                return leftCameraController.savePreset(presetIdx);
            }

            @Override
            public boolean callPressed(int presetIdx)
            {
                // attempt to move the camera, also show this camera in the preview window
                final boolean cameraMoveOk = leftCameraController.moveToPreset(presetIdx);
                switcherCommandSender.setPreviewVideo(settings.getLeftCameraVideoIndex());
                SwingUtilities.invokeLater(() -> updatePreviewProgramColorsOnLeftRightCameraUis());
                return cameraMoveOk;
            }

            @Override
            public boolean panTilt(double pan, double tilt)
            {
                return leftCameraController.panAndTilt(pan, tilt);
            }

            @Override
            public boolean zoom(double zoom)
            {
                return leftCameraController.changeZoom(zoom);
            }
        });

        // connect RIGHT camera UI panel actions
        this.rightCameraControlPanel.setCallback(new IPtzCameraUiCallbacks()
        {
            @Override
            public boolean setPressed(int presetIdx)
            {
                return rightCameraController.savePreset(presetIdx);
            }

            @Override
            public boolean callPressed(int presetIdx)
            {
                // attempt to move the camera, also show this camera in the preview window
                final boolean cameraMoveOk = rightCameraController.moveToPreset(presetIdx);
                switcherCommandSender.setPreviewVideo(settings.getRightCameraVideoIndex());
                SwingUtilities.invokeLater(() -> updatePreviewProgramColorsOnLeftRightCameraUis());
                return cameraMoveOk;
            }

            @Override
            public boolean panTilt(double pan, double tilt)
            {
                return rightCameraController.panAndTilt(pan, tilt);
            }

            @Override
            public boolean zoom(double zoom)
            {
                return rightCameraController.changeZoom(zoom);
            }
        });

        // set up the video switcher control UI implementation
        logger.info("Starting connection to video switcher, multiview={}", this.settings.isVideoSwitcherMultiviewEnabled() ? "enabled" : "disabled");
        final AbstractBroadcastSwitcherUi videoSwitcherControllerUi = this.settings.isVideoSwitcherMultiviewEnabled()
                ? new BroadcastSwitcherMultiviewControlPanelUi() : new BroadcastSwitcherControlPanelUi();
        this.switcherControlPanel.add(videoSwitcherControllerUi, BorderLayout.CENTER);

        // connections for the switcher's UI control panel to actually send commands
        final AtomicBoolean lyricsEnabled = new AtomicBoolean(false);
        videoSwitcherControllerUi.setCallbacks(new BroadcastSwitcherUiCallbacks()
        {
            @Override
            public void onPreviewSourceChanged(int previewSourceChanged)
            {
                switcherCommandSender.setPreviewVideo(previewSourceChanged);
            }

            @Override
            public void onProgramSourceChanged(int programSourceChanged)
            {
                switcherCommandSender.setProgramVideo(programSourceChanged);
            }

            @Override
            public void onFadeToBlack()
            {
                switcherCommandSender.performFadeToBlack();
            }

            @Override
            public void setSwitcherSendingLiveAudio(boolean enable)
            {
                switcherCommandSender.enableSendingLiveAudioLevels(enable);
            }

            @Override
            public void setAudioLevelPercent(double percent0to1)
            {
                switcherCommandSender.setMasterAudioLevel(percent0to1);
            }

            @Override
            public void onLyricsEnabled()
            {
                lyricsEnabled.set(!lyricsEnabled.get());
                switcherCommandSender.setKeyerOnAirEnabled(lyricsEnabled.get());
            }

            @Override
            public void onFadePressed()
            {
                switcherCommandSender.performAuto();
            }

            @Override
            public void onCutPressed()
            {
                switcherCommandSender.performCut();
            }
        });

        // connections for the switcher's status to get updated on the UI control panel
        this.switcherCommandSender.addUpstreamKeyOnAirConsumer(videoSwitcherControllerUi::setLyricsStatus);
        this.switcherCommandSender.addLiveAudioLevelDbConsumer(videoSwitcherControllerUi::setLiveAudioLevel);
        this.switcherCommandSender.addTransitionInProgressConsumer(videoSwitcherControllerUi::setFadeTransitionInProgressStatus);
        this.switcherCommandSender.addConnectionStatusConsumer(videoSwitcherControllerUi::setSwitcherConnectionStatus);
        this.switcherCommandSender.addPreviewVideoSourceChangedConsumer(previewIdx -> {
            videoSwitcherControllerUi.setPreviewSourceStatus(previewIdx);
            this.updatePreviewProgramColorsOnLeftRightCameraUis(); // reflect on UI when switcher changes its source
        });
        this.switcherCommandSender.addProgramVideoSourceChangedConsumer(programIdx -> {
            videoSwitcherControllerUi.setProgramSourceStatus(programIdx);
            this.updatePreviewProgramColorsOnLeftRightCameraUis(); // reflect on UI when switcher changes its source
        });

        // for each of the video source inputs ([name, index] repeated for each input) create corresponding program and preview buttons
        videoSwitcherControllerUi.setVideoSourceNamesAndSwitcherIndexes(settings.getSwitcherVideoNamesAndIndexes());

        // load saved presets for the PTZ camera control panels
        this.leftCameraControlPanel.loadPresetsSavedToDisk();
        this.rightCameraControlPanel.loadPresetsSavedToDisk();

        // after UI initialization is done, finally start the connection to the switcher
        this.switcherCommandSender.initialize(settings.getSwitcherIp());
    }

    /**
     * The PTZ camera UI panels (for LEFT and RIGHT cameras) can have their active colors
     * updated to reflect the state of the video switcher. This method gets called whenever
     * the switcher goes to a new state, so we can reflect preview/program states on the camera UI's.
     */
    private void updatePreviewProgramColorsOnLeftRightCameraUis()
    {
        // pull current preview/program sources
        final int previewIdx = this.switcherCommandSender.getCurrentVideoPreviewIdx();
        final int programIdx = this.switcherCommandSender.getCurrentVideoProgramIdx();
        final int leftCameraIdx = this.settings.getLeftCameraVideoIndex();
        final int rightCameraIdx = this.settings.getRightCameraVideoIndex();

        // update cameras
        this.leftCameraControlPanel.setActivePresetBackgroundColor(programIdx == leftCameraIdx ? Color.RED : previewIdx == leftCameraIdx ? Color.GREEN : null);
        this.rightCameraControlPanel.setActivePresetBackgroundColor(programIdx == rightCameraIdx ? Color.RED : previewIdx == rightCameraIdx ? Color.GREEN : null);
    }

    /**
     * JFormDesigner Auto-Generated Code.
     */
    @SuppressWarnings("all")
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        JPanel panel1 = new JPanel();
        parentSplitPane = new JSplitPane();
        JPanel panelTop = new JPanel();
        leftCameraControlPanel = new PtzCameraUi();
        rightCameraControlPanel = new PtzCameraUi();
        JPanel panel2 = new JPanel();
        switcherControlPanel = new JPanel();

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
            panel1.setBorder(new EmptyBorder(5, 0, 5, 0));
            panel1.setBackground(Color.black);
            panel1.setName("panel1");
            panel1.setLayout(new GridBagLayout());
            ((GridBagLayout)panel1.getLayout()).columnWidths = new int[] {0, 0};
            ((GridBagLayout)panel1.getLayout()).rowHeights = new int[] {0, 0};
            ((GridBagLayout)panel1.getLayout()).columnWeights = new double[] {1.0, 1.0E-4};
            ((GridBagLayout)panel1.getLayout()).rowWeights = new double[] {0.6, 1.0E-4};

            //======== parentSplitPane ========
            {
                parentSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
                parentSplitPane.setOpaque(false);
                parentSplitPane.setResizeWeight(0.5);
                parentSplitPane.setName("parentSplitPane");

                //======== panelTop ========
                {
                    panelTop.setOpaque(false);
                    panelTop.setBorder(new EmptyBorder(0, 0, 10, 0));
                    panelTop.setPreferredSize(new Dimension(673, 480));
                    panelTop.setName("panelTop");
                    panelTop.setLayout(new GridBagLayout());
                    ((GridBagLayout)panelTop.getLayout()).columnWidths = new int[] {0, 0, 0};
                    ((GridBagLayout)panelTop.getLayout()).rowHeights = new int[] {0, 0};
                    ((GridBagLayout)panelTop.getLayout()).columnWeights = new double[] {1.0, 1.0, 1.0E-4};
                    ((GridBagLayout)panelTop.getLayout()).rowWeights = new double[] {0.6, 1.0E-4};

                    //---- leftCameraControlPanel ----
                    leftCameraControlPanel.setBorder(new CompoundBorder(
                        new TitledBorder(new LineBorder(new Color(0xb200b2), 3, true), "LEFT Camera", TitledBorder.LEADING, TitledBorder.DEFAULT_POSITION,
                            new Font("Segoe UI", Font.BOLD, 20), Color.magenta),
                        new EmptyBorder(0, 5, 5, 5)));
                    leftCameraControlPanel.setPreferredSize(new Dimension(324, 500));
                    leftCameraControlPanel.setMinimumSize(new Dimension(324, 200));
                    leftCameraControlPanel.setName("leftCameraControlPanel");
                    panelTop.add(leftCameraControlPanel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 25), 0, 0));

                    //---- rightCameraControlPanel ----
                    rightCameraControlPanel.setBorder(new CompoundBorder(
                        new TitledBorder(new LineBorder(new Color(0xb200b2), 3, true), "RIGHT Camera", TitledBorder.LEADING, TitledBorder.DEFAULT_POSITION,
                            new Font("Segoe UI", Font.BOLD, 20), Color.magenta),
                        new EmptyBorder(0, 5, 5, 5)));
                    rightCameraControlPanel.setPreferredSize(new Dimension(324, 500));
                    rightCameraControlPanel.setMinimumSize(new Dimension(324, 200));
                    rightCameraControlPanel.setName("rightCameraControlPanel");
                    panelTop.add(rightCameraControlPanel, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 0), 0, 0));
                }
                parentSplitPane.setTopComponent(panelTop);

                //======== panel2 ========
                {
                    panel2.setOpaque(false);
                    panel2.setBorder(new EmptyBorder(5, 0, 0, 0));
                    panel2.setName("panel2");
                    panel2.setLayout(new BorderLayout());

                    //======== switcherControlPanel ========
                    {
                        switcherControlPanel.setBorder(new CompoundBorder(
                            new TitledBorder(new LineBorder(new Color(0xb200b2), 3, true), "Video Switcher", TitledBorder.LEADING, TitledBorder.DEFAULT_POSITION,
                                new Font("Segoe UI", Font.BOLD, 20), Color.magenta),
                            new EmptyBorder(5, 5, 5, 5)));
                        switcherControlPanel.setOpaque(false);
                        switcherControlPanel.setPreferredSize(new Dimension(24, 200));
                        switcherControlPanel.setMinimumSize(new Dimension(24, 200));
                        switcherControlPanel.setName("switcherControlPanel");
                        switcherControlPanel.setLayout(new BorderLayout());
                    }
                    panel2.add(switcherControlPanel, BorderLayout.CENTER);
                }
                parentSplitPane.setBottomComponent(panel2);
            }
            panel1.add(parentSplitPane, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));
        }
        contentPane.add(panel1, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JSplitPane parentSplitPane;
    private PtzCameraUi leftCameraControlPanel;
    private PtzCameraUi rightCameraControlPanel;
    private JPanel switcherControlPanel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
