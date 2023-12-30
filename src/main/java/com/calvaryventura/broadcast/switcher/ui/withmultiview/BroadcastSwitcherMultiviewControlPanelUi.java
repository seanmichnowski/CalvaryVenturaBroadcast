package com.calvaryventura.broadcast.switcher.ui.withmultiview;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

import com.calvaryventura.broadcast.settings.BroadcastSettings;
import com.calvaryventura.broadcast.switcher.ui.AbstractBroadcastSwitcherUi;
import com.calvaryventura.broadcast.switcher.ui.BroadcastSwitcherUiCallbacks;
import com.calvaryventura.broadcast.uiwidgets.DragScrollListener;
import org.apache.log4j.BasicConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.caprica.vlcj.media.Media;
import uk.co.caprica.vlcj.media.MediaEventAdapter;
import uk.co.caprica.vlcj.media.MediaParsedStatus;
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Map;
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
    private static final String VLC_INSTALLATION_NAME = "vlc";
    private static final int VLC_MINIMUM_VERSION = 3;
    private static final String VLC_NOT_INSTALLED_ERROR_MSG = "<html>You must install the program 'VLC' in order" +
            "<br>to view the multiview screen in real-time.<br>See: <u>https://www.videolan.org/vlc/#download</u></html>";

    // help display contents
    private static final String HELP_TEXT = "<html>Multiview screen available actions:<br><ul>" +
            "<li>Clicking the 'Preview' pane performs a fade</li>" +
            "<li>Clicking the 'Program' pane performs a cut</li>" +
            "<li>Single-Clicking any of the input panes puts it into Preview</li>" +
            "<li>Double-Clicking any of the input panes puts it into Program</li>" +
            "</ul><br>Double-clicking any camera preset name provides" +
            "a list of default options you can choose from.<br></html>";

    // local vars
    private final BroadcastSettings broadcastSettings;
    private final EmbeddedMediaPlayer player;
    private final Rectangle videoPlaybackRectangleWithinVideoCanvas = new Rectangle();

    /**
     * Creates the basic UI elements and callbacks.
     */
    public BroadcastSwitcherMultiviewControlPanelUi()
    {
        // initialization
        this.initComponents();
        this.broadcastSettings = BroadcastSettings.getInst();

        // set the help JTextPane to honor it's JFormDesigner font settings, and provide the text
        this.textPaneHelp.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        this.textPaneHelp.setText(HELP_TEXT);
        new DragScrollListener(this.textPaneHelp); // enable iPhone-like scrolling

        // button connections
        this.buttonFadeToBlack.addActionListener(e -> this.callbacks.onFadeToBlack());
        this.buttonToggleLyrics.addActionListener(e -> this.callbacks.onLyricsEnabled());
        this.buttonCloseHelp.addActionListener(e -> this.dialogHelp.setVisible(false));
        this.buttonhelp.addActionListener(e -> {
            this.dialogHelp.setLocationRelativeTo(this);
            this.dialogHelp.setVisible(true);
        });

        // create the video player
        final EmbeddedMediaPlayerComponent videoCanvas = new EmbeddedMediaPlayerComponent();
        this.player = videoCanvas.mediaPlayer();

        // ensure we have VLC before attempting to start playback, otherwise show an error
        if (true) // TODO verifyVlcInstallationOnHostComputer())
        {
            this.panelVideo.add(videoCanvas, BorderLayout.CENTER);
            this.initializeMouseSelectionOnMultiviewPanel(videoCanvas);
            this.initializeVlcPlayback(videoCanvas);
        } else
        {
            final JLabel errorMessage = new JLabel(VLC_NOT_INSTALLED_ERROR_MSG);
            errorMessage.setForeground(Color.RED);
            errorMessage.setFont(new Font("Arial", Font.BOLD, 20));
            this.panelVideo.add(errorMessage, BorderLayout.CENTER);
        }
    }

    /**
     * Checks the current VLC installation and verifies a correct minimum version.
     * See local static variables for minimum version and installation location.
     *
     * @return indication if VLC is installed AND we have at least the minimum version.
     */
    private static boolean verifyVlcInstallationOnHostComputer()
    {
        try
        {
            // get the vlc version by invoking the program
            final Process process = Runtime.getRuntime().exec(VLC_INSTALLATION_NAME + " --version");
            try (final BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream())))
            {
                // pull the version from the command's output, example: "VLC version 3.0.8 Vetinari (3.0.8-0-gf350b6b5a7)"
                final String versionStr = in.lines()
                        .filter(l -> l.toLowerCase().contains("version")).findFirst()
                        .orElseThrow(() -> new RuntimeException("Cannot find the version of VLC installation '" + VLC_INSTALLATION_NAME + "'. Check path or installation."));

                // for the input string seen above, this would return "3.0.8" as vlcVersion, start and end being 4 and 5 respectively
                final int versionStrIdxStart = versionStr.indexOf("version");
                final int versionStrIdxEnd = versionStr.substring(versionStrIdxStart + 8).indexOf(" ");
                final String vlcVersion = versionStr.substring(versionStrIdxStart + 8, versionStrIdxStart + 8 + versionStrIdxEnd);

                // ensure the version of VLC is at least the minimum version
                final boolean pass = Integer.parseInt(vlcVersion.substring(0, 1)) >= VLC_MINIMUM_VERSION;
                logger.info("Installed VLC version: {}... {} (>={})", vlcVersion, pass ? "OK" : "FAIL", VLC_MINIMUM_VERSION);
                if (!pass)
                {
                    logger.info("VLC is installed, but we require at least version {} or higher", VLC_MINIMUM_VERSION);
                }
                return pass;
            }
        } catch (Exception e)
        {
            logger.error("Unable to lookup valid VLC installation on host computer", e);
            return false;
        }
    }

    /**
     * TODO
     */
    private void initializeMouseSelectionOnMultiviewPanel(EmbeddedMediaPlayerComponent videoCanvas)
    {
        videoCanvas.videoSurfaceComponent().addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                super.mousePressed(e);

                // Now, we had previously calculated a rectangle of pixels representing the playing video WITHIN the parent canvas.
                // Since we are listening to mouse events on the parent canvas, we can take the X/Y coordinates of the mouse press,
                // and compare them to the inscribed video playback rectangle, and find a percentage that the mouse lies WITHIN the rectangle.
                final double xPercent = (e.getX() - videoPlaybackRectangleWithinVideoCanvas.getMinX()) / videoPlaybackRectangleWithinVideoCanvas.getWidth();
                final double yPercent = (e.getY() - videoPlaybackRectangleWithinVideoCanvas.getMinY()) / videoPlaybackRectangleWithinVideoCanvas.getHeight();
                if (xPercent < 0 || xPercent > 1.0 || yPercent < 0 || yPercent > 1.0)
                {
                    // the mouse press lies outside the inscribed rectangle
                    logger.info("OUTSIDE!!!!"); // TODO removeme
                    return;
                }

                // based on the mouse percent INTO the playing video's rectangle, determine which grid box WITHIN the video we clicked inside (starts at 0 for X and Y and referenced from the upper-left corner)
                final int xGridBoxMouseLoc = (int) (xPercent * broadcastSettings.getVideoSwitcherMultiviewNumColumnDivisions());
                final int yGridBoxMouseLoc = (int) (yPercent * broadcastSettings.getVideoSwitcherMultiviewNumRowDivisions());
                final Point mouseClickGridBox = new Point(xGridBoxMouseLoc, yGridBoxMouseLoc);

                // find which multiview pane the user is clicking inside
                if (e.getClickCount() == 1 && broadcastSettings.getVideoSwitcherMultiviewPreviewPaneGridBoxes().stream().anyMatch(gridPoint -> gridPoint.equals(mouseClickGridBox)))
                {
                    callbacks.onFadePressed(); // pressing in the "PREVIEW" pane triggers a fade transition
                } else if (e.getClickCount() == 1 && broadcastSettings.getVideoSwitcherMultiviewProgramPaneGridBoxes().stream().anyMatch(gridPoint -> gridPoint.equals(mouseClickGridBox)))
                {
                    callbacks.onCutPressed(); // pressing in the "PROGRAM" pane triggers a cut transition
                } else
                {
                    IntStream.range(0, broadcastSettings.getVideoSwitcherMultiviewInputsGridBoxes().size()).boxed()
                            .filter(inputIdx -> broadcastSettings.getVideoSwitcherMultiviewInputsGridBoxes().get(inputIdx).equals(mouseClickGridBox))
                            .findFirst().ifPresent(gridBoxIdx ->
                            {
                                // map the index of the source grid box to the ACTUAL HDMI input and HDMI name for the switcher's input channel
                                final Map<String, Integer> switcherVideoNamesAndIndexes = broadcastSettings.getSwitcherVideoNamesAndIndexes();
                                final int switcherSourceIdx = new ArrayList<>(switcherVideoNamesAndIndexes.values()).get(gridBoxIdx);
                                final String switcherSourceName = new ArrayList<>(switcherVideoNamesAndIndexes.keySet()).get(gridBoxIdx);
                                if (e.getClickCount() == 1)
                                {
                                    logger.info("Changing to preview video input idx={} name={}", switcherSourceIdx, switcherSourceName);
                                    callbacks.onPreviewSourceChanged(switcherSourceIdx);
                                } else if (e.getClickCount() == 2)
                                {
                                    logger.info("Changing to program video input idx={} name={}", switcherSourceIdx, switcherSourceName);
                                    callbacks.onProgramSourceChanged(switcherSourceIdx);
                                }
                            });
                }
            }
        });
    }

    /**
     * We must wait until the video canvas is actually showing on the screen before playing with VLC.
     * To do this, wait on the "ancestorAdded" event from the video canvas, then configure playback.
     * When we receive the event that video playback has started, now we are able to pull the video's
     * height and width bounds. Use these to determine the overall GUI display rectangle the video occupies.
     *
     * @param videoCanvas media player panel which will house the playback video
     */
    public void initializeVlcPlayback(EmbeddedMediaPlayerComponent videoCanvas)
    {
        videoCanvas.addAncestorListener(new AncestorListener()
        {
            @Override
            public void ancestorAdded(AncestorEvent event)
            {
                // after the video canvas is showing, we can begin playback
                player.media().play(broadcastSettings.getVideoSwitcherMultiviewVlcMediaPath());

                // add a listener, so that after the media begins to play, we get the pixel boundaries of the media surface
                player.media().events().addMediaEventListener(new MediaEventAdapter()
                {
                    @Override
                    public void mediaParsedChanged(Media media, MediaParsedStatus mediaParsedStatus)
                    {
                        final Dimension videoPlaybackDimensions = player.video().videoDimension();
                        if (videoPlaybackDimensions != null)
                        {
                            // whenever the video playback canvas changes size, update the processing to find the boundaries of the video
                            videoCanvasRectangleResized(videoPlaybackDimensions, videoCanvas.getSize());
                            videoCanvas.addComponentListener(new ComponentAdapter()
                            {
                                @Override
                                public void componentResized(ComponentEvent e)
                                {
                                    super.componentResized(e);
                                    videoCanvasRectangleResized(videoPlaybackDimensions, videoCanvas.getSize());
                                }
                            });
                        }
                    }
                });
            }

            @Override
            public void ancestorRemoved(AncestorEvent event)
            {
            }

            @Override
            public void ancestorMoved(AncestorEvent event)
            {
            }
        });
    }

    /**
     * Call this whenever the parent video canvas is resized. The video canvas has a certain size (WxH), but the video
     * playing WITHIN this canvas might be (will be) scaled to fit the canvas. The playing video's aspect ratio will
     * remain constant, and at least one dimension (width or height) will be scaled to touch and perfectly fit the
     * size of the parent video canvas. So this method attempts to account for the scaling and determine the rectangle
     * of pixels WITHIN the parent video canvas where the ACTUAL VIDEO IS FOUND. It populates {@link #videoPlaybackRectangleWithinVideoCanvas}.
     * <p>
     * We might be a pixel or two off on the edges, but that's OK since we use the rectangle to register mouse events
     * and determine WHERE in the playing video we are clicking.
     *
     * @param videoPlaybackSize this is the actual size of the playing video and NEVER changes (for example, WxH 480x360 or 1920x1080 etc.)
     * @param videoCanvasSize   this is the size of the canvas playing the actual video and WILL change as the parent program is resized
     */
    private void videoCanvasRectangleResized(Dimension videoPlaybackSize, Dimension videoCanvasSize)
    {
        // aspect ratio is the width divided by the height, find for both the parent canvas and the playing video
        final double aspectRatioCanvas = videoCanvasSize.getWidth() / videoCanvasSize.getHeight();
        final double aspectRatioVideo = videoPlaybackSize.getWidth() / videoPlaybackSize.getHeight();

        // compare the aspect ratios
        final double width;
        final double height;
        if (aspectRatioCanvas > aspectRatioVideo)
        {
            // the parent canvas is WIDER than the playing video
            width = videoCanvasSize.getWidth() * aspectRatioVideo / aspectRatioCanvas;
            height = videoCanvasSize.getHeight();
        } else if (aspectRatioCanvas < aspectRatioVideo)
        {
            // the parent canvas is TALLER than the playing video
            width = videoCanvasSize.getWidth();
            height = videoCanvasSize.getHeight() * aspectRatioCanvas / aspectRatioVideo;
        } else
        {
            // the parent canvas is equivalent to the playing video
            width = videoCanvasSize.getWidth();
            height = videoCanvasSize.getHeight();
        }

        // center the newly calculated video size into a rectangle centered within the parent canvas
        final double startX = videoCanvasSize.getWidth() / 2 - width / 2;
        final double startY = videoCanvasSize.getHeight() / 2 - height / 2;
        this.videoPlaybackRectangleWithinVideoCanvas.setBounds((int) startX, (int) startY, (int) width, (int) height);
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
     * @param active       indication the fade to black is active or inactive
     * @param inTransition indicates we are fading, show the button in yellow
     */
    @Override
    public void setFadeToBlackStatus(boolean active, boolean inTransition)
    {
        this.buttonFadeToBlack.setBackground(inTransition ? Color.YELLOW : active ? Color.RED : Color.DARK_GRAY);
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
            public void onFadeToBlack()
            {
                logger.info("Fade to Black");
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
        panelVideo = new JPanel();
        JPanel panel1 = new JPanel();
        JPanel panel2 = new JPanel();
        labelTransitionInProgress = new JLabel();
        labelConnectionStatus = new JLabel();
        buttonToggleLyrics = new JButton();
        buttonFadeToBlack = new JButton();
        buttonhelp = new JButton();
        dialogHelp = new JFrame();
        JPanel panelHelpContents = new JPanel();
        JScrollPane scrollPaneHelp = new JScrollPane();
        textPaneHelp = new JTextPane();
        buttonCloseHelp = new JButton();

        //======== this ========
        setBackground(Color.black);
        setName("this");
        setLayout(new GridBagLayout());
        ((GridBagLayout)getLayout()).columnWidths = new int[] {0, 0};
        ((GridBagLayout)getLayout()).rowHeights = new int[] {0, 0};
        ((GridBagLayout)getLayout()).columnWeights = new double[] {1.0, 1.0E-4};
        ((GridBagLayout)getLayout()).rowWeights = new double[] {0.1, 1.0E-4};

        //======== panelVideo ========
        {
            panelVideo.setOpaque(false);
            panelVideo.setName("panelVideo");
            panelVideo.setLayout(new BorderLayout());

            //======== panel1 ========
            {
                panel1.setOpaque(false);
                panel1.setBorder(new EmptyBorder(1, 0, 10, 0));
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

                //---- buttonFadeToBlack ----
                buttonFadeToBlack.setText("Fade to Black");
                buttonFadeToBlack.setForeground(Color.cyan);
                buttonFadeToBlack.setBackground(Color.darkGray);
                buttonFadeToBlack.setFont(new Font("Segoe UI", Font.BOLD, 20));
                buttonFadeToBlack.setPreferredSize(new Dimension(200, 40));
                buttonFadeToBlack.setName("buttonFadeToBlack");
                panel1.add(buttonFadeToBlack, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
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
            panelVideo.add(panel1, BorderLayout.NORTH);
        }
        add(panelVideo, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));

        //======== dialogHelp ========
        {
            dialogHelp.setTitle("Broadcast Multiview Instructions");
            dialogHelp.setPreferredSize(new Dimension(500, 300));
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
    private JPanel panelVideo;
    private JLabel labelTransitionInProgress;
    private JLabel labelConnectionStatus;
    private JButton buttonToggleLyrics;
    private JButton buttonFadeToBlack;
    private JButton buttonhelp;
    private JFrame dialogHelp;
    private JTextPane textPaneHelp;
    private JButton buttonCloseHelp;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
