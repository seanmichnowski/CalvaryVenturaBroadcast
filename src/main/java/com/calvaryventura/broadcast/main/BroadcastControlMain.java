package com.calvaryventura.broadcast.main;

import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.util.Objects;
import javax.swing.*;
import javax.swing.border.*;

import com.calvaryventura.broadcast.ptz.commander.PtzCameraCommandSender;
import com.calvaryventura.broadcast.ptz.ui.IPtzCameraUiCallbacks;
import com.calvaryventura.broadcast.ptz.ui.PtzCameraUi;
import com.calvaryventura.broadcast.switcher.BroadcastSwitcherController;
import com.calvaryventura.broadcast.switcher.IBroadcastSwitcherCallbacks;
import com.calvaryventura.broadcast.switcher.ui.BroadcastSwitcherUi;
import org.apache.log4j.BasicConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main UI and entry point.
 */
public class BroadcastControlMain extends JFrame
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final PtzCameraCommandSender wallCameraCommandSender;
    private final PtzCameraCommandSender overheadCameraCommandSender;
    private final BroadcastSwitcherController broadcastSwitcherController;
    private static final String UCAT_SETTINGS_JAR_RESOURCE = "/settings/broadcast_settings.txt"; // src/main/resources

    /**
     * Main entry point for application.
     */
    public static void main(String[] args)
    {
        BasicConfigurator.configure();
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
        this.setExtendedState(JFrame.MAXIMIZED_BOTH); // fullscreen
        this.setLocationRelativeTo(null);
        this.setVisible(true);

        // load settings from a config file
        String wallCameraIp = "";
        String overheadCameraIp = "";
        String broadcastIp = "";
        try
        {
            final BufferedReader in = new BufferedReader(new InputStreamReader(
                    Objects.requireNonNull(this.getClass().getResourceAsStream(UCAT_SETTINGS_JAR_RESOURCE))));
            String line;
            while ((line = in.readLine()) != null)
            {
                final String[] split = line.trim().split("=");
                switch (split[0].trim())
                {
                    case "wallCameraIp":
                        wallCameraIp = split[1].trim();
                        break;
                    case "overheadCameraIp":
                        overheadCameraIp = split[1].trim();
                        break;
                    case "broadcastSwitcherIp":
                        broadcastIp = split[1].trim();
                        break;
                    default:
                        throw new Exception("Unrecognized config file line: " + line);
                }
            }
            in.close();
        } catch (Exception e)
        {
            System.out.println("Unable to load settings from JAR resource");
            e.printStackTrace();
        }

        // initialize command senders
        this.broadcastSwitcherController = new BroadcastSwitcherController(broadcastIp, null); // TODO this.labelStatus);
        this.wallCameraCommandSender = new PtzCameraCommandSender(wallCameraIp, null); //this.labelCameraStatus);
        this.overheadCameraCommandSender = new PtzCameraCommandSender(overheadCameraIp, null); //this.labelCameraStatus);

        this.leftCameraControlPanel.setCallback(new IPtzCameraUiCallbacks()
        {
            @Override
            public boolean setPressed(int presetIdx)
            {
                return wallCameraCommandSender.setPreset(presetIdx);
            }

            @Override
            public boolean callPreviewPressed(int presetIdx)
            {
                return wallCameraCommandSender.callPreset(presetIdx);
            }

            @Override
            public boolean callProgramPressed(int presetIdx)
            {
                return wallCameraCommandSender.callPreset(presetIdx);
            }

            @Override
            public boolean panTilt(double pan, double tilt)
            {
                return false;
            }

            @Override
            public boolean focus(double focus)
            {
                return false;
            }

            @Override
            public boolean zoom(double zoom)
            {
                return false;
            }
        });

        this.rightCameraControlPanel.setCallback(new IPtzCameraUiCallbacks()
        {
            @Override
            public boolean setPressed(int presetIdx)
            {
                return wallCameraCommandSender.setPreset(presetIdx);
            }

            @Override
            public boolean callPreviewPressed(int presetIdx)
            {
                return wallCameraCommandSender.callPreset(presetIdx);
            }

            @Override
            public boolean callProgramPressed(int presetIdx)
            {
                return wallCameraCommandSender.callPreset(presetIdx);
            }

            @Override
            public boolean panTilt(double pan, double tilt)
            {
                return false;
            }

            @Override
            public boolean focus(double focus)
            {
                return false;
            }

            @Override
            public boolean zoom(double zoom)
            {
                return false;
            }
        });

        this.switcherControlPanel.setCallback(new IBroadcastSwitcherCallbacks()
        {
            @Override
            public void fadeToBlackActive(boolean active)
            {
                broadcastSwitcherController.sendHttpGetCommand(10);
            }

            @Override
            public void lyricsActive(boolean active)
            {
                broadcastSwitcherController.sendHttpGetCommand(18);
            }

            @Override
            public void activePreviewInputChanged(int inputIdx)
            {

            }

            @Override
            public void activeProgramInputChanged(int inputIdx)
            {
                // TODO
                broadcastSwitcherController.sendHttpGetCommand(20); // proclaim
                broadcastSwitcherController.sendHttpGetCommand(21); // booth
                broadcastSwitcherController.sendHttpGetCommand(22); // overhead
                broadcastSwitcherController.sendHttpGetCommand(23); // wall
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
            ((GridBagLayout)panel1.getLayout()).rowHeights = new int[] {0, 0, 0};
            ((GridBagLayout)panel1.getLayout()).columnWeights = new double[] {1.0, 1.0, 1.0E-4};
            ((GridBagLayout)panel1.getLayout()).rowWeights = new double[] {1.0, 0.4, 1.0E-4};

            //---- leftCameraControlPanel ----
            leftCameraControlPanel.setBorder(new CompoundBorder(
                new TitledBorder(new LineBorder(new Color(178, 0, 178), 3, true), "Wall Camera", TitledBorder.LEADING, TitledBorder.DEFAULT_POSITION,
                    new Font("Segoe UI", Font.BOLD, 20), Color.magenta),
                new EmptyBorder(5, 5, 5, 5)));
            leftCameraControlPanel.setName("leftCameraControlPanel");
            panel1.add(leftCameraControlPanel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 20, 25), 0, 0));

            //---- rightCameraControlPanel ----
            rightCameraControlPanel.setBorder(new CompoundBorder(
                new TitledBorder(new LineBorder(new Color(178, 0, 178), 3, true), "Overhead Camera", TitledBorder.LEADING, TitledBorder.DEFAULT_POSITION,
                    new Font("Segoe UI", Font.BOLD, 20), Color.magenta),
                new EmptyBorder(5, 5, 5, 5)));
            rightCameraControlPanel.setName("rightCameraControlPanel");
            panel1.add(rightCameraControlPanel, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 20, 0), 0, 0));

            //---- switcherControlPanel ----
            switcherControlPanel.setBorder(new CompoundBorder(
                new TitledBorder(new LineBorder(new Color(178, 0, 178), 3, true), "Broadcast Switcher Controls", TitledBorder.LEADING, TitledBorder.DEFAULT_POSITION,
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
