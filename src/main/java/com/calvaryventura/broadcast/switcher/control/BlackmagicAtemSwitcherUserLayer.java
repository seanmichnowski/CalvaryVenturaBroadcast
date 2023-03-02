package com.calvaryventura.broadcast.switcher.control;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * TODO
 * <p>
 * There is a {@link BlackmagicAtemSwitcherTransportLayer} which lives inside this class,
 * and a {@link BlackmagicAtemSwitcherNetworkLayer} which lives inside of that one.
 */
public class BlackmagicAtemSwitcherUserLayer
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final BlackmagicAtemSwitcherTransportLayer transportLayer;

    private final boolean[] atemTransitionInTransition = new boolean[2];
    private final int[] atemTransitionFramesRemaining = new int[2];
    private final int[] atemTransitionPosition = new int[2];
    private final boolean[] atemFadeToBlackStateFullyBlack = new boolean[2];
    private final boolean[] atemFadeToBlackStateInTransition = new boolean[2];
    private final boolean[] atemFadeToBlackStateFramesRemaining = new boolean[2];
    private final int[] atemAuxSourceInput = new int[6];
    private final int[] atemTallyByIndexTallyFlags = new int[21];

    private int currentVideoPreviewIdx;
    private int currentVideoProgramIdx;
    private int currentVideoAuxIdx;
    private boolean upstreamKeyOn;
    private boolean fadeToBlackOn;
    private int transitionPosition;

