package com.calvaryventura.broadcast.switcher.connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * TODO
 */
public class BlackmagicAtemSwitcherUserLayer extends BlackmagicAtemSwitcherTransportLayer
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final int[] atemProgramInputVideoSource = new int[2];
    private final int[] atemPreviewInputVideoSource = new int[2];
    private final boolean[] atemTransitionInTransition = new boolean[2];
    private final int[] atemTransitionFramesRemaining = new int[2];
    private final int[] atemTransitionPosition = new int[2];
    private final boolean[][] atemKeyerOnAirEnabled = new boolean[2][4];
    private final boolean[] atemFadeToBlackStateFullyBlack = new boolean[2];
    private final boolean[] atemFadeToBlackStateInTransition = new boolean[2];
    private final boolean[] atemFadeToBlackStateFramesRemaining = new boolean[2];
    private final int[] atemAuxSourceInput = new int[6];
    private final int[] atemTallyByIndexTallyFlags = new int[21];
    private int atemTallyByIndexSources;

    /**
     * TODO
     *
     * @param switcherIp
     */
    public BlackmagicAtemSwitcherUserLayer(String switcherIp) throws Exception
    {
        super(switcherIp);
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
        super.sendCommand("PrgI", new byte[]{0, 0, high, low});
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
        super.sendCommand("TODO", new byte[]{0, 0, high, low}); // TODO
    }


    /**
     * TODO
     *
     * @param cmdStr
     */
    private void _parseGetCommands(String cmdStr)
    {
        byte mE, keyer, aUXChannel;
        int sources;
        long temp;
        byte readBytesForTlSr;

        if (cmdStr.equals("AMLv"))
        {
            _readToPacketBuffer(36);
        } else if (cmdStr.equals("TlSr"))
        {
            readBytesForTlSr = ((ATEM_packetBufferLength - 2) / 3) * 3 + 2;
            _readToPacketBuffer(readBytesForTlSr);
        } else
        {
            _readToPacketBuffer();    // Default
        }

        if (cmdStr.equals("_pin"))
        {
            if (_packetBuffer[5] == 'T')
            {
                _ATEMmodel = 0;
            } else if (_packetBuffer[5] == '1')
            {
                _ATEMmodel = _packetBuffer[29] == '4' ? 4 : 1;
            } else if (_packetBuffer[5] == '2')
            {
                _ATEMmodel = _packetBuffer[29] == '4' ? 5 : 2;
            } else if (_packetBuffer[5] == 'P')
            {
                _ATEMmodel = 3;
            }
            logger.info("Switcher type: {}, {}", _ATEMmodel, _ATEMmodel == 0 ? "Television Studio" : _ATEMmodel == 1 ? "ATEM 1 M/E" : _ATEMmodel == 2 ? "ATEM 2 M/E" :
                    _ATEMmodel == 3 ? "ATEM Production Studio 4K" : _ATEMmodel == 4 ? "ATEM 1 M/E 4K" : _ATEMmodel == 5 ? "ATEM 2 M/E 4K" : "unknown");
        }

        if (cmdStr.equals("PrgI"))
        {

            mE = _packetBuffer[0];
            if (mE <= 1)
            {
                atemProgramInputVideoSource[mE] = BlackmagicAtemSwitcherPacketUtils.word(_packetBuffer[2], _packetBuffer[3]);
                logger.info("atemProgramInputVideoSource[mE={}] = {}", mE, atemProgramInputVideoSource[mE]);
            }
        } else if (cmdStr.equals("PrvI"))
        {
            mE = _packetBuffer[0];
            if (mE <= 1)
            {
                atemPreviewInputVideoSource[mE] = BlackmagicAtemSwitcherPacketUtils.word(_packetBuffer[2], _packetBuffer[3]);
                logger.info("atemPreviewInputVideoSource[mE={}] = {}", mE, atemPreviewInputVideoSource[mE]);
            }
        } else if (cmdStr.equals("TrPs"))
        {
            mE = _packetBuffer[0];
            if (mE <= 1)
            {
                atemTransitionInTransition[mE] = _packetBuffer[1] > 0; // TODO?
                logger.info("atemTransitionInTransition[mE={}] = {}", mE, atemTransitionInTransition[mE]);

                atemTransitionFramesRemaining[mE] = _packetBuffer[2];
                logger.info("atemTransitionFramesRemaining[mE={}] = {}", mE, atemTransitionFramesRemaining[mE]);

                atemTransitionPosition[mE] = BlackmagicAtemSwitcherPacketUtils.word(_packetBuffer[4], _packetBuffer[5]);
                logger.info("atemTransitionPosition[mE={}] = {}", mE, atemTransitionPosition[mE]);
            }
        } else if (cmdStr.equals("KeOn"))
        {
            mE = _packetBuffer[0];
            keyer = _packetBuffer[1];
            if (mE <= 1 && keyer <= 3)
            {
                atemKeyerOnAirEnabled[mE][keyer] = _packetBuffer[2] > 0; // TODO boolean?
                logger.info("atemKeyerOnAirEnabled[mE={}][keyer={}] = {}", mE, keyer, atemKeyerOnAirEnabled[mE][keyer]);
            }
        } else if (cmdStr.equals("FtbS"))
        {
            mE = _packetBuffer[0];
            if (mE <= 1)
            {
                atemFadeToBlackStateFullyBlack[mE] = _packetBuffer[1] > 0; // TODO booelan?
                logger.info("atemFadeToBlackStateFullyBlack[mE={}] = {}", mE, atemFadeToBlackStateFullyBlack[mE]);


                atemFadeToBlackStateInTransition[mE] = _packetBuffer[2] > 0; // TODO booelan?
                logger.info("atemFadeToBlackStateInTransition[mE={}] = {}", mE, atemFadeToBlackStateInTransition[mE]);

                atemFadeToBlackStateFramesRemaining[mE] = _packetBuffer[3] > 0; // TODO boolean or is this a number?
                logger.info("atemFadeToBlackStateFramesRemaining[mE={}] = {}", mE, atemFadeToBlackStateFramesRemaining[mE]);
            }
        } else if (cmdStr.equals("AuxS"))
        {
            aUXChannel = _packetBuffer[0];
            if (aUXChannel <= 5)
            {
                atemAuxSourceInput[aUXChannel] = BlackmagicAtemSwitcherPacketUtils.word(_packetBuffer[2], _packetBuffer[3]);
                logger.info("atemAuxSourceInput[aUXChannel={}] = {}", aUXChannel, atemAuxSourceInput[aUXChannel]);
            }
        } else if (cmdStr.equals("TlIn"))
        {
            sources = BlackmagicAtemSwitcherPacketUtils.word(_packetBuffer[0], _packetBuffer[1]);
            if (sources <= 20)
            {
                atemTallyByIndexSources = BlackmagicAtemSwitcherPacketUtils.word(_packetBuffer[0], _packetBuffer[1]);
                logger.info("atemTallyByIndexSources = {}", atemTallyByIndexSources);

                for (int a = 0; a < sources; a++)
                {
                    atemTallyByIndexTallyFlags[a] = _packetBuffer[2 + a];
                    logger.info("atemTallyByIndexTallyFlags[a={}] = {}", a, atemTallyByIndexTallyFlags[a]);
                }
            }
        } else
        {
            logger.info("Unknown command");
        }
    }

    /**
     * Get Program Input; Video Source
     * mE 	0: ME1, 1: ME2
     */
    public int getProgramInputVideoSource(int mE)
    {
        return atemProgramInputVideoSource[mE];
    }

    /**
     * Set Program Input; Video Source
     * mE 	0: ME1, 1: ME2
     * videoSource 	(See video source list)
     */
    public void setProgramInputVideoSource(byte mE, int videoSource) throws Exception
    {
        super._prepareCommandPacket("CPgI", 4);
        _packetBuffer[12 + _cBBO + 4 + 4] = mE;
        _packetBuffer[12 + _cBBO + 4 + 4 + 2] = BlackmagicAtemSwitcherPacketUtils.highByte(videoSource);
        _packetBuffer[12 + _cBBO + 4 + 4 + 3] = BlackmagicAtemSwitcherPacketUtils.lowByte(videoSource);
        super._finishCommandPacket();
    }

    /**
     * Get Preview Input; Video Source
     * mE 	0: ME1, 1: ME2
     */
    public int getPreviewInputVideoSource(int mE)
    {
        return atemPreviewInputVideoSource[mE];
    }

    /**
     * Set Preview Input; Video Source
     * mE 	0: ME1, 1: ME2
     * videoSource 	(See video source list)
     */
    public void setPreviewInputVideoSource(byte mE, int videoSource) throws Exception
    {
        super._prepareCommandPacket("CPvI", 4);
        _packetBuffer[12 + _cBBO + 4 + 4] = mE;
        _packetBuffer[12 + _cBBO + 4 + 4 + 2] = BlackmagicAtemSwitcherPacketUtils.highByte(videoSource);
        _packetBuffer[12 + _cBBO + 4 + 4 + 3] = BlackmagicAtemSwitcherPacketUtils.lowByte(videoSource);
        super._finishCommandPacket();
    }

    /**
     * Set Cut; M/E
     * mE 	0: ME1, 1: ME2
     */
    public void performCutME(byte mE) throws Exception
    {
        super._prepareCommandPacket("DCut", 4);
        _packetBuffer[12 + _cBBO + 4 + 4] = mE;
        super._finishCommandPacket();
    }

    /**
     * Set Auto; M/E
     * mE 	0: ME1, 1: ME2
     */
    public void performAutoME(byte mE) throws Exception
    {
        super._prepareCommandPacket("DAut", 4);
        _packetBuffer[12 + _cBBO + 4 + 4] = mE;
        super._finishCommandPacket();
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
    public void setTransitionPosition(byte mE, int position) throws Exception
    {
        super._prepareCommandPacket("CTPs", 4);
        _packetBuffer[12 + _cBBO + 4 + 4] = mE;
        _packetBuffer[12 + _cBBO + 4 + 4 + 2] = BlackmagicAtemSwitcherPacketUtils.highByte(position);
        _packetBuffer[12 + _cBBO + 4 + 4 + 3] = BlackmagicAtemSwitcherPacketUtils.lowByte(position);
        super._finishCommandPacket();
    }

    /**
     * Get Keyer On Air; Enabled
     * mE 	0: ME1, 1: ME2
     * keyer 	0-3: Keyer 1-4
     */
    public boolean getKeyerOnAirEnabled(int mE, int keyer)
    {
        return atemKeyerOnAirEnabled[mE][keyer];
    }

    /**
     * Set Keyer On Air; Enabled
     * mE 	0: ME1, 1: ME2
     * keyer 	0-3: Keyer 1-4
     * enabled 	Bit 0: On/Off
     */
    public void setKeyerOnAirEnabled(byte mE, byte keyer, boolean enabled) throws Exception
    {
        super._prepareCommandPacket("CKOn", 4);
        _packetBuffer[12 + _cBBO + 4 + 4] = mE;
        _packetBuffer[12 + _cBBO + 4 + 4 + 1] = keyer;
        _packetBuffer[12 + _cBBO + 4 + 4 + 2] = (byte) (enabled ? 1 : 0);
        super._finishCommandPacket();
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
        super._prepareCommandPacket("FtbA", 4);
        _packetBuffer[12 + _cBBO + 4 + 4] = mE;
        _packetBuffer[12 + _cBBO + 4 + 4 + 1] = 0x02;
        super._finishCommandPacket();
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
        super._prepareCommandPacket("CAuS", 4);
        // Set Mask: 1
        _packetBuffer[12 + _cBBO + 4 + 4] |= 1;
        _packetBuffer[12 + _cBBO + 4 + 4 + 1] = aUXChannel;
        _packetBuffer[12 + _cBBO + 4 + 4 + 2] = BlackmagicAtemSwitcherPacketUtils.highByte(input);
        _packetBuffer[12 + _cBBO + 4 + 4 + 3] = BlackmagicAtemSwitcherPacketUtils.lowByte(input);
        super._finishCommandPacket();
    }

    /**
     * Get Tally By Index; Sources
     */
    public int getTallyByIndexSources()
    {
        return atemTallyByIndexSources;
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
