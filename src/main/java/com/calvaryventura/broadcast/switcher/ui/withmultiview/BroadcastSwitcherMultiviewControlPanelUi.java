package com.calvaryventura.broadcast.switcher.ui.withmultiview;

import com.calvaryventura.broadcast.settings.BroadcastSettings;
import com.calvaryventura.broadcast.switcher.ui.AbstractBroadcastSwitcherUi;
import com.calvaryventura.broadcast.switcher.ui.BroadcastSwitcherUiCallbacks;
import com.calvaryventura.broadcast.uiwidgets.DragScrollListener;
import com.github.kokorin.jaffree.LogLevel;
import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.Frame;
import com.github.kokorin.jaffree.ffmpeg.FrameConsumer;
import com.github.kokorin.jaffree.ffmpeg.FrameOutput;
import com.github.kokorin.jaffree.ffmpeg.Stream;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import org.apache.log4j.BasicConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

/**
 * Simple control panel for the video switcher. Contains functions like FadeToBlack, CUT, FADE, etc.
 * Call {@link #setVideoSourceNamesAndSwitcherIndexes(Map)} early in the initialization process
 * which will dynamically create buttons for program and preview, one button for each video source.
 */
public class BroadcastSwitcherMultiviewControlPanelUi extends AbstractBroadcastSwitcherUi
{
    // VLC macros
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String PATH_ENVIRONMENT_VARIABLE = "PATH";
    private static final String FFMPEG_INSTALLATION_NAME = "ffmpeg";
    private static final int FFMPEG_MINIMUM_VERSION = 4;
    private static final String FFMPEG_NOT_INSTALLED_ERROR_MSG = "<html>You must install the program 'FFMPEG' in order" +
            "<br>to view the multiview screen in real-time.<br>See: <u>https://www.geeksforgeeks.org/how-to-install-ffmpeg-on-windows/</u>" +
            "<br>Ensure you have at least FFMPEG version <b>" + FFMPEG_MINIMUM_VERSION + "</b></html>";

    // help display contents
    private static final String HELP_TEXT = "<html>Multiview screen available actions:<br><ul>" +
            "<li>Clicking the 'Preview' pane performs a fade</li>" +
            "<li>Clicking the 'Program' pane performs a cut</li>" +
            "<li>Single-Clicking any of the input panes puts it into Preview</li>" +
            "<li>Double-Clicking any of the input panes puts it into Program</li></ul>" +
            "<br>Double-clicking any camera preset name provides " +
            "a list of default options you can choose from. " +
            "When you select one of these, that camera preset is automatically stored, " +
            "you don't have to click the \"SET\" button after.</html>";

    // local vars
    private BufferedImage multiviewImage = null;
    private final JPanel videoCanvasPanel;
    private final Object multiviewImageDrawLock = new Object();
    private Future<?> ffmpegBackgroundThread;

