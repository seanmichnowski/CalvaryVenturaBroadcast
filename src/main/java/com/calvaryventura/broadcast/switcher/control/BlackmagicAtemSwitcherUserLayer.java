package com.calvaryventura.broadcast.switcher.control;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
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
    private boolean isAudioMasterMuted;

    private final List<Consumer<Boolean>> switcherConnectionStatusConsumer = new ArrayList<>();
    private final List<Consumer<Integer>> programVideoSourceChangedConsumer = new ArrayList<>();
    private final List<Consumer<Integer>> previewVideoSourceChangedConsumer = new ArrayList<>();
    private final List<Consumer<Integer>> auxVideoSourceChangedConsumer = new ArrayList<>();
    private final List<Consumer<Boolean>> upstreamKeyOnAirConsumer = new ArrayList<>();
    private final List<BiConsumer<Boolean, Boolean>> fadeToBlackActiveAndTransitionConsumer = new ArrayList<>();
    private final List<Consumer<Boolean>> audioMuteStatusConsumer = new ArrayList<>();
    private final List<Consumer<Boolean>> transitionInProgressConsumer = new ArrayList<>();
    private final List<Consumer<Integer>> transitionPositionConsumer = new ArrayList<>();

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
    logger.info("Received CMD='{}'", cmd); // TODO
        switch (cmd)
        {
            // video switcher connection status
            case "CONN":
                this.switcherConnectionStatusConsumer.forEach(c -> c.accept(data[0] == 1));
                break;

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
                this.auxVideoSourceChangedConsumer.forEach(c -> c.accept(this.currentVideoAuxIdx));
                logger.info("Aux video output: {}", this.currentVideoAuxIdx);
                break;

            // upstream key (byte 1 is the keyer index, but we only have one)
            case "KeOn":
                this.upstreamKeyOnAir = data[2] != 0;
                logger.info("Upstream key: {}", this.upstreamKeyOnAir);
                this.upstreamKeyOnAirConsumer.forEach(c -> c.accept(this.upstreamKeyOnAir));
                break;

            // fade to black (red blinking light on the panel when it's on)
            case "FtbS":
                this.fadeToBlackOn = data[1] != 0;
                this.fadeToBlackInProgress = data[2] != 0;
                this.fadeToBlackActiveAndTransitionConsumer.forEach(c -> c.accept(this.fadeToBlackOn, this.fadeToBlackInProgress));
                break;

            // transition position
            case "TrPs":
                this.transitionInProgress = data[1] != 0;
                this.transitionPosition = BlackmagicAtemSwitcherPacketUtils.word(data[4], data[5]); // 0-9999
                logger.info("Transition in progress: {}, position: {}", this.transitionInProgress, this.transitionPosition);
                this.transitionPositionConsumer.forEach(c -> c.accept(this.transitionPosition));
                this.transitionInProgressConsumer.forEach(c -> c.accept(this.transitionInProgress));
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
                ByteBuffer bb = ByteBuffer.wrap(data);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                final int audioWordRaw = bb.getInt(10);
                logger.info("Master audio volume set to {}dB", (float) audioWordRaw / 100); // TODO
                break;

            // audio master volume
            // TODO I never receive this!!!!
            case "CAMM":
                final float audioVolumePercent = (float) BlackmagicAtemSwitcherPacketUtils.word(data[2], data[3]) / 65381 * 100;
                logger.info("Audio master volume set to {}%", audioVolumePercent);
                this.isAudioMasterMuted = audioVolumePercent == 0;
                this.audioMuteStatusConsumer.forEach(c -> c.accept(isAudioMasterMuted));
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
     * @param connectionStatusConsumer fired when the switcher connects or disconnects
     */
    public void addConnectionStatusConsumer(Consumer<Boolean> connectionStatusConsumer)
    {
        this.switcherConnectionStatusConsumer.add(connectionStatusConsumer);
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
     * @param fadeToBlackConsumer fired when the fade to black status changes
     */
    public void addFadeToBlackActiveAndTransitionConsumer(BiConsumer<Boolean, Boolean> fadeToBlackConsumer)
    {
        this.fadeToBlackActiveAndTransitionConsumer.add(fadeToBlackConsumer);
    }

    /**
     * @param muteConsumer fired when the mute status changes
     */
    public void addAudioMuteStatusConsumer(Consumer<Boolean> muteConsumer)
    {
        this.audioMuteStatusConsumer.add(muteConsumer);
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
     * Toggles the audio mute (master channel) on the ATEM switcher.
     * See <a href="https://docs.openswitcher.org/commands/audiomixer.html">link</a>.
     *
     * @return successful command execution
     */
    public boolean toggleAudioMute()
    {
        final boolean muted = !this.isAudioMasterMuted; // set the opposite command to what we had previously
        logger.info("Setting audio muted: {}", muted);
        final int audioLevel = muted ? 0 : 65381; // max audio volume
        final byte high = (byte) ((audioLevel >> 8) & 0xFF);
        final byte low = (byte) (audioLevel & 0xFF);
        //return this.transportLayer.sendCommand("CAMM", new byte[]{2, 0, high, low, 0, 0, 1, 0});
        final byte[] dataToSend = new byte[20];
        dataToSend[0] = 0x08; // bitmask for the master audio fader setting
        dataToSend[12] = 0;
        dataToSend[13] = 0;
        dataToSend[14] = 0;
        dataToSend[15] = 0;
        return this.transportLayer.sendCommand("CFMP", dataToSend);
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
}
