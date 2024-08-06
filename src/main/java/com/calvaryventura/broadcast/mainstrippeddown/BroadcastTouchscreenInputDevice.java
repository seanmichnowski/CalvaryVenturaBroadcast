package com.calvaryventura.broadcast.mainstrippeddown;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * TODO
 */
public class BroadcastTouchscreenInputDevice
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String EVTEST_INSTALLATION_NAME = "/usr/bin/evtest";
    private static final String EVTEST_EVENT_TOUCHSCREEN = "touch";
    private static final String EVTEST_DEVICE_LIST_BEGINNING = "/dev/input/event";
    private static final String EVTEST_TOUCH_PRESS_FRAGMENT = "(BTN_TOUCH), value 1";
    private static final String EVTEST_TOUCH_RELEASE_FRAGMENT = "(BTN_TOUCH), value 0";
    private static final String EVTEST_TOUCH_POSITION_X_FRAGMENT = "(ABS_X)";
    private static final String EVTEST_TOUCH_POSITION_Y_FRAGMENT = "(ABS_Y)";

    /**
     * Main entry point for this utility. Creates a background thread which internally
     * reads the 'evtest' utility and fires the consumer when the user interacts with the touchscreen.
     *
     * @param xyPercentOfTouch fired when the user interacts with the touchscreen input device
     */
    public void initializeTouchscreenCallbacks(BiConsumer<Double, Double> xyPercentOfTouch)
    {
        // run a background thread for reading the 'evtest' utility
        Executors.newSingleThreadExecutor().submit(() -> {
            try
            {
                // verify 'evtest' is installed
                verifyEvtestInstallationOnHostComputer();

                // get the name of the linux input event ID corresponding to the touchscreen
                final List<String> touchscreenEventIds = getEvtestEventIdsForAllTouchscreens();

                // use dmesg to find all touchscreens in the system, and return the first physical-location-USB one
                final String multiviewTouchscreenId = determineWhichTouchscreenIdToUse(touchscreenEventIds);

                // create a background loop which opens the multiview touchscreen's events and fires callbacks on touches
                doEvtestReadLoop(multiviewTouchscreenId, true, xyPercentOfTouch);
            } catch (Exception e)
            {
                logger.error("Cannot use the 'evtest' utility to read touchscreen input device: {}", e.getMessage());
            }
        });
    }

    /**
     * Blocking invocation of 'evtest' which continually reads the raw data from the specified input device.
     * Initial output describing the maximum X and Y ranges:
     * Event type 3 (EV_ABS)
     * Event code 0 (ABS_X)
     * Value     48
     * Min        0
     * Max     1199
     * Resolution      12
     * Event code 1 (ABS_Y)
     * Value    425
     * Min        0
     * Max      635
     * Resolution      12
     *
     * @param inputDeviceId    ID of the touchscreen as found from the previous operation
     * @param doGrab
     * @param xyPercentOfTouch fired when we get touches
     * @throws Exception if we can't run 'evtest' or parse its output
     */
    private static void doEvtestReadLoop(String inputDeviceId, boolean doGrab, BiConsumer<Double, Double> xyPercentOfTouch) throws Exception
    {
        // use the 'grab' argument to capture all input events from the touchscreen and NOT let them escape to become erroneous mouse pointer events
        final String command = EVTEST_INSTALLATION_NAME + (doGrab ? " --grab " : " ") + inputDeviceId;
        logger.info("Using touchscreen id: '{}', command: '{}'", inputDeviceId, command);
        final Process process = Runtime.getRuntime().exec(command);
        final BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));

        // let the display itself tell us its max and min X/Y values
        boolean xEntryToFollow = false;
        boolean yEntryToFollow = false;
        int touchscreenMaxX = -1;
        int touchscreenMaxY = -1;
        String line;
        while ((line = in.readLine()) != null)
        {
            if (line.contains(EVTEST_TOUCH_POSITION_X_FRAGMENT))
            {
                xEntryToFollow = true;
                continue;
            } else if (xEntryToFollow && line.contains("Max"))
            {
                touchscreenMaxX = Integer.parseInt(line.substring(line.lastIndexOf(" ") + 1));
                xEntryToFollow = false;
            } else if (line.contains(EVTEST_TOUCH_POSITION_Y_FRAGMENT))
            {
                yEntryToFollow = true;
                continue;
            } else if (yEntryToFollow && line.contains("Max"))
            {
                touchscreenMaxY = Integer.parseInt(line.substring(line.lastIndexOf(" ") + 1));
                yEntryToFollow = false;
            }

            if (touchscreenMaxX != -1 && touchscreenMaxY != -1)
            {
                logger.info("Found touchscreen's maximum X={} Y={}", touchscreenMaxX, touchscreenMaxY);
                break;
            }
        }

        // continually wait for new touchscreen presses
        while (!Thread.currentThread().isInterrupted())
        {
            // reset touchscreen values
            boolean foundTouchPress = false;
            double absoluteXPressLocation = -1;  // referenced to the upper-left corner
            double absoluteYPressLocation = -1;  // referenced to the upper-left corner
            boolean foundTouchRelease = false;

            // traverse line-by-line looking for touch parameters
            while (!Thread.currentThread().isInterrupted() && (line = in.readLine()) != null)
            {
                if (line.endsWith(EVTEST_TOUCH_PRESS_FRAGMENT))
                {
                    foundTouchPress = true;
                    continue;
                } else if (line.contains(EVTEST_TOUCH_POSITION_X_FRAGMENT))
                {
                    absoluteXPressLocation = Integer.parseInt(line.substring(line.lastIndexOf(" ") + 1));
                    continue;
                } else if (line.contains(EVTEST_TOUCH_POSITION_Y_FRAGMENT))
                {
                    absoluteYPressLocation = Integer.parseInt(line.substring(line.lastIndexOf(" ") + 1));
                    continue;
                } else if (line.endsWith(EVTEST_TOUCH_RELEASE_FRAGMENT))
                {
                    foundTouchRelease = true;
                }

                // getting here means we have a touch event!
                // send the callback and break out of while loop to reset looking for a new touch
                if (foundTouchPress && foundTouchRelease)
                {
                    xyPercentOfTouch.accept(absoluteXPressLocation / touchscreenMaxX, absoluteYPressLocation / touchscreenMaxY);
                    break;
                }
            }
        }
    }

    /**
     * Example output from running 'evtest:'
     * No device specified, trying to scan all of /dev/input/event*
     * Available devices:
     * /dev/input/event0:	Lid Switch
     * /dev/input/event1:	Power Button
     * /dev/input/event2:	Sleep Button
     * /dev/input/event3:	Power Button
     * /dev/input/event4:	AT Translated Set 2 keyboard
     * /dev/input/event5:	Video Bus
     * /dev/input/event6:	Video Bus
     * /dev/input/event7:	PixArt USB Optical Mouse
     * /dev/input/event12:	Intel HID events
     * /dev/input/event13:	Intel HID 5 button array
     * /dev/input/event14:	Dell WMI hotkeys
     * /dev/input/event15:	Integrated_Webcam_HD: Integrate
     * /dev/input/event16:	HDA Intel PCH Headphone Mic
     * /dev/input/event17:	HDA Intel PCH HDMI/DP,pcm=3
     * /dev/input/event18:	HDA Intel PCH HDMI/DP,pcm=7
     * /dev/input/event19:	HDA Intel PCH HDMI/DP,pcm=8
     * /dev/input/event20:	HDA Intel PCH HDMI/DP,pcm=9
     * /dev/input/event21:	HDA Intel PCH HDMI/DP,pcm=10
     * /dev/input/event22:	Integrated_Webcam_HD: Integrate
     * /dev/input/event23:	HDA NVidia HDMI/DP,pcm=3
     * /dev/input/event24:	HDA NVidia HDMI/DP,pcm=7
     * /dev/input/event25:	HDA NVidia HDMI/DP,pcm=8
     * /dev/input/event26:	HDA NVidia HDMI/DP,pcm=9
     * /dev/input/event27:	HDA NVidia HDMI/DP,pcm=10
     * /dev/input/event28:	DELL0927:00 044E:1220 Mouse
     * /dev/input/event29:	DELL0927:00 044E:1220 Touchpad
     * /dev/input/event30:	DELL0927:00 044E:1220 UNKNOWN
     * Select the device event number [0-30]:
     *
     * @return ID's of all touchscreens found in the system
     * @throws Exception if we can't run 'evtest' or parse its output
     */
    private static List<String> getEvtestEventIdsForAllTouchscreens() throws Exception
    {
        // get the ffmpeg version by invoking the program
        final Process process = Runtime.getRuntime().exec(EVTEST_INSTALLATION_NAME);

        // send an invalid choice into the 'evtest' utility so that it exits
        try (final BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream())))
        {
            bufferedWriter.write("-1\n");
            bufferedWriter.flush();
        }

        // since we are not initially sending 'evtest' any arguments, it prints everything out on stderr
        try (final BufferedReader in = new BufferedReader(new InputStreamReader(process.getErrorStream())))
        {
            final List<String> inputDevices = in.lines().filter(l -> l.startsWith(EVTEST_DEVICE_LIST_BEGINNING))
                    .filter(l -> l.toLowerCase().contains(EVTEST_EVENT_TOUCHSCREEN)).collect(Collectors.toList());
            logger.info("Discovered touch input devices: {}", Arrays.toString(inputDevices.toArray()));
            final List<String> inputEventIdsForTouchscreens = inputDevices.stream()
                    .map(deviceEntry -> deviceEntry.split(":")[0].trim())
                    .collect(Collectors.toList());
            logger.info("Using event input: {}", Arrays.toString(inputEventIdsForTouchscreens.toArray()));
            return inputEventIdsForTouchscreens;
        }
    }

    /**
     * Checks the current 'evtest' installation and verifies we can read the version.
     *
     * @throws Exception if the 'evetst' program is not installed or we can't run it
     */
    private static void verifyEvtestInstallationOnHostComputer() throws Exception
    {
        // get the ffmpeg version by invoking the program
        final Process process = Runtime.getRuntime().exec(EVTEST_INSTALLATION_NAME + " --version");
        try (final BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream())))
        {
            final String versionStr = in.lines().findFirst().orElseThrow(() -> new RuntimeException("The program evtest doesn't seem to be installed"));
            logger.info("Installed 'evtest' version: '{}'", versionStr);
        }
    }

    /**
     * Out of all touchscreens in the system, there doesn't seem to be an elegant way of
     * determining which one is the actual multiview screen.
     *
     * @param touchscreenIds ID's from {@link #getEvtestEventIdsForAllTouchscreens()} corresponding to all touchscreens in the system
     * @return ID of the touchscreen corresponding to the actual broadcast switcher's multiview screen in the system
     */
    private static String determineWhichTouchscreenIdToUse(List<String> touchscreenIds) throws Exception
    {
        // find all touch screen USB devices in the system
        final AtomicInteger idxOf0thTouchscreen = new AtomicInteger();
        final Process process = Runtime.getRuntime().exec("dmesg | grep usb | grep touch");
        try (final BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream())))
        {
            // pull all lines from the dmesg process
            final List<String> lines = in.lines().collect(Collectors.toList());
            logger.info("DMESG lines when searching for touchscreen: {}", Arrays.toString(lines.toArray()));

            // find the index of the dmesg USB touchscreen listing containing the string we EXPECT to find our multiview on (first port)
            IntStream.range(0, lines.size()).boxed().filter(idx -> lines.get(idx).contains("0-1")).findFirst().ifPresent(idxOf0thTouchscreen::set);
        }
        return touchscreenIds.get(idxOf0thTouchscreen.get());
    }
}