// TODO
//  have a callback from the transport layer for switcher connection/init status (or maybe use a CONN command mnemonic?)
//  implement more status field parsing via pyatm
//  uncomment in the network layer and see why we aren't properly acknowledging the empty packet?
//  use companion and see if there's a way to get the current status after we send a command, or just an ACK?

    /**
     * Creates the transport layer for low-level messaging.
     * The transport layer also handles initialization.
     *
     * @param switcherIp IP address for the Blackmagic ATEM switcher
     */
    public BlackmagicAtemSwitcherUserLayer(String switcherIp) throws Exception
    {
        this.transportLayer = new BlackmagicAtemSwitcherTransportLayer(switcherIp,
                this::parseSwitcherStatusFromTransportLayer);
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
            case "PrgI":
                this.currentVideoProgramIdx = BlackmagicAtemSwitcherPacketUtils.word(data[2], data[3]);
                logger.info("Current program video: {}", this.currentVideoProgramIdx);
                break;
            case "PrvI":
                this.currentVideoPreviewIdx = BlackmagicAtemSwitcherPacketUtils.word(data[2], data[3]);
                logger.info("Current preview video: {}", this.currentVideoPreviewIdx);
                break;
            case "AuxS":
                logger.info("Aux video output: TODO");
                break;
            case "KeOn":
                logger.info("Upstream key: TODO");
                break;
            case "FtbP":
                logger.info("Fade to black: TODO");
                break;
            default:
                break;
        }
    }

    public int getCurrentVideoPreviewIdx()
    {
        return this.currentVideoPreviewIdx;
    }

    public int getCurrentVideoProgramIdx()
    {
        return this.currentVideoProgramIdx;
    }

    public int getCurrentVideoAuxIdx()
    {
        return this.currentVideoAuxIdx;
    }

    public boolean isUpstreamKeyOn()
    {
        return this.upstreamKeyOn;
    }

    public boolean isFadeToBlackOn()
    {
        return this.fadeToBlackOn;
    }

    public int getTransitionPosition()
    {
        return this.transitionPosition;
    }

    /**
     * TODO
     * @param videoSourceIdx
     * @throws Exception
     */
    public void setProgramVideo(int videoSourceIdx) throws Exception
    {
        final byte high = (byte) ((videoSourceIdx >> 8) & 0xFF);
        final byte low = (byte) (videoSourceIdx & 0xFF);
        this.transportLayer.sendCommand("CPgI", new byte[]{0, 0, high, low});
    }

    /**
     * TODO
     * @param videoSourceIdx
     * @throws Exception
     */
    public void setPreviewVideo(int videoSourceIdx) throws Exception
    {
        final byte high = (byte) ((videoSourceIdx >> 8) & 0xFF);
        final byte low = (byte) (videoSourceIdx & 0xFF);
        this.transportLayer.sendCommand("CPvI", new byte[]{0, 0, high, low});
    }

    /**
     * Set Cut; M/E
     * mE 	0: ME1, 1: ME2
     */
    public void performCutME(byte mE) throws Exception
    {
        this.transportLayer.sendCommand("DCut", new byte[0]); // TODO send 4 0 bytes?
    }

    /**
     * Set Auto; M/E
     * mE 	0: ME1, 1: ME2
     */
    public void performAutoME(byte mE) throws Exception
    {
        this.transportLayer.sendCommand("DAut", new byte[0]); // TODO send 4 0 bytes?
    }

    /**
     * Get Transition Position; In Transition
     * mE 	0: ME1, 1: ME2
     */
    public boolean getTransitionInTransition(int mE)
    {
        return atemTransitionInTransition[mE];
    }

    /**
     * Get Transition Position; Frames Remaining
     * mE 	0: ME1, 1: ME2
     */
    public int getTransitionFramesRemaining(int mE)
    {
        return atemTransitionFramesRemaining[mE];
    }

    /**
     * Get Transition Position; Position
     * mE 	0: ME1, 1: ME2
     */
    public int getTransitionPosition(int mE)
    {
        return atemTransitionPosition[mE];
    }

    /**
     * Set Transition Position; Position
     * mE 	0: ME1, 1: ME2
     * position 	0-9999
     */
    public void setTransitionPosition(int position) throws Exception
    {
        final byte high = (byte) ((position >> 8) & 0xFF);
        final byte low = (byte) (position & 0xFF);
        this.transportLayer.sendCommand("CTPs", new byte[]{0, 0, high, low});
    }

    /**
     * Set Keyer On Air; Enabled
     * mE 	0: ME1, 1: ME2
     * keyer 	0-3: Keyer 1-4
     * enabled 	Bit 0: On/Off
     */
    public void setKeyerOnAirEnabled(byte mE, byte keyer, boolean enabled) throws Exception
    {
        this.transportLayer.sendCommand("CKOn", new byte[]{0, keyer, (byte) (enabled ? 1 : 0), 0}); // TODO can I send 3 bytes?
    }

    /**
     * Get Fade-To-Black State; Fully Black
     * mE 	0: ME1, 1: ME2
     */
    public boolean getFadeToBlackStateFullyBlack(int mE)
    {
        return atemFadeToBlackStateFullyBlack[mE];
    }

    /**
     * Get Fade-To-Black State; In Transition
     * mE 	0: ME1, 1: ME2
     */
    public boolean getFadeToBlackStateInTransition(int mE)
    {
        return atemFadeToBlackStateInTransition[mE];
    }

    /**
     * Get Fade-To-Black State; Frames Remaining
     * mE 	0: ME1, 1: ME2
     */
    public boolean getFadeToBlackStateFramesRemaining(int mE)
    {
        return atemFadeToBlackStateFramesRemaining[mE];
    }

    /**
     * Set Fade-To-Black; M/E
     * mE 	0: ME1, 1: ME2
     */
    public void performFadeToBlackME(byte mE) throws Exception
    {
        this.transportLayer.sendCommand("FtbA", new byte[]{0, 0x02, 0, 0}); // TODO can I send 2 bytes?
    }

    /**
     * Get Aux Source; Input
     * aUXChannel 	0-5: Aux 1-6
     */
    public int getAuxSourceInput(int aUXChannel)
    {
        return atemAuxSourceInput[aUXChannel];
    }

    /**
     * Set Aux Source; Input
     * aUXChannel 	0-5: Aux 1-6
     * input 	(See video source list)
     */
    public void setAuxSourceInput(byte aUXChannel, int input) throws Exception
    {
        final byte high = (byte) ((input >> 8) & 0xFF);
        final byte low = (byte) (input & 0xFF);
        this.transportLayer.sendCommand("CAuS", new byte[]{1, aUXChannel, high, low});
    }

    /**
     * Get Tally By Index; Tally Flags
     * sources 	0-20: Number of
     */
    public int getTallyByIndexTallyFlags(int sources)
    {
        return atemTallyByIndexTallyFlags[sources];
    }

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

    /*
     * Translating a index to a video source
     */
    public int getVideoIndexSrc(int index)
    {
        switch (index)
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
            case 21:  // Color Bars
                return 1000;
            case 22:  // Color 1
                return 2001;
            case 23:  // Color 2
                return 2002;
            case 24:  // Media Player 1
                return 3010;
            case 25:  // Media Player 1 Key
                return 3011;
            case 26:  // Media Player 2
                return 3020;
            case 27:  // Media Player 2 Key
                return 3021;
            case 28:  // Key 1 Mask
                return 4010;
            case 29:  // Key 2 Mask
                return 4020;
            case 30:  // Key 3 Mask
                return 4030;
            case 31:  // Key 4 Mask
                return 4040;
            case 32:  // DSK 1 Mask
                return 5010;
            case 33:  // DSK 2 Mask
                return 5020;
            case 34:  // Super Source
                return 6000;
            case 35:  // Clean Feed 1
                return 7001;
            case 36:  // Clean Feed 2
                return 7002;
            case 37:  // Auxilary 1
                return 8001;
            case 38:  // Auxilary 2
                return 8002;
            case 39:  // Auxilary 3
                return 8003;
            case 40:  // Auxilary 4
                return 8004;
            case 41:  // Auxilary 5
                return 8005;
            case 42:  // Auxilary 6
                return 8006;
            case 43:  // ME 1 Prog
                return 10010;
            case 44:  // ME 1 Prev
                return 10011;
            case 45:  // ME 2 Prog
                return 10020;
            case 46:  // ME 2 Prev
                return 10021;
            default:
                return 0;
        }
    }
}
