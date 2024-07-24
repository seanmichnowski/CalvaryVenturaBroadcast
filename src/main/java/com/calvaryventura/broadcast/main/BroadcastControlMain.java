package com.calvaryventura.broadcast.main;

import java.awt.*;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import javax.swing.*;
import javax.swing.border.*;

import com.calvaryventura.broadcast.ptzcamera.control.PtzCameraController;
import com.calvaryventura.broadcast.ptzcamera.ui.IPtzCameraControllerUiCallback;
import com.calvaryventura.broadcast.ptzcamera.ui.PtzCameraControllerUi;
import com.calvaryventura.broadcast.settings.BroadcastSettings;
import com.calvaryventura.broadcast.switcher.control.BlackmagicAtemSwitcherUserLayer;
import com.calvaryventura.broadcast.switcher.ui.AbstractBroadcastSwitcherUi;
import com.calvaryventura.broadcast.switcher.ui.BroadcastSwitcherUiCallbacks;
import com.calvaryventura.broadcast.switcher.ui.withmultiview.BroadcastSwitcherMultiviewControlPanelUi;
import com.calvaryventura.broadcast.switcher.ui.withoutmultiview.BroadcastSwitcherControlPanelUi;
import com.calvaryventura.broadcast.uiwidgets.SplitPaneBarColorizer;
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
    private final List<PtzCameraController> ptzCameraControllers = new ArrayList<>();
    private final PtzCameraControllerUi ptzCameraUi;


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
        // UI initialization
        this.initComponents();
        this.setLocationRelativeTo(null);
        this.setTitle(BroadcastSettings.getInst().getProgramTitle());
        SplitPaneBarColorizer.setSplitPaneBarStriped(this.splitPaneMainContent, Color.GREEN);

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
        final AbstractBroadcastSwitcherUi videoSwitcherControllerUi = BroadcastSettings.getInst().isVideoSwitcherMultiviewEnabled()
                ? new BroadcastSwitcherMultiviewControlPanelUi() : new BroadcastSwitcherControlPanelUi();
        this.switcherControlPanel.add(videoSwitcherControllerUi, BorderLayout.CENTER);
        this.switcherControlPanel.setBorder(TitledBorderCreator.createTitledBorder("Video Switcher"));

        // connections for the switcher's UI control panel to actually send commands
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
                switcherCommandSender.toggleKeyerOnAirEnabled();
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
            this.updatePreviewProgramColorsOnCameraUis(); // reflect on UI when switcher changes its source
        });
        this.switcherCommandSender.addProgramVideoSourceChangedConsumer(programIdx -> {
            videoSwitcherControllerUi.setProgramSourceStatus(programIdx);
            this.updatePreviewProgramColorsOnCameraUis(); // reflect on UI when switcher changes its source
        });

        // for each of the video source inputs ([name, index] repeated for each input) create corresponding program and preview buttons
        videoSwitcherControllerUi.setVideoSourceNamesAndSwitcherIndexes(BroadcastSettings.getInst().getSwitcherVideoNamesAndIndexes());

        // after UI initialization is done, finally start the connection to the switcher
        this.switcherCommandSender.initialize(BroadcastSettings.getInst().getSwitcherIp());

        // finally, show the frame maximized!
        this.setVisible(true);
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
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
     * JFormDesigner Auto-Generated Code.
     */
    @SuppressWarnings("all")
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        splitPaneMainContent = new JSplitPane();
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

        //======== splitPaneMainContent ========
        {
            splitPaneMainContent.setBorder(new EmptyBorder(0, 0, 5, 0));
            splitPaneMainContent.setBackground(Color.black);
            splitPaneMainContent.setOrientation(JSplitPane.VERTICAL_SPLIT);
            splitPaneMainContent.setResizeWeight(0.6);
            splitPaneMainContent.setDividerSize(20);
            splitPaneMainContent.setName("splitPaneMainContent");

            //======== parentPtzCamerasPanel ========
            {
                parentPtzCamerasPanel.setOpaque(false);
                parentPtzCamerasPanel.setBorder(BorderFactory.createEmptyBorder());
                parentPtzCamerasPanel.setPreferredSize(new Dimension(673, 10));
                parentPtzCamerasPanel.setMinimumSize(new Dimension(0, 0));
                parentPtzCamerasPanel.setRequestFocusEnabled(false);
                parentPtzCamerasPanel.setName("parentPtzCamerasPanel");
                parentPtzCamerasPanel.setLayout(new BorderLayout());
            }
            splitPaneMainContent.setTopComponent(parentPtzCamerasPanel);

            //======== switcherControlPanel ========
            {
                switcherControlPanel.setOpaque(false);
                switcherControlPanel.setPreferredSize(new Dimension(24, 100));
                switcherControlPanel.setMinimumSize(new Dimension(24, 0));
                switcherControlPanel.setName("switcherControlPanel");
                switcherControlPanel.setLayout(new BorderLayout());
            }
            splitPaneMainContent.setBottomComponent(switcherControlPanel);
        }
        contentPane.add(splitPaneMainContent, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JSplitPane splitPaneMainContent;
    private JPanel parentPtzCamerasPanel;
    private JPanel switcherControlPanel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
