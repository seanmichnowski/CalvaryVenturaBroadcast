package com.calvaryventura.broadcast.main;

import java.awt.*;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.ArrayList;
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
import com.calvaryventura.broadcast.uiwidgets.TitledBorderCreator;
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
    private final List<PtzCameraUi> ptzCameraUis = new ArrayList<>();
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

        // make all scroll bars wider so they are easier to grab on a touchscreen
        UIManager.put("ScrollBar.width", 30);

        // initialize PTZ camera(s)
        this.settings.getPtzCameraNamesIps().forEach((ptzCameraName, ptzCameraSocketAddress) ->
        {
            // for each PTZ camera, create the controller and create the UI
            final PtzCameraUi ptzCameraUi = new PtzCameraUi(ptzCameraName);
            final PtzCameraController ptzCameraController = new PtzCameraController(ptzCameraName, ptzCameraSocketAddress, ptzCameraUi::setCameraConnectionStatus);
            this.ptzCameraUis.add(ptzCameraUi);

            // connect LEFT camera UI panel actions
            ptzCameraUi.setCallback(new IPtzCameraUiCallbacks()
            {
                @Override
                public boolean setPressed(int presetIdx)
                {
                    return ptzCameraController.savePreset(presetIdx);
                }

                @Override
                public boolean callPressed(int presetIdx)
                {
                    // attempt to move the camera, also show this camera in the preview window
                    final boolean cameraMoveOk = ptzCameraController.moveToPreset(presetIdx);
                    switcherCommandSender.setPreviewVideo(settings.getLeftCameraVideoIndex());
                    SwingUtilities.invokeLater(() -> updatePreviewProgramColorsOnLeftRightCameraUis());
                    return cameraMoveOk;
                }

                @Override
                public boolean panTilt(double pan, double tilt)
                {
                    return ptzCameraController.panAndTilt(pan, tilt);
                }

                @Override
                public boolean zoom(double zoom)
                {
                    return ptzCameraController.changeZoom(zoom);
                }
            });

            // load presets for the camera's UI
            ptzCameraUi.loadPresetsSavedToDisk();
        });

        // add all PTZ camera UI panels we just created to the parent panel
        this.ptzCameraUis.forEach(ui -> this.parentPtzCamerasPanel.add(ui));
        this.parentPtzCamerasPanel.revalidate();

        // set up the video switcher control UI implementation
        logger.info("Starting connection to video switcher, multiview={}", this.settings.isVideoSwitcherMultiviewEnabled() ? "enabled" : "disabled");
        final AbstractBroadcastSwitcherUi videoSwitcherControllerUi = this.settings.isVideoSwitcherMultiviewEnabled()
                ? new BroadcastSwitcherMultiviewControlPanelUi() : new BroadcastSwitcherControlPanelUi();
        this.switcherControlPanel.add(videoSwitcherControllerUi, BorderLayout.CENTER);
        this.switcherControlPanel.setBorder(TitledBorderCreator.createTitledBorder("Video Switcher"));

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

        // update all PTZ camera UI panels to potentially show the current video switcher's PREVIEW/PROGRAM state
        for (int i = 0; i < this.ptzCameraUis.size(); i++)
        {
            final int videoSwitcherInputIdxForThisPtzCamera = this.settings.getPtzCameraSwitcherInputIndexes().get(i);
            this.ptzCameraUis.get(i).setActivePresetBackgroundColor(programIdx == videoSwitcherInputIdxForThisPtzCamera ? Color.RED : previewIdx == videoSwitcherInputIdxForThisPtzCamera ? Color.GREEN : null);
        }
    }

    /**
     * JFormDesigner Auto-Generated Code.
     */
    @SuppressWarnings("all")
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        JPanel panelMainContent = new JPanel();
        parentPtzCamerasPanel = new JPanel();
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

        //======== panelMainContent ========
        {
            panelMainContent.setBorder(new EmptyBorder(5, 0, 5, 0));
            panelMainContent.setBackground(Color.black);
            panelMainContent.setName("panelMainContent");
            panelMainContent.setLayout(new GridBagLayout());
            ((GridBagLayout)panelMainContent.getLayout()).columnWidths = new int[] {0, 0};
            ((GridBagLayout)panelMainContent.getLayout()).rowHeights = new int[] {0, 0, 0};
            ((GridBagLayout)panelMainContent.getLayout()).columnWeights = new double[] {1.0, 1.0E-4};
            ((GridBagLayout)panelMainContent.getLayout()).rowWeights = new double[] {1.0, 1.0, 1.0E-4};

            //======== parentPtzCamerasPanel ========
            {
                parentPtzCamerasPanel.setOpaque(false);
                parentPtzCamerasPanel.setBorder(BorderFactory.createEmptyBorder());
                parentPtzCamerasPanel.setPreferredSize(new Dimension(673, 480));
                parentPtzCamerasPanel.setMinimumSize(new Dimension(0, 0));
                parentPtzCamerasPanel.setName("parentPtzCamerasPanel");
                parentPtzCamerasPanel.setLayout(new GridLayout(1, 0, 20, 0));
            }
            panelMainContent.add(parentPtzCamerasPanel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 15, 0), 0, 0));

            //======== switcherControlPanel ========
            {
                switcherControlPanel.setOpaque(false);
                switcherControlPanel.setPreferredSize(new Dimension(24, 100));
                switcherControlPanel.setMinimumSize(new Dimension(24, 0));
                switcherControlPanel.setName("switcherControlPanel");
                switcherControlPanel.setLayout(new BorderLayout());
            }
            panelMainContent.add(switcherControlPanel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));
        }
        contentPane.add(panelMainContent, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel parentPtzCamerasPanel;
    private JPanel switcherControlPanel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
