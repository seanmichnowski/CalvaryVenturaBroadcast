package com.calvaryventura.broadcast.settings;

import javax.swing.JOptionPane;
import java.awt.Dimension;
import java.awt.Point;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Parses the settings text file within the JAR and loads contents into this structure.
 */
public class BroadcastSettings
{
    // location of the settings text file within the JAR (in src/main/resources)
    private static final String UCAT_SETTINGS_JAR_RESOURCE = "/settings/broadcast_settings.txt";

    // handle so that we can get a static reference to this class from anywhere
    private static final BroadcastSettings INSTANCE = new BroadcastSettings();

    // settings
    private String programTitle;
    private String leftCameraIp;
    private String rightCameraIp;
    private String switcherIp;
    private List<String> defaultPresetNames;
    private int lyricsOverlayVideoSourceIdx;
    private Map<String, Integer> switcherVideoNamesAndIndexes;
    private boolean videoSwitcherMultiviewEnabled;
    private int videoSwitcherMultiviewNumRowDivisions;
    private int videoSwitcherMultiviewNumColumnDivisions;
    private List<Point> videoSwitcherMultiviewPreviewPaneGridBoxes;
    private List<Point> videoSwitcherMultiviewProgramPaneGridBoxes;
    private List<Point> videoSwitcherMultiviewInputsGridBoxes;
    private String videoSwitcherMultiviewVlcMediaPath;
    private Dimension videoSwitcherMultiviewVideoSize;
    private int minAudioLevelDb;
    private int warnAudioLevelDb;
    private int highAudioLevelDb;
    private int maxAudioLevelDb;

    /**
     * Global static method for getting the broadcast settings from anywhere.
     *
     * @return handle to the broadcast settings
     */
    public static BroadcastSettings getInst()
    {
        return INSTANCE;
    }

