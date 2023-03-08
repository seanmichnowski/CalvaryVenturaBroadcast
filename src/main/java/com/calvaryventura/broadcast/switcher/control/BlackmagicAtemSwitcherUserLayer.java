package com.calvaryventura.broadcast.switcher.control;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;

/**
 * Top-level control for the Blackmagic ATEM video switcher.
 * Messaging documentation: <a href="https://docs.openswitcher.org/index.html">link</a>.
 * <p>
 * There is a {@link BlackmagicAtemSwitcherTransportLayer} which lives inside this class,
 * and a {@link BlackmagicAtemSwitcherNetworkLayer} which lives inside of that one.
 */
public class BlackmagicAtemSwitcherUserLayer
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private BlackmagicAtemSwitcherTransportLayer transportLayer;
    private int currentVideoPreviewIdx;
    private int currentVideoProgramIdx;
    private int currentVideoAuxIdx;
    private boolean upstreamKeyOnAir;
    private boolean fadeToBlackOn;
    private boolean fadeToBlackInProgress;
    private int transitionPosition;
    private boolean transitionInProgress;
    private int[] tallyLightsPerVideoIndexes;

    private final List<Consumer<Boolean>> switcherConnectionStatusConsumer = new ArrayList<>();
    private final List<Consumer<Integer>> programVideoSourceChangedConsumer = new ArrayList<>();
    private final List<Consumer<Integer>> previewVideoSourceChangedConsumer = new ArrayList<>();
    private final List<Consumer<Integer>> auxVideoSourceChangedConsumer = new ArrayList<>();
    private final List<Consumer<Boolean>> upstreamKeyOnAirConsumer = new ArrayList<>();
    private final List<Consumer<Boolean>> fadeToBlackActiveConsumer = new ArrayList<>();
    private final List<Consumer<Boolean>> fadeToBlackInTransitionConsumer = new ArrayList<>();
    private final List<Consumer<Boolean>> transitionInProgressConsumer = new ArrayList<>();
    private final List<Consumer<Integer>> transitionPositionConsumer = new ArrayList<>();

