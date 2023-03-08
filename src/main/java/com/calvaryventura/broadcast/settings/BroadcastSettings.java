package com.calvaryventura.broadcast.settings;

import javax.swing.JOptionPane;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
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

    // settings
    private String leftCameraIp;
    private String rightCameraIp;
    private String switcherIp;
    private int lyricsOverlayVideoSourceIdx;
    private Map<String, Integer> switcherVideoNamesAndIndexes;


    /**
     * Loads the settings file or displays a fatal
     * error popup if we cannot parse the file.
     */
    public BroadcastSettings()
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

            // left camera IP
            this.leftCameraIp = lines.stream().filter(split -> split[0].trim().equalsIgnoreCase("leftCameraIp"))
                    .findFirst().orElseThrow(() -> new RuntimeException("Cannot find 'leftCameraIp' in settings file"))[1].trim();

            // right camera IP
            this.rightCameraIp = lines.stream().filter(split -> split[0].trim().equalsIgnoreCase("rightCameraIp"))
                    .findFirst().orElseThrow(() -> new RuntimeException("Cannot find 'rightCameraIp' in settings file"))[1].trim();

            // video switcher IP
            this.switcherIp = lines.stream().filter(split -> split[0].trim().equalsIgnoreCase("videoSwitcherIp"))
                    .findFirst().orElseThrow(() -> new RuntimeException("Cannot find 'videoSwitcherIp' in settings file"))[1].trim();

            // lyrics video input source index
            this.lyricsOverlayVideoSourceIdx = Integer.parseInt(lines.stream().filter(split -> split[0].trim().equalsIgnoreCase("lyricsOverlayInputSourceIndex"))
                    .findFirst().orElseThrow(() -> new RuntimeException("Cannot find 'lyricsOverlayInputSourceIndex' in settings file"))[1].trim());

            // names of the video sources
            final String[] videoSourceNames = lines.stream().filter(split -> split[0].trim().equalsIgnoreCase("videoSwitcherInputSourceNames"))
                    .findFirst().orElseThrow(() -> new RuntimeException("Cannot find 'videoSwitcherInputSourceNames' in settings file"))[1].trim().split(",");

            // source indexes of the video sources
            final String[] videoSourceIndexes = lines.stream().filter(split -> split[0].trim().equalsIgnoreCase("videoSwitcherInputSourceIndexes"))
                    .findFirst().orElseThrow(() -> new RuntimeException("Cannot find 'videoSwitcherInputSourceIndexes' in settings file"))[1].trim().split(",");

            // combine the above two entries into the map of video source name/index
            this.switcherVideoNamesAndIndexes = IntStream.range(0, videoSourceNames.length).boxed()
                    .collect(Collectors.toMap(i -> videoSourceNames[i].trim(), i -> Integer.parseInt(videoSourceIndexes[i].trim())));
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
}