    /**
     * Creates the basic UI elements and callbacks.
     */
    public BroadcastSwitcherMultiviewControlPanelUi()
    {
        // initialization
        super();
        this.initComponents();

        // set the help JTextPane to honor it's JFormDesigner font settings, and provide the text
        this.textPaneHelp.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        this.textPaneHelp.setText(HELP_TEXT);
        new DragScrollListener(this.textPaneHelp); // enable iPhone-like scrolling

        // button connections
        this.buttonVolume.addActionListener(e -> super.showVolumePopup());
        this.buttonToggleLyrics.addActionListener(e -> this.callbacks.onLyricsEnabled());
        this.buttonCloseHelp.addActionListener(e -> this.dialogHelp.setVisible(false));
        this.buttonhelp.addActionListener(e -> {
            this.dialogHelp.setLocationRelativeTo(this);
            this.dialogHelp.setVisible(true);
        });

        // create the panel for drawing the multiview buffered image
        this.videoCanvasPanel = new JPanel(null)
        {
            @Override
            public void paintComponent(Graphics g)
            {
                paintMultiviewPanel((Graphics2D) g);
            }
        };

        // if FFMPEG is installed, then add the video canvas to our program
        if (verifyFfmpegInstallationOnHostComputer())
        {
            this.add(this.videoCanvasPanel, BorderLayout.CENTER);
            this.initializeMouseSelectionOnMultiviewPanel(this.videoCanvasPanel);
            //this.startFfmpegMultiviewVideoDecodeThread(BroadcastSettings.getInst().getVideoSwitcherMultiviewVlcMediaPath(), BroadcastSettings.getInst().getVideoSwitcherMultiviewVideoSize());
        } else
        {
            // no FFMPEG installed!
            final JLabel errorMessage = new JLabel(FFMPEG_NOT_INSTALLED_ERROR_MSG);
            errorMessage.setForeground(Color.RED);
            errorMessage.setFont(new Font("Arial", Font.BOLD, 20));
            this.add(errorMessage); // GridBagLayout
        }

        // listen for resizing the video canvas; each resize must trigger a restart of FFMPEG, since we directly scale the video to the canvas size
        this.videoCanvasPanel.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                super.componentResized(e);
                startFfmpegMultiviewVideoDecodeThread(BroadcastSettings.getInst().getVideoSwitcherMultiviewVlcMediaPath(), videoCanvasPanel.getSize());
            }
        });
    }

    /**
     * Checks the current FFMPEG installation and verifies a correct minimum version.
     * See local static variables for minimum version and installation location.
     *
     * @return indication if FFMPEG is installed AND we have at least the minimum version
     */
    private static boolean verifyFfmpegInstallationOnHostComputer()
    {
        try
        {
            // find executable by name by searching all directories on the host computer's PATH
            final String absolutePath = Arrays.stream(System.getenv(PATH_ENVIRONMENT_VARIABLE).split(File.pathSeparator))
                    .map(directory -> new File(directory, FFMPEG_INSTALLATION_NAME))
                    .filter(file -> file.isFile() && file.canExecute()).findFirst()
                    .map(File::getAbsolutePath).orElseThrow(() -> new RuntimeException("Cannot locate VLC installation on host computer"));

            // get the ffmpeg version by invoking the program
            final Process process = Runtime.getRuntime().exec(absolutePath + " -version");
            try (final BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream())))
            {
                // pull the version from the command's output, example: "ffmpeg version 3.4.11-0ubuntu0.1 Copyright (c) 2000-2022 the FFmpeg developers"
                final String versionStr = in.lines()
                        .filter(l -> l.toLowerCase().contains("version")).findFirst()
                        .orElseThrow(() -> new RuntimeException("Cannot find the version of FFMPEG installation at " + absolutePath));

                // for the input string seen above, this would return "3.0.8" as vlcVersion, start and end being 4 and 5 respectively
                final int versionStrIdxStart = versionStr.indexOf("version");
                final int versionStrIdxEnd = versionStr.substring(versionStrIdxStart + 8).indexOf(" ");
                final String vlcVersion = versionStr.substring(versionStrIdxStart + 8, versionStrIdxStart + 8 + versionStrIdxEnd);

                // ensure the version of FFMPEG is at least the minimum version
                final boolean pass = Integer.parseInt(vlcVersion.substring(0, 1)) >= FFMPEG_MINIMUM_VERSION;
                logger.info("Installed FFMPEG version: {}... {} (>={})", vlcVersion, pass ? "OK" : "FAIL", FFMPEG_MINIMUM_VERSION);
                if (!pass)
                {
                    logger.info("Current FFMPEG installation: '{}', but we require at least version {} or higher", absolutePath, FFMPEG_MINIMUM_VERSION);
                }
                return pass;
            }
        } catch (Exception e)
        {
            logger.error("Unable to lookup valid FFMPEG installation on host computer: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Paints the multiview image into the local video canvas JPanel {@link #videoCanvasPanel}.
     * This method gets called automatically as the panel is repainted, only called from
     * the panel's overridden paint method. We are either painting the current multiview
     * buffered image, or if it's NULL, a simple message saying we're waiting for it.
     *
     * @param g2d graphics handle to the video canvas panel
     */
    private void paintMultiviewPanel(Graphics2D g2d)
    {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        synchronized (this.multiviewImageDrawLock)
        {
            if (this.multiviewImage != null)
            {
                g2d.drawImage(this.multiviewImage, null, null);
            } else
            {
                final String message = "Awaiting multiview video decode...";
                g2d.setFont(new Font("Arial", Font.BOLD, 16));
                g2d.setColor(Color.WHITE);
                g2d.drawString(message, 5, this.getHeight() / 4);
            }
        }
    }

    /**
     * Initialize the FFMPEG decoder to read the incoming RTSP multiview stream. We use the FFMPEG program
     * directly to scale the output video to exactly the JPanel canvas size. Therefore, we must call this
     * method again each time the playback canvas gets resized.
     *
     * @param rtspConnectionUrl RTSP address of the multiview stream, coming from the external encoder
     * @param videoCanvasSize   desired output size for the playing video
     */
    private void startFfmpegMultiviewVideoDecodeThread(String rtspConnectionUrl, Dimension videoCanvasSize)
    {
        if (this.ffmpegBackgroundThread != null)
        {
            this.ffmpegBackgroundThread.cancel(true);
            this.ffmpegBackgroundThread = null;
        }
        this.ffmpegBackgroundThread = Executors.newSingleThreadExecutor().submit(() -> {
            try
            {
                final FFmpeg fFmpeg = FFmpeg.atPath().addInput(UrlInput.fromUrl(rtspConnectionUrl))
                        .addOutput(FrameOutput.withConsumer(
                                        new FrameConsumer()
                                        {
                                            @Override
                                            public void consumeStreams(List<Stream> streams)
                                            {
                                            }

                                            @Override
                                            public void consume(Frame frame)
                                            {
                                                synchronized (multiviewImageDrawLock)
                                                {
                                                    multiviewImage = frame == null ? null : frame.getImage(); // check for end-of-stream
                                                }
                                                videoCanvasPanel.repaint();
                                            }
                                        })
                                .setFrameRate(20)
                                .disableStream(StreamType.AUDIO)
                                .disableStream(StreamType.SUBTITLE)
                                .disableStream(StreamType.DATA))
                        .setProgressListener(progress -> logger.info(progress.toString()))
                        .setLogLevel(LogLevel.WARNING)
                        .addArguments("-vf", String.format("scale=%d:%d", videoCanvasSize.width, videoCanvasSize.height))
                        .addArgument("-xerror")
                        .addArguments("-probesize", "32")
                        .addArguments("-movflags", "faststart")
                        .addArguments("-rtbufsize", "0")
                        .addArguments("-fflags", "nobuffer");
                logger.info("Starting FFMPEG background thread to decode multiview RTSP stream...");
                fFmpeg.execute();
            } catch (Throwable e)
            {
                // null-out the multiview image
                logger.info("Fatal FFMPEG error", e);
                synchronized (this.multiviewImageDrawLock)
                {
                    this.multiviewImage = null;
                }
            }
        });
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
    private void initializeMouseSelectionOnMultiviewPanel(JPanel videoCanvas)
    {
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
                    callbacks.onFadePressed(); // pressing in the "PREVIEW" pane triggers a fade transition
                } else if (e.getClickCount() == 1 && settings.getVideoSwitcherMultiviewProgramPaneGridBoxes().stream().anyMatch(gridPoint -> gridPoint.equals(mouseClickGridBox)))
                {
                    callbacks.onCutPressed(); // pressing in the "PROGRAM" pane triggers a cut transition
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
                                    callbacks.onPreviewSourceChanged(switcherSourceIdx);
                                } else if (e.getClickCount() == 2)
                                {
                                    logger.info("Changing to program video input idx={} name={}", switcherSourceIdx, switcherSourceName);
                                    callbacks.onPreviewSourceChanged(switcherSourceIdx);
                                    callbacks.onFadePressed();
                                }
                            });
                }
            }
        });
    }

    /**
     * Call this early in the initialization process to set the program/preview buttons in this UI.
     * A program and a preview button is created for each element in this map. Pressing these
     * buttons in the UI triggers that corresponding video source index to be fired in a callback.
     *
     * @param videoSourceNamesAndSwitcherIndexes lists the [name, index] of each type of buttons to create
     */
    public void setVideoSourceNamesAndSwitcherIndexes(Map<String, Integer> videoSourceNamesAndSwitcherIndexes)
    {

    }

    /**
     * @param connected indication if we have established connection with the switcher
     */
    @Override
    public void setSwitcherConnectionStatus(boolean connected)
    {
        this.labelConnectionStatus.setText(connected ? "Switcher connected :)" : "Switcher not connected :(");
        this.labelConnectionStatus.setForeground(connected ? Color.GREEN : Color.RED);
    }

    /**
     * @param active indication the transition is in progress
     */
    @Override
    public void setFadeTransitionInProgressStatus(boolean active)
    {
        this.labelTransitionInProgress.setForeground(active ? Color.YELLOW : Color.BLACK);
    }

    /**
     * @param active indication the lyrics are displayed on-screen
     */
    @Override
    public void setLyricsStatus(boolean active)
    {
        this.buttonToggleLyrics.setBackground(active ? Color.RED : Color.DARK_GRAY);
    }

    /**
     * Standalone test for this multiview panel.
     *
     * @param args no program arguments
     */
    public static void main(String[] args)
    {
        // logger configuration
        BasicConfigurator.configure();

        // create the test panel
        final BroadcastSwitcherMultiviewControlPanelUi uut = new BroadcastSwitcherMultiviewControlPanelUi();
        uut.setCallbacks(new BroadcastSwitcherUiCallbacks()
        {
            @Override
            public void onPreviewSourceChanged(int previewSourceChanged)
            {
                logger.info("PREVIEW SOURCE: {}", previewSourceChanged);
            }

            @Override
            public void onProgramSourceChanged(int programSourceChanged)
            {
                logger.info("PROGRAM SOURCE: {}", programSourceChanged);
            }

            @Override
            public void setSwitcherSendingLiveAudio(boolean enable)
            {
                logger.info("Switcher sends live audio levels: {}", enable);
            }

            @Override
            public void setAudioLevelPercent(double percent0to1)
            {
                logger.info("Audio level commanded: {}%", percent0to1);
            }

            @Override
            public void onLyricsEnabled()
            {
                logger.info("Lyrics");
            }

            @Override
            public void onFadePressed()
            {
                logger.info("FADE");
            }

            @Override
            public void onCutPressed()
            {
                logger.info("CUT");
            }
        });

        // add the test panel to a standalone frame and display it
        final JFrame f = new JFrame("Multiview Test");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.add(uut);
        f.setSize(800, 600);
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }

    /**
     * JFormDesigner Auto-Generated Code.
     */
    @SuppressWarnings("all")
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        JPanel panel1 = new JPanel();
        JPanel panel2 = new JPanel();
        labelTransitionInProgress = new JLabel();
        labelConnectionStatus = new JLabel();
        buttonToggleLyrics = new JButton();
        buttonVolume = new JButton();
        buttonhelp = new JButton();
        dialogHelp = new JFrame();
        JPanel panelHelpContents = new JPanel();
        JScrollPane scrollPaneHelp = new JScrollPane();
        textPaneHelp = new JTextPane();
        buttonCloseHelp = new JButton();

        //======== this ========
        setOpaque(false);
        setName("this");
        setLayout(new BorderLayout());

        //======== panel1 ========
        {
            panel1.setOpaque(false);
            panel1.setBorder(new EmptyBorder(1, 0, 10, 0));
            panel1.setPreferredSize(new Dimension(100, 48));
            panel1.setMinimumSize(new Dimension(100, 0));
            panel1.setName("panel1");
            panel1.setLayout(new GridBagLayout());
            ((GridBagLayout)panel1.getLayout()).columnWidths = new int[] {0, 0, 0, 0, 0};
            ((GridBagLayout)panel1.getLayout()).rowHeights = new int[] {0, 0};
            ((GridBagLayout)panel1.getLayout()).columnWeights = new double[] {0.0, 0.0, 0.0, 1.0, 1.0E-4};
            ((GridBagLayout)panel1.getLayout()).rowWeights = new double[] {1.0, 1.0E-4};

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
                labelTransitionInProgress.setFont(new Font("Segoe UI", Font.PLAIN, 12));
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
                labelConnectionStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                labelConnectionStatus.setHorizontalAlignment(SwingConstants.LEFT);
                labelConnectionStatus.setMaximumSize(new Dimension(150, 15));
                labelConnectionStatus.setName("labelConnectionStatus");
                panel2.add(labelConnectionStatus, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 0), 0, 0));
            }
            panel1.add(panel2, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.VERTICAL,
                new Insets(0, 0, 0, 0), 0, 0));

            //---- buttonToggleLyrics ----
            buttonToggleLyrics.setText("Toggle Lyrics");
            buttonToggleLyrics.setForeground(Color.cyan);
            buttonToggleLyrics.setBackground(Color.darkGray);
            buttonToggleLyrics.setFont(new Font("Segoe UI", Font.BOLD, 20));
            buttonToggleLyrics.setName("buttonToggleLyrics");
            panel1.add(buttonToggleLyrics, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 20), 0, 0));

            //---- buttonVolume ----
            buttonVolume.setText("Volume");
            buttonVolume.setForeground(Color.cyan);
            buttonVolume.setBackground(Color.darkGray);
            buttonVolume.setFont(new Font("Segoe UI", Font.BOLD, 20));
            buttonVolume.setPreferredSize(new Dimension(120, 40));
            buttonVolume.setMinimumSize(new Dimension(120, 30));
            buttonVolume.setName("buttonVolume");
            panel1.add(buttonVolume, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 20), 0, 0));

            //---- buttonhelp ----
            buttonhelp.setText("Help");
            buttonhelp.setForeground(Color.lightGray);
            buttonhelp.setBackground(Color.black);
            buttonhelp.setFont(new Font("Segoe UI", Font.BOLD, 16));
            buttonhelp.setHorizontalAlignment(SwingConstants.CENTER);
            buttonhelp.setIcon(new ImageIcon(getClass().getResource("/icons/people_connection_32x32.png")));
            buttonhelp.setName("buttonhelp");
            panel1.add(buttonhelp, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 20), 0, 0));
        }
        add(panel1, BorderLayout.NORTH);

        //======== dialogHelp ========
        {
            dialogHelp.setTitle("Broadcast Multiview Instructions");
            dialogHelp.setPreferredSize(new Dimension(550, 300));
            dialogHelp.setAlwaysOnTop(true);
            dialogHelp.setIconImage(new ImageIcon(getClass().getResource("/icons/people_connection_32x32.png")).getImage());
            dialogHelp.setName("dialogHelp");
            Container dialogHelpContentPane = dialogHelp.getContentPane();
            dialogHelpContentPane.setLayout(new BorderLayout());

            //======== panelHelpContents ========
            {
                panelHelpContents.setBackground(new Color(0xffccff));
                panelHelpContents.setBorder(new EmptyBorder(20, 20, 10, 20));
                panelHelpContents.setName("panelHelpContents");
                panelHelpContents.setLayout(new GridBagLayout());
                ((GridBagLayout)panelHelpContents.getLayout()).columnWidths = new int[] {0, 0};
                ((GridBagLayout)panelHelpContents.getLayout()).rowHeights = new int[] {0, 0, 0};
                ((GridBagLayout)panelHelpContents.getLayout()).columnWeights = new double[] {1.0, 1.0E-4};
                ((GridBagLayout)panelHelpContents.getLayout()).rowWeights = new double[] {1.0, 0.0, 1.0E-4};

                //======== scrollPaneHelp ========
                {
                    scrollPaneHelp.setBackground(Color.darkGray);
                    scrollPaneHelp.setOpaque(false);
                    scrollPaneHelp.setBorder(null);
                    scrollPaneHelp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                    scrollPaneHelp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
                    scrollPaneHelp.setName("scrollPaneHelp");

                    //---- textPaneHelp ----
                    textPaneHelp.setBackground(new Color(0xffccff));
                    textPaneHelp.setForeground(new Color(0x9900ff));
                    textPaneHelp.setFont(new Font("Ubuntu", Font.BOLD, 18));
                    textPaneHelp.setBorder(null);
                    textPaneHelp.setContentType("text/html");
                    textPaneHelp.setEditable(false);
                    textPaneHelp.setCaretColor(new Color(0xffccff));
                    textPaneHelp.setName("textPaneHelp");
                    scrollPaneHelp.setViewportView(textPaneHelp);
                }
                panelHelpContents.add(scrollPaneHelp, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 10, 0), 0, 0));

                //---- buttonCloseHelp ----
                buttonCloseHelp.setText("Close");
                buttonCloseHelp.setForeground(new Color(0x9900ff));
                buttonCloseHelp.setBackground(Color.darkGray);
                buttonCloseHelp.setFont(new Font("Segoe UI", Font.BOLD, 20));
                buttonCloseHelp.setPreferredSize(new Dimension(120, 50));
                buttonCloseHelp.setOpaque(false);
                buttonCloseHelp.setName("buttonCloseHelp");
                panelHelpContents.add(buttonCloseHelp, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                    GridBagConstraints.EAST, GridBagConstraints.VERTICAL,
                    new Insets(0, 0, 0, 0), 0, 0));
            }
            dialogHelpContentPane.add(panelHelpContents, BorderLayout.CENTER);
            dialogHelp.pack();
            dialogHelp.setLocationRelativeTo(dialogHelp.getOwner());
        }
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JLabel labelTransitionInProgress;
    private JLabel labelConnectionStatus;
    private JButton buttonToggleLyrics;
    private JButton buttonVolume;
    private JButton buttonhelp;
    private JFrame dialogHelp;
    private JTextPane textPaneHelp;
    private JButton buttonCloseHelp;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