    /**
     * Loads the settings file or displays a fatal error popup if we cannot parse the file.
     * Private method which can only be instantiated locally, as we create the static handle {@link #INSTANCE}.
     */
    private BroadcastSettings()
    {
        try
        {
            // open the JAR's settings file
            final BufferedReader in = new BufferedReader(new InputStreamReader(
                    Objects.requireNonNull(this.getClass().getResourceAsStream(UCAT_SETTINGS_JAR_RESOURCE))));

            // read all file lines into a split based on '=' sign
            String line;
            final List<String[]> lines = new ArrayList<>();
            while ((line = in.readLine()) != null)
            {
                if (!line.startsWith("#"))
                {
                    lines.add(line.trim().split("="));
                }
            }
            in.close();

            // program title
            this.programTitle = lines.stream().filter(split -> split[0].trim().equalsIgnoreCase("programTitle"))
                    .findFirst().orElseThrow(() -> new RuntimeException("Cannot find 'programTitle' in settings file"))[1].trim();

            // left camera IP
            this.leftCameraIp = lines.stream().filter(split -> split[0].trim().equalsIgnoreCase("leftCameraIp"))
                    .findFirst().orElseThrow(() -> new RuntimeException("Cannot find 'leftCameraIp' in settings file"))[1].trim();

            // right camera IP
            this.rightCameraIp = lines.stream().filter(split -> split[0].trim().equalsIgnoreCase("rightCameraIp"))
                    .findFirst().orElseThrow(() -> new RuntimeException("Cannot find 'rightCameraIp' in settings file"))[1].trim();

            // video switcher IP
            this.switcherIp = lines.stream().filter(split -> split[0].trim().equalsIgnoreCase("videoSwitcherIp"))
                    .findFirst().orElseThrow(() -> new RuntimeException("Cannot find 'videoSwitcherIp' in settings file"))[1].trim();

            // default preset names
            this.defaultPresetNames = Arrays.stream(lines.stream().filter(split -> split[0].trim().equalsIgnoreCase("defaultPresetNames"))
                    .findFirst().orElseThrow(() -> new RuntimeException("Cannot find 'videoSwitcherIp' in settings file"))[1].trim().split(","))
                    .map(String::trim).collect(Collectors.toList());

            // lyrics video input source index
            this.lyricsOverlayVideoSourceIdx = Integer.parseInt(lines.stream().filter(split -> split[0].trim().equalsIgnoreCase("videoSwitcherLyricsOverlayInputSourceIndex"))
                    .findFirst().orElseThrow(() -> new RuntimeException("Cannot find 'videoSwitcherLyricsOverlayInputSourceIndex' in settings file"))[1].trim());

            // names of the video sources
            final String[] videoSourceNames = lines.stream().filter(split -> split[0].trim().equalsIgnoreCase("videoSwitcherInputSourceNames"))
                    .findFirst().orElseThrow(() -> new RuntimeException("Cannot find 'videoSwitcherInputSourceNames' in settings file"))[1].trim().split(",");

            // source indexes of the video sources
            final String[] videoSourceIndexes = lines.stream().filter(split -> split[0].trim().equalsIgnoreCase("videoSwitcherInputSourceIndexes"))
                    .findFirst().orElseThrow(() -> new RuntimeException("Cannot find 'videoSwitcherInputSourceIndexes' in settings file"))[1].trim().split(",");

            // combine the above two entries into the map of video source name/index
            this.switcherVideoNamesAndIndexes = IntStream.range(0, videoSourceNames.length).boxed()
                    .collect(Collectors.toMap(i -> videoSourceNames[i].trim(), i -> Integer.parseInt(videoSourceIndexes[i].trim())));

            // multiview display
            this.videoSwitcherMultiviewEnabled = lines.stream().filter(split -> split[0].trim().equalsIgnoreCase("videoSwitcherMultiviewEnabled"))
                    .findFirst().orElseThrow(() -> new RuntimeException("Cannot find multiview parameter in settings file"))[1].trim().equalsIgnoreCase("TRUE");

            // multiview display
            this.videoSwitcherMultiviewNumRowDivisions = Integer.parseInt(lines.stream().filter(split -> split[0].trim().equalsIgnoreCase("videoSwitcherMultiviewNumRowDivisions"))
                    .findFirst().orElseThrow(() -> new RuntimeException("Cannot find multiview parameter in settings file"))[1].trim());

            // multiview display
            this.videoSwitcherMultiviewNumColumnDivisions = Integer.parseInt(lines.stream().filter(split -> split[0].trim().equalsIgnoreCase("videoSwitcherMultiviewNumColumnDivisions"))
                    .findFirst().orElseThrow(() -> new RuntimeException("Cannot find multiview parameter in settings file"))[1].trim());

            // multiview display
            final String videoSwitcherMultiviewPreviewPaneGridBoxesStr = lines.stream().filter(split -> split[0].trim().equalsIgnoreCase("videoSwitcherMultiviewPreviewPaneGridBoxes"))
                    .findFirst().orElseThrow(() -> new RuntimeException("Cannot find multiview parameter in settings file"))[1].trim();
            this.videoSwitcherMultiviewPreviewPaneGridBoxes = Arrays.stream(videoSwitcherMultiviewPreviewPaneGridBoxesStr.split(","))
                    .map(gridBoxXYStr -> gridBoxXYStr.split("/"))
                    .map(gridBoxXY -> new Point(Integer.parseInt(gridBoxXY[0].trim()), Integer.parseInt(gridBoxXY[1].trim())))
                    .collect(Collectors.toList());

            // multiview display
            final String videoSwitcherMultiviewProgramPaneGridBoxesStr = lines.stream().filter(split -> split[0].trim().equalsIgnoreCase("videoSwitcherMultiviewProgramPaneGridBoxes"))
                    .findFirst().orElseThrow(() -> new RuntimeException("Cannot find multiview parameter in settings file"))[1].trim();
            this.videoSwitcherMultiviewProgramPaneGridBoxes = Arrays.stream(videoSwitcherMultiviewProgramPaneGridBoxesStr.split(","))
                    .map(gridBoxXYStr -> gridBoxXYStr.split("/"))
                    .map(gridBoxXY -> new Point(Integer.parseInt(gridBoxXY[0].trim()), Integer.parseInt(gridBoxXY[1].trim())))
                    .collect(Collectors.toList());

            // multiview display
            final String videoSwitcherMultiviewInputsGridBoxesStr = lines.stream().filter(split -> split[0].trim().equalsIgnoreCase("videoSwitcherMultiviewInputsGridBoxes"))
                    .findFirst().orElseThrow(() -> new RuntimeException("Cannot find multiview parameter in settings file"))[1].trim();
            this.videoSwitcherMultiviewInputsGridBoxes = Arrays.stream(videoSwitcherMultiviewInputsGridBoxesStr.split(","))
                    .map(gridBoxXYStr -> gridBoxXYStr.split("/"))
                    .map(gridBoxXY -> new Point(Integer.parseInt(gridBoxXY[0].trim()), Integer.parseInt(gridBoxXY[1].trim())))
                    .collect(Collectors.toList());

            // multiview display
            this.videoSwitcherMultiviewVlcMediaPath = lines.stream().filter(split -> split[0].trim().equalsIgnoreCase("videoSwitcherMultiviewVlcMediaPath"))
                    .findFirst().orElseThrow(() -> new RuntimeException("Cannot find multiview parameter in settings file"))[1].trim();

            // size of the streaming multiview video (width X height)
            final int multiviewVideoWidth = !this.videoSwitcherMultiviewEnabled ? 0 : Integer.parseInt(lines.stream().filter(split -> split[0].trim().equalsIgnoreCase("videoSwitcherMultiviewVideoWidth"))
                    .findFirst().orElseThrow(() -> new RuntimeException("Cannot find multivew streaming video width in settings file"))[1].trim());
            final int multiviewVideoHeight = !this.videoSwitcherMultiviewEnabled ? 0 : Integer.parseInt(lines.stream().filter(split -> split[0].trim().equalsIgnoreCase("videoSwitcherMultiviewVideoHeight"))
                    .findFirst().orElseThrow(() -> new RuntimeException("Cannot find multivew streaming video height in settings file"))[1].trim());
            this.videoSwitcherMultiviewVideoSize = new Dimension(multiviewVideoWidth, multiviewVideoHeight);

            // audio
            this.minAudioLevelDb = Integer.parseInt(lines.stream().filter(split -> split[0].trim().equalsIgnoreCase("minAudioLevelDb"))
                    .findFirst().orElseThrow(() -> new RuntimeException("Cannot find minimum audio level in settings file"))[1].trim());
            this.warnAudioLevelDb = Integer.parseInt(lines.stream().filter(split -> split[0].trim().equalsIgnoreCase("warnAudioLevelDb"))
                    .findFirst().orElseThrow(() -> new RuntimeException("Cannot find warning audio level in settings file"))[1].trim());
            this.highAudioLevelDb = Integer.parseInt(lines.stream().filter(split -> split[0].trim().equalsIgnoreCase("highAudioLevelDb"))
                    .findFirst().orElseThrow(() -> new RuntimeException("Cannot find high audio level in settings file"))[1].trim());
            this.maxAudioLevelDb = Integer.parseInt(lines.stream().filter(split -> split[0].trim().equalsIgnoreCase("maxAudioLevelDb"))
                    .findFirst().orElseThrow(() -> new RuntimeException("Cannot find maximum audio level in settings file"))[1].trim());
        } catch (Exception e)
        {
            // don't proceed since we can't load settings
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, e.getMessage(),
                    "Unable to load settings file from JAR", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    /**
     * @return name of the program to show in the parent frame
     */
    public String getProgramTitle()
    {
        return programTitle;
    }

    /**
     * @return IP address of the left camera
     */
    public String getLeftCameraIp()
    {
        return this.leftCameraIp;
    }

    /**
     * @return IP address of the right camera
     */
    public String getRightCameraIp()
    {
        return this.rightCameraIp;
    }

    /**
     * @return IP address of the blackmagic video switcher
     */
    public String getSwitcherIp()
    {
        return this.switcherIp;
    }

    /**
     * @return preset names to appear when making a camera preset
     */
    public List<String> getDefaultPresetNames()
    {
        return defaultPresetNames;
    }

    /**
     * @return video source index of the lyrics video input to the switcher
     */
    public int getLyricsOverlayVideoSourceIdx()
    {
        return this.lyricsOverlayVideoSourceIdx;
    }

    /**
     * @return map of [videoName, videoSourceIndex] for all inputs to the switcher
     */
    public Map<String, Integer> getSwitcherVideoNamesAndIndexes()
    {
        return this.switcherVideoNamesAndIndexes;
    }

    // one day remove calling out specifically left/right cameras, see the broadcast_settings.txt file for a comment there
    public int getLeftCameraVideoIndex()
    {
        return this.switcherVideoNamesAndIndexes.get("LEFT");
    }

    // one day remove calling out specifically left/right cameras, see the broadcast_settings.txt file for a comment there
    public int getRightCameraVideoIndex()
    {
        return this.switcherVideoNamesAndIndexes.get("RIGHT");
    }

    public boolean isVideoSwitcherMultiviewEnabled()
    {
        return videoSwitcherMultiviewEnabled;
    }

    public int getVideoSwitcherMultiviewNumRowDivisions()
    {
        return videoSwitcherMultiviewNumRowDivisions;
    }

    public int getVideoSwitcherMultiviewNumColumnDivisions()
    {
        return videoSwitcherMultiviewNumColumnDivisions;
    }

    public List<Point> getVideoSwitcherMultiviewPreviewPaneGridBoxes()
    {
        return videoSwitcherMultiviewPreviewPaneGridBoxes;
    }

    public List<Point> getVideoSwitcherMultiviewProgramPaneGridBoxes()
    {
        return videoSwitcherMultiviewProgramPaneGridBoxes;
    }

    public List<Point> getVideoSwitcherMultiviewInputsGridBoxes()
    {
        return videoSwitcherMultiviewInputsGridBoxes;
    }

    public String getVideoSwitcherMultiviewVlcMediaPath()
    {
        return videoSwitcherMultiviewVlcMediaPath;
    }

    public Dimension getVideoSwitcherMultiviewVideoSize()
    {
        return videoSwitcherMultiviewVideoSize;
    }

    public int getMinAudioLevelDb()
    {
        return minAudioLevelDb;
    }

    public int getWarnAudioLevelDb()
    {
        return warnAudioLevelDb;
    }

    public int getHighAudioLevelDb()
    {
        return highAudioLevelDb;
    }

    public int getMaxAudioLevelDb()
    {
        return maxAudioLevelDb;
    }
}
