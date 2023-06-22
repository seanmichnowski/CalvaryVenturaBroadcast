package com.calvaryventura.broadcast.main;

import java.awt.*;
import java.lang.invoke.MethodHandles;
import javax.swing.*;
import javax.swing.border.*;

import com.calvaryventura.broadcast.ptzcamera.control.PtzCameraController;
import com.calvaryventura.broadcast.ptzcamera.ui.IPtzCameraUiCallbacks;
import com.calvaryventura.broadcast.ptzcamera.ui.PtzCameraUi;
import com.calvaryventura.broadcast.settings.BroadcastSettings;
import com.calvaryventura.broadcast.switcher.control.BlackmagicAtemSwitcherUserLayer;
import com.calvaryventura.broadcast.switcher.ui.BroadcastSwitcherUi;
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
        // UI initialization
        this.initComponents();
        this.setLocationRelativeTo(null);
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.setVisible(true);

        // initialize LEFT/RIGHT camera command senders
        this.settings = new BroadcastSettings();
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

        // connections for the switcher's UI control panel to actually send commands
        this.switcherControlPanel.onAutoPressed(e -> this.switcherCommandSender.performAuto());
        this.switcherControlPanel.onCutPressed(e -> this.switcherCommandSender.performCut());
        this.switcherControlPanel.onLyricsPressed(this.switcherCommandSender::setKeyerOnAirEnabled);
        this.switcherControlPanel.onFadeToBlackPressed(e -> this.switcherCommandSender.performFadeToBlack());
        this.switcherControlPanel.onPreviewSourceChanged(this.switcherCommandSender::setPreviewVideo);
        this.switcherControlPanel.onProgramSourceChanged(this.switcherCommandSender::setProgramVideo);

        // connections for the switcher's status to get updated on the UI control panel
        this.switcherCommandSender.addUpstreamKeyOnAirConsumer(keyOnAir -> this.switcherControlPanel.setLyricsStatus(keyOnAir));
        this.switcherCommandSender.addFadeToBlackActiveAndTransitionConsumer((active, transition) -> this.switcherControlPanel.setFadeToBlackOnStatus(active, transition));
        this.switcherCommandSender.addTransitionInProgressConsumer(progress -> this.switcherControlPanel.setFadeTransitionInProgressStatus(progress));
        this.switcherCommandSender.addConnectionStatusConsumer(connected -> this.switcherControlPanel.setSwitcherConnectionStatus(connected));
        this.switcherCommandSender.addPreviewVideoSourceChangedConsumer(previewIdx -> {
            this.switcherControlPanel.setPreviewSourceStatus(previewIdx);
            this.updatePreviewProgramColorsOnLeftRightCameraUis(); // reflect on UI when switcher changes its source
        });
        this.switcherCommandSender.addProgramVideoSourceChangedConsumer(programIdx -> {
            this.switcherControlPanel.setProgramSourceStatus(programIdx);
            this.updatePreviewProgramColorsOnLeftRightCameraUis(); // reflect on UI when switcher changes its source
        });

        // for each of the video source inputs ([name, index] repeated for each input) create corresponding program and preview buttons
        this.switcherControlPanel.setVideoSourceNamesAndSwitcherIndexes(settings.getSwitcherVideoNamesAndIndexes());

        // load saved presets for the PTZ camera control panels
        this.leftCameraControlPanel.loadPresetsSavedToDisk();
        this.rightCameraControlPanel.loadPresetsSavedToDisk();

        // after all the UI initialization is done, finally start the connection to the switcher
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
        leftCameraControlPanel = new PtzCameraUi();
        rightCameraControlPanel = new PtzCameraUi();
        switcherControlPanel = new BroadcastSwitcherUi();

        //======== this ========
        setTitle("Calvary Ventura Camera Control");
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
            ((GridBagLayout)panel1.getLayout()).columnWidths = new int[] {0, 0, 0};
            ((GridBagLayout)panel1.getLayout()).rowHeights = new int[] {0, 250, 0};
            ((GridBagLayout)panel1.getLayout()).columnWeights = new double[] {1.0, 1.0, 1.0E-4};
            ((GridBagLayout)panel1.getLayout()).rowWeights = new double[] {1.0, 0.2, 1.0E-4};

            //---- leftCameraControlPanel ----
            leftCameraControlPanel.setBorder(new CompoundBorder(
                new TitledBorder(new LineBorder(new Color(0xb200b2), 3, true), "LEFT Camera", TitledBorder.LEADING, TitledBorder.DEFAULT_POSITION,
                    new Font("Segoe UI", Font.BOLD, 20), Color.magenta),
                new EmptyBorder(0, 5, 5, 5)));
            leftCameraControlPanel.setName("leftCameraControlPanel");
            panel1.add(leftCameraControlPanel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 20, 25), 0, 0));

            //---- rightCameraControlPanel ----
            rightCameraControlPanel.setBorder(new CompoundBorder(
                new TitledBorder(new LineBorder(new Color(0xb200b2), 3, true), "RIGHT Camera", TitledBorder.LEADING, TitledBorder.DEFAULT_POSITION,
                    new Font("Segoe UI", Font.BOLD, 20), Color.magenta),
                new EmptyBorder(0, 5, 5, 5)));
            rightCameraControlPanel.setName("rightCameraControlPanel");
            panel1.add(rightCameraControlPanel, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 20, 0), 0, 0));

            //---- switcherControlPanel ----
            switcherControlPanel.setBorder(new CompoundBorder(
                new TitledBorder(new LineBorder(new Color(0xb200b2), 3, true), "Video Switcher", TitledBorder.LEADING, TitledBorder.DEFAULT_POSITION,
                    new Font("Segoe UI", Font.BOLD, 20), Color.magenta),
                new EmptyBorder(5, 5, 5, 5)));
            switcherControlPanel.setName("switcherControlPanel");
            panel1.add(switcherControlPanel, new GridBagConstraints(0, 1, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));
        }
        contentPane.add(panel1, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private PtzCameraUi leftCameraControlPanel;
    private PtzCameraUi rightCameraControlPanel;
    private BroadcastSwitcherUi switcherControlPanel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