// TODO
//  have a callback from the transport layer for switcher connection/init status (or maybe use a CONN command mnemonic?)

    /**
     * Creates the transport layer for low-level messaging.
     * The transport layer also handles initialization.
     *
     * @param switcherIp IP address for the Blackmagic ATEM switcher
     */
    public void initialize(String switcherIp)
    {
        try
        {
            this.transportLayer = new BlackmagicAtemSwitcherTransportLayer(switcherIp,
                    this::parseSwitcherStatusFromTransportLayer);
        } catch (Exception e)
        {
            logger.error("Unable to start communication to the switcher", e);
            this.switcherConnectionStatusConsumer.forEach(s -> s.accept(false));
        }
    }

    /**
     * Fired when a new status field is available from the switcher.
     *
     * @param cmd  four letter status mnemonic
     * @param data variable length data associated with this status
     */
    private void parseSwitcherStatusFromTransportLayer(String cmd, byte[] data)
    {
        switch (cmd)
        {
            // program video selection
            case "PrgI":
                this.currentVideoProgramIdx = BlackmagicAtemSwitcherPacketUtils.word(data[2], data[3]);
                logger.info("Current program video: {}", this.currentVideoProgramIdx);
                this.programVideoSourceChangedConsumer.forEach(c -> c.accept(this.currentVideoProgramIdx));
                break;

            // preview video selection
            case "PrvI":
                this.currentVideoPreviewIdx = BlackmagicAtemSwitcherPacketUtils.word(data[2], data[3]);
                logger.info("Current preview video: {}", this.currentVideoPreviewIdx);
                this.previewVideoSourceChangedConsumer.forEach(c -> c.accept(this.currentVideoPreviewIdx));
                break;

            // aux video output index (byte 1 is the aux output, but we only have one)
            case "AuxS":
                this.currentVideoAuxIdx = BlackmagicAtemSwitcherPacketUtils.word(data[2], data[3]);
                logger.info("Aux video output: {}", this.currentVideoAuxIdx);
                break;

            // upstream key (byte 1 is the keyer index, but we only have one)
            case "KeOn":
                this.upstreamKeyOnAir = data[2] != 0;
                logger.info("Upstream key: {}", this.upstreamKeyOnAir);
                this.upstreamKeyOnAirConsumer.forEach(c -> c.accept(this.upstreamKeyOnAir));
                break;

            // fade to black (red blinking light on the panel when it's on)
            case "FtbP":
                this.fadeToBlackOn = data[1] != 0;
                this.fadeToBlackInProgress = data[2] != 0;
                logger.info("Fade to black on: {}, in progress: {}", this.fadeToBlackOn, this.fadeToBlackInProgress);
                break;

            // transition position
            case "TrPs":
                this.transitionInProgress = data[1] != 0;
                this.transitionPosition = BlackmagicAtemSwitcherPacketUtils.word(data[4], data[5]); // 0-9999
                logger.info("Transition in progress: {}, position: {}", this.transitionInProgress, this.transitionPosition);
                break;

            // tally lights (bit0=PROGRAM, bit1=PREVIEW, repeated for every tally light)
            case "TlIn":
                this.tallyLightsPerVideoIndexes = new int[BlackmagicAtemSwitcherPacketUtils.word(data[0], data[1])];
                IntStream.range(0, this.tallyLightsPerVideoIndexes.length).boxed()
                        .forEach(i -> this.tallyLightsPerVideoIndexes[i] = data[2 + i]);
                logger.info("Tally lights: {}", Arrays.toString(this.tallyLightsPerVideoIndexes));
                break;

            // tally lights given by source index
            case "TlSr":
                logger.info("Tally lights by source index....");
                break;

            // audio input configuration
            case "AMIP":
                logger.info("AUDIO input configuration.... TODO");
                break;

            // there are MANY more fields we don't process
            default:
                break;
        }
    }

    /**
     * @return index of the current preview video
     */
    public int getCurrentVideoPreviewIdx()
    {
        return this.currentVideoPreviewIdx;
    }

    /**
     * @return index of the current program video
     */
    public int getCurrentVideoProgramIdx()
    {
        return this.currentVideoProgramIdx;
    }

    /**
     * @return index of the current aux video
     */
    public int getCurrentVideoAuxIdx()
    {
        return this.currentVideoAuxIdx;
    }

    /**
     * @return upstream key is On Air
     */
    public boolean isUpstreamKeyOnAir()
    {
        return this.upstreamKeyOnAir;
    }

    /**
     * @return fade to black is active (blinking red front panel light)
     */
    public boolean isFadeToBlackOn()
    {
        return this.fadeToBlackOn;
    }

    /**
     * @return fade to black is in progress
     */
    public boolean isFadeToBlackInProgress()
    {
        return fadeToBlackInProgress;
    }

    /**
     * @param programVideoSourceChangedConsumer fired when the program video source is changed
     */
    public void addProgramVideoSourceChangedConsumer(Consumer<Integer> programVideoSourceChangedConsumer)
    {
        this.programVideoSourceChangedConsumer.add(programVideoSourceChangedConsumer);
    }

    /**
     * @param previewVideoSourceChangedConsumer fired when the preview video source is changed
     */
    public void addPreviewVideoSourceChangedConsumer(Consumer<Integer> previewVideoSourceChangedConsumer)
    {
        this.previewVideoSourceChangedConsumer.add(previewVideoSourceChangedConsumer);
    }

    /**
     * @param auxVideoSourceChangedConsumer fired when the AUX video output is changed
     */
    public void addAuxVideoSourceChangedConsumer(Consumer<Integer> auxVideoSourceChangedConsumer)
    {
        this.auxVideoSourceChangedConsumer.add(auxVideoSourceChangedConsumer);
    }

    /**
     * @param upstreamKeyOnAirConsumer fired when the upstream key is on or off air
     */
    public void addUpstreamKeyOnAirConsumer(Consumer<Boolean> upstreamKeyOnAirConsumer)
    {
        this.upstreamKeyOnAirConsumer.add(upstreamKeyOnAirConsumer);
    }

    /**
     * @param fadeToBlackActiveConsumer fired when the fade to black status changes
     */
    public void addFadeToBlackActiveConsumer(Consumer<Boolean> fadeToBlackActiveConsumer)
    {
        this.fadeToBlackActiveConsumer.add(fadeToBlackActiveConsumer);
    }

    /**
     * @param fadeToBlackInTransitionConsumer fired when the fade to black is in transition
     */
    public void addFadeToBlackInTransitionConsumer(Consumer<Boolean> fadeToBlackInTransitionConsumer)
    {
        this.fadeToBlackInTransitionConsumer.add(fadeToBlackInTransitionConsumer);
    }

    /**
     * @param transitionInProgressConsumer fired when the transition is in progress
     */
    public void addTransitionInProgressConsumer(Consumer<Boolean> transitionInProgressConsumer)
    {
        this.transitionInProgressConsumer.add(transitionInProgressConsumer);
    }

    /**
     * @param transitionPositionConsumer fired when the transition position changes (range 0-9999)
     */
    public void addTransitionPositionConsumer(Consumer<Integer> transitionPositionConsumer)
    {
        this.transitionPositionConsumer.add(transitionPositionConsumer);
    }

    /**
     * @param videoSourceIdx index of the video source to set as program
     *
     * @return successful command execution
     */
    public boolean setProgramVideo(int videoSourceIdx)
    {
        final byte high = (byte) ((videoSourceIdx >> 8) & 0xFF);
        final byte low = (byte) (videoSourceIdx & 0xFF);
        return this.transportLayer.sendCommand("CPgI", new byte[]{0, 0, high, low});
    }

    /**
     * @param videoSourceIdx index of the video source to set as preview
     *
     * @return successful command execution
     */
    public boolean setPreviewVideo(int videoSourceIdx)
    {
        final byte high = (byte) ((videoSourceIdx >> 8) & 0xFF);
        final byte low = (byte) (videoSourceIdx & 0xFF);
        return this.transportLayer.sendCommand("CPvI", new byte[]{0, 0, high, low});
    }

    /**
     * Equivalent to the user pressing the CUT button on the front panel.
     *
     * @return successful command execution
     */
    public boolean performCut()
    {
        return this.transportLayer.sendCommand("DCut", new byte[4]);
    }

    /**
     * Equivalent to the user pressing the AUTO button on the front panel.
     *
     * @return successful command execution
     */
    public boolean performAuto()
    {
        return this.transportLayer.sendCommand("DAut", new byte[4]);
    }

    /**
     * @return a transition is currently in progress
     */
    public boolean getTransitionInProgress()
    {
        return this.transitionInProgress;
    }

    /**
     * @return position of the transition (T-slider) bar, range 0-9999
     */
    public int getTransitionPosition()
    {
        return this.transitionPosition;
    }

    /**
     * @param position sets the transition (T-slider), range 0-9999
     *
     * @return successful command execution
     */
    public boolean setTransitionPosition(int position)
    {
        final byte high = (byte) ((position >> 8) & 0xFF);
        final byte low = (byte) (position & 0xFF);
        return this.transportLayer.sendCommand("CTPs", new byte[]{0, 0, high, low});
    }

    /**
     * Byte 1 is the keyer index, but we only have one.
     *
     * @param enabled sets the upstream keyer On Air
     *
     * @return successful command execution
     */
    public boolean setKeyerOnAirEnabled(boolean enabled)
    {
        return this.transportLayer.sendCommand("CKOn", new byte[]{0, 0, (byte) (enabled ? 1 : 0), 0});
    }

    /**
     * Triggers the fade to black transition.
     *
     * @return successful command execution
     */
    public boolean performFadeToBlack()
    {
        return this.transportLayer.sendCommand("FtbA", new byte[4]);
    }

    /**
     * @param input index of the video input, see video source list
     *
     * @return successful command execution
     */
    public boolean setAuxSourceInput(int input)
    {
        final byte high = (byte) ((input >> 8) & 0xFF);
        final byte low = (byte) (input & 0xFF);
        return this.transportLayer.sendCommand("CAuS", new byte[]{1, 0, high, low});
    }




    // TODO not sure which way this is...
    public int getVideoSrcIndex(int videoSrc)
    {
        switch (videoSrc)
        {
            case 0:  // Black
                return 0;
            case 1:  // Input 1
                return 1;
            case 2:  // Input 2
                return 2;
            case 3:  // Input 3
                return 3;
            case 4:  // Input 4
                return 4;
            case 5:  // Input 5
                return 5;
            case 6:  // Input 6
                return 6;
            case 7:  // Input 7
                return 7;
            case 8:  // Input 8
                return 8;
            case 9:  // Input 9
                return 9;
            case 10:  // Input 10
                return 10;
            case 11:  // Input 11
                return 11;
            case 12:  // Input 12
                return 12;
            case 13:  // Input 13
                return 13;
            case 14:  // Input 14
                return 14;
            case 15:  // Input 15
                return 15;
            case 16:  // Input 16
                return 16;
            case 17:  // Input 17
                return 17;
            case 18:  // Input 18
                return 18;
            case 19:  // Input 19
                return 19;
            case 20:  // Input 20
                return 20;
            case 1000:  // Color Bars
                return 21;
            case 2001:  // Color 1
                return 22;
            case 2002:  // Color 2
                return 23;
            case 3010:  // Media Player 1
                return 24;
            case 3011:  // Media Player 1 Key
                return 25;
            case 3020:  // Media Player 2
                return 26;
            case 3021:  // Media Player 2 Key
                return 27;
            case 4010:  // Key 1 Mask
                return 28;
            case 4020:  // Key 2 Mask
                return 29;
            case 4030:  // Key 3 Mask
                return 30;
            case 4040:  // Key 4 Mask
                return 31;
            case 5010:  // DSK 1 Mask
                return 32;
            case 5020:  // DSK 2 Mask
                return 33;
            case 6000:  // Super Source
                return 34;
            case 7001:  // Clean Feed 1
                return 35;
            case 7002:  // Clean Feed 2
                return 36;
            case 8001:  // Auxilary 1
                return 37;
            case 8002:  // Auxilary 2
                return 38;
            case 8003:  // Auxilary 3
                return 39;
            case 8004:  // Auxilary 4
                return 40;
            case 8005:  // Auxilary 5
                return 41;
            case 8006:  // Auxilary 6
                return 42;
            case 10010:  // ME 1 Prog
                return 43;
            case 10011:  // ME 1 Prev
                return 44;
            case 10020:  // ME 2 Prog
                return 45;
            case 10021:  // ME 2 Prev
                return 46;
            default:
                return 0;
        }
    }
}
