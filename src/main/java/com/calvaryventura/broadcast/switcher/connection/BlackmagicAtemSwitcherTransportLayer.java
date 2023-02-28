package com.calvaryventura.broadcast.switcher.connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This layer is the communication protocol with the Blackmagic ATEM device.
 */
public class BlackmagicAtemSwitcherTransportLayer
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final BlackmagicAtemSwitcherNetworkLayer udpInterface;
    private int localSequenceNumber;

    // ATEM packet specs
    private static final int ATEM_headerCmd_AckRequest = 0x1; // Please acknowledge reception of this package...
    private static final int ATEM_headerCmd_HelloPacket = 0x2;
    private static final int ATEM_headerCmd_Resend = 0x4;            // This is a resent information
    private static final int ATEM_headerCmd_RequestNextAfter = 0x8;    // I'm requesting you to resend something to me.
    private static final int ATEM_headerCmd_Ack = 0x10;       // This package is an acknowledge to package id (byte 4-5) ATEM_headerCmd_AckRequest
    private static final int ATEM_maxInitPackageCount = 40;        // The maximum number of initialization packages. By observation on a 2M/E 4K can be up to (not fixed!) 32. We allocate a f more then...
    protected static final int ATEM_packetBufferLength = 96;        // Size of packet buffer

    // ATEM Connection Basics
    int _localPacketIdCounter;    // This is our counter for the command packages we might like to send to ATEM
    boolean _initPayloadSent;            // If true, the initial reception of the ATEM memory has passed and we can begin to respond during the runLoop()
    int _initPayloadSentAtPacketId;    // The Remote Package ID at which point the initialization payload was completed.
    boolean _hasInitialized;            // If true, all initial payload packets has been received during requests for resent - and we are completely ready to rock!
    boolean _isConnected;                // Set true if we have received a hello package from the switcher.
    int sessionID;                // Session id of session, given by ATEM switcher
    long lastContactMsUtc;            // Last time (millis) the switcher sent a packet to us.
    int _lastRemotePacketID;        // The most recent Remote Packet Id from switcher
    int[] _missedInitializationPackages = new int[(ATEM_maxInitPackageCount + 7) / 8];    // Used to track which initialization packages have been missed
    int _returnPacketLength;

    private boolean initializationComplete;

    // ATEM Buffer:
    byte[] _packetBuffer = new byte[ATEM_packetBufferLength];        // Buffer for storing segments of the packets from ATEM and creating answer packets.

    int _cmdLength;                // Used when parsing packets
    int _cmdPointer;                // Used when parsing packets

    // TODO removeme...
    final int _cBBO = 0;        // Bundle Buffer Offset; This is an offset if you want to add more commands.

    int _ATEMmodel;

    boolean neverConnected;
    boolean waitingForIncoming;

    /**
     * Sets up the UDP connection to the switcher, and sets up the background
     * thread which keeps the connection alive and processes rx data.
     *
     * @param switcherIp IP address of the blackmagic ATEM switcher
     *
     * @throws Exception on network error
     */
    protected BlackmagicAtemSwitcherTransportLayer(String switcherIp) throws Exception
    {
        this.neverConnected = true;
        this.waitingForIncoming = false;
        this.lastContactMsUtc = 0;
        this.udpInterface = new BlackmagicAtemSwitcherNetworkLayer(9910, switcherIp, this::rxPacketAvailable);

        // initialize the transport layer's UDP reception loop at 2Hz
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(() ->{
            try
            {
                this.doInitializationSequence();
                //this.runLoop();
            } catch (Throwable e)
            {
                logger.error("Error in transport layer loop", e);
                this.initializationComplete = false;
            }
        }, 500, 500, TimeUnit.MILLISECONDS);

    }

    /**
     *
     * @param packet
     */
    private void rxPacketAvailable(BlackmagicAtemSwitcherPacket packet)
    {
        if (initializationComplete)
        {
            try
            {
                //this.parsePayloadFields(packet.getPayloadFields());
            } catch (Exception e)
            {
                logger.info("Cannot process packet payload", e);
            }
        }
    }

    /**
     * Call this at the beginning. Sets the {@link #initializationComplete} flag.
     *
     * @throws Exception on device connection issue or initialization timeout
     */
    private void doInitializationSequence() throws Exception
    {
        if (this.initializationComplete)
        {
            return;
        }

        // create outgoing initialization packet 1
        final int randSessionId = (int) (Math.random() * 1000);
        final byte[] initialPayload = new byte[] {0x01, 0, 0, 0, 0, 0, 0, 0};
        final BlackmagicAtemSwitcherPacket firstPacket = new BlackmagicAtemSwitcherPacket(false, true, false, false, false,
                randSessionId, 0, 0, 0, null, initialPayload);

        // send first packet, and await the second
        logger.info("Sending first initialization packet to the switcher");
        this.udpInterface.getReceivedPacketQueue().clear();
        this.udpInterface.sendBlocking(firstPacket);
        final BlackmagicAtemSwitcherPacket poll = this.udpInterface.getReceivedPacketQueue().poll(1, TimeUnit.SECONDS);
        if (poll == null)
        {
            throw new Exception("Never received a response from the switcher during first initialization sequence");
        }

        // read the incoming packet 2
        logger.info("Received second packet {}", poll);
        if (poll.getRawPayloadBytes().length == 0 || poll.getRawPayloadBytes()[0] != 0x02)
        {
            throw new Exception("Switcher responded with initialization packet 2, but said we were not successful");
        }

        // create and send packet 3
        final BlackmagicAtemSwitcherPacket thirdPacket = new BlackmagicAtemSwitcherPacket(false, false, false, false, true,
                randSessionId, 0, 0, 0, null, new byte[0]);
        this.udpInterface.getReceivedPacketQueue().clear();
        this.udpInterface.sendBlocking(thirdPacket);

        // parse the received packet(s)
        this.sessionID = -1;
        while (!Thread.currentThread().isInterrupted())
        {
            // get packet(s) until we receive the empty payload field entry
            final BlackmagicAtemSwitcherPacket packet = this.udpInterface.getReceivedPacketQueue().poll(1, TimeUnit.SECONDS);
            if (packet == null)
            {
                throw new Exception("Never received a response from the switcher during third initialization sequence");
            }

            // the switcher now responds with the correct sessionID, the random one is no longer valid
            logger.info("Received switcher state packet: {}", packet);
            if (this.sessionID == -1)
            {
                this.sessionID = packet.getSessionId();
                logger.info("Switcher assigned sessionID of {} to this client", this.sessionID);
            }

            // pull the payload and see if we get the empty payload field
            this.parsePayloadFields(packet.getPayloadFields());
            if (packet.getRawPayloadBytes().length == 0)
            {
                // special case on acknowledging the packet containing the empty payload, set acknowledge and remote sequence numbers to 0x61
                final int acknowledgementNumber = packet.getRemoteSequenceNumber();
                final int remoteSequenceNumber = 0x61; // only time the client sets the remote sequence number field
                final BlackmagicAtemSwitcherPacket ackPacket = new BlackmagicAtemSwitcherPacket(true, false, false, false, true,
                        this.sessionID, acknowledgementNumber, remoteSequenceNumber, 0, null, new byte[0]);
                logger.info("Client found the last (empty) packet in the switcher's full status dump. Acknowledging switcher's last status packet...");
                this.udpInterface.sendBlocking(ackPacket);
                break;
            } else
            {
                // normal case, just respond with the reliable and ACK flags
                final int acknowledgementNumber = packet.getLocalSequenceNumber();
                final BlackmagicAtemSwitcherPacket ackPacket = new BlackmagicAtemSwitcherPacket(true, false, false, false, true,
                        this.sessionID, acknowledgementNumber, 0, 0, null, new byte[0]);
                logger.info("Client acknowledging switcher's status packet...");
                this.udpInterface.sendBlocking(ackPacket);
            }
        }

        logger.info("Completed BlackMagic switcher initialization!");
        this.initializationComplete = true;
        this.localSequenceNumber = 0;
    }

    /**
     * TODO
     * @param payloadFields
     * @return
     *
     * @throws Exception
     */
    private void parsePayloadFields(Map<String, byte[]> payloadFields) throws Exception
    {
        logger.info("Parsing {} payload fields: {}", payloadFields.size(), Arrays.toString(payloadFields.keySet().toArray()));
        payloadFields.forEach((cmdMnemonic, fieldBytes) -> {
            logger.info("Payload field '{}', bytes: {}", cmdMnemonic, DatatypeConverter.printHexBinary(fieldBytes));
        });
        // TODO
    }


// TODO make something to send packets

    /**
     * TODO
     * @param cmd
     * @param data
     * @throws Exception
     */
    protected void sendCommand(String cmd, byte[] data) throws Exception
    {
        final Map<String, byte[]> map = new HashMap<>();
        map.put(cmd, data);
        final BlackmagicAtemSwitcherPacket packet = new BlackmagicAtemSwitcherPacket(true, false, false, false, false,
                this.sessionID, 0, 0, ++this.localSequenceNumber, map, null);
        this.udpInterface.sendBlocking(packet);

// TODO do I have to receive an ACK
    }




// TODO old........................................................................................................................
    /**
     * Initiating connection handshake to the ATEM switcher.
     *
     * @throws Exception on network connection issue
     */
    private void initializeConnectionToSwitcher() throws Exception
    {
        // reset fields
        _localPacketIdCounter = 0;    // Init localPacketIDCounter to 0;
        _initPayloadSent = false;     // Will be true after initial payload of data is delivered (regular 12-byte ping packages are transmitted.)
        _hasInitialized = false;      // Will be true after initial payload of data is resent and received well
        _isConnected = false;         // Will be true after the initial hello-package handshakes.
        sessionID = 0x53AB;          // Temporary session ID - a new will be given back from ATEM.
        lastContactMsUtc = System.currentTimeMillis();
        Arrays.fill(_missedInitializationPackages, 0, (ATEM_maxInitPackageCount + 7) / 8, 0xFF);
        _initPayloadSentAtPacketId = ATEM_maxInitPackageCount;    // The max value it can be
        this.clearPacketBuffer();
        this._createCommandHeader(ATEM_headerCmd_HelloPacket, 12 + 8);
        _packetBuffer[12] = 0x01; // This seems to be what the client should send upon first request
        _packetBuffer[9] = 0x3a;  // This seems to be what the client should send upon first request
        logger.info("Sending hello packet to switcher...");
        this.sendPacketBuffer(20);
    }

    /**
     * TODO
     * @param length
     * @throws Exception network connection issue
     */
    protected void sendPacketBuffer(int length) throws Exception
    {
        this.udpInterface.sendBlocking(this._packetBuffer, length);
    }

    /**
     * Keeps connection to the switcher alive
     * Therefore: Call this in the Arduino loop() function and make sure it gets call at least 2 times a second
     * Other recommendations might come up in the future.
     */
    private void runLoop() throws Exception
    {
        logger.info("Top of loop...");
        if (neverConnected)
        {
            this.initializeConnectionToSwitcher();
            neverConnected = false;
        }

        // Iterate until UDP buffer is empty
        while (this.udpInterface.rxDataAvailable())
        {
            this.udpInterface.readIntoBuffer(_packetBuffer, 12); // read header
            final int packetSize = 12;
            logger.info("Read UDP bytes header: {}", IntStream.range(0, 12).boxed().map(i ->
                    String.format("%02X", _packetBuffer[i])).collect(Collectors.joining(" ")));

            sessionID = BlackmagicAtemSwitcherPacketUtils.word(_packetBuffer[2], _packetBuffer[3]);
            logger.info("Session ID={}", sessionID);
            int headerBitmask = _packetBuffer[0] >> 3;
            _lastRemotePacketID = BlackmagicAtemSwitcherPacketUtils.word(_packetBuffer[10], _packetBuffer[11]);
            logger.info("Session ID={}, headerBitmask=0x{}, _lastRemotePacketID={}", sessionID, String.format("%02X", headerBitmask), _lastRemotePacketID);
            if (_lastRemotePacketID < ATEM_maxInitPackageCount)
            {
                _missedInitializationPackages[_lastRemotePacketID >> 3] &= ~(1 << (_lastRemotePacketID & 0x07));
            }

            int packetLength = BlackmagicAtemSwitcherPacketUtils.word((byte) (_packetBuffer[0] & 0b00000111), _packetBuffer[1]);
            logger.info("packetLength={}", packetLength);

            if (packetSize == packetLength)
            {  // Just to make sure these are equal, they should be!
                lastContactMsUtc = System.currentTimeMillis();
                waitingForIncoming = false;

                if ((headerBitmask & ATEM_headerCmd_HelloPacket) == ATEM_headerCmd_HelloPacket)
                {    // Respond to "Hello" packages:
                    _isConnected = true;

                    // _packetBuffer[12]	The ATEM will return a "2" in this return package of same length. If the ATEM returns "3" it means "fully booked" (no more clients can connect) and a "4" seems to be a kind of reconnect (seen when you drop the connection and the ATEM desperately tries to figure out what happened...)
                    // _packetBuffer[15]	This number seems to increment with about 3 each time a new client tries to connect to ATEM. It may be used to judge how many client connections has been made during the up-time of the switcher?

                    logger.info("hello packet!");
                    clearPacketBuffer();
                    _createCommandHeader(ATEM_headerCmd_Ack, 12);
                    _packetBuffer[9] = 0x03;    // This seems to be what the client should send upon first request.
                    sendPacketBuffer(12);
                }

                // If a packet is 12 bytes long it indicates that all the initial information
                // has been delivered from the ATEM and we can begin to answer back on every request
                // Currently we don't know any other way to decide if an answer should be sent back...
                // The QT lib uses the "InCm" command to indicate this, but in the latest version of the firmware (2.14)
                // all the camera control information comes AFTER this command, so it's not a clear ending token anymore.
                // However, I'm not sure if I checked the _lastRemotePacketID of the packages with the additional camera control info - if it was a resend,
                // "InCm" may still indicate the number of the last init-package and that's all I need to request the missing ones....

                // BTW: It has been observed on an old 10Mbit hub that packages could arrive in a different order than sent and this may
                // mess things up a bit on the initialization. So it's recommended to has as direct routes as possible.
                if (!_initPayloadSent && packetSize == 12 && _lastRemotePacketID > 1)
                {
                    _initPayloadSent = true;
                    _initPayloadSentAtPacketId = _lastRemotePacketID;
                    logger.info("_initPayloadSent=TRUE @rpID {} session ID: {}", _initPayloadSentAtPacketId, sessionID);
                }

                if (_initPayloadSent && (headerBitmask & ATEM_headerCmd_AckRequest) == ATEM_headerCmd_AckRequest
                        && (_hasInitialized || !((headerBitmask & ATEM_headerCmd_Resend) == ATEM_headerCmd_Resend)))
                {    // Respond to request for acknowledge	(and to resends also, whatever...
                    this.clearPacketBuffer();
                    this._createCommandHeader(ATEM_headerCmd_Ack, 12, _lastRemotePacketID);
                    this.sendPacketBuffer(12);
                    logger.info("remotePacketID: {}, Header: 0x{}, Packet: {} bytes - ACK", _lastRemotePacketID, String.format("%02X", headerBitmask), packetLength);
                } else if (_initPayloadSent && (headerBitmask & ATEM_headerCmd_RequestNextAfter) == ATEM_headerCmd_RequestNextAfter && _hasInitialized)
                {    // ATEM is requesting a previously sent package which must have dropped out of the order. We return an empty one so the ATEM doesnt' crash (which some models will, if it doesn't get an answer before another 63 commands gets sent from the controller.)
                    byte b1 = _packetBuffer[6];
                    byte b2 = _packetBuffer[7];
                    this.clearPacketBuffer();
                    this._createCommandHeader(ATEM_headerCmd_Ack, 12, 0);
                    _packetBuffer[0] = ATEM_headerCmd_AckRequest << 3;    // Overruling this. A small trick because createCommandHeader shouldn't increment local package ID counter
                    _packetBuffer[10] = b1;
                    _packetBuffer[11] = b2;
                    this.sendPacketBuffer(12);
                    logger.info("ATEM asking to resend {}", (b1 << 8) | b2);
                } else
                {
                    logger.info("remotePacketID: {}, Header: 0x{}, Packet: {} bytes", _lastRemotePacketID, String.format("%02X", headerBitmask), packetLength);
                }

                if (!((headerBitmask & ATEM_headerCmd_HelloPacket) == ATEM_headerCmd_HelloPacket) && packetLength > 12)
                {
                    this._parsePacket(packetLength);
                }
            } else
            {
                logger.info("ERROR: Packet size mismatch: Size:{} != Len:{}", packetSize, packetLength);

                // Flushing
                this.udpInterface.flushReceivedBuffer();
            }
        }

        // After initialization, we check which packages were missed and ask for them:
        if (!_hasInitialized && _initPayloadSent && !waitingForIncoming)
        {
            for (int i = 1; i < _initPayloadSentAtPacketId; i++)
            {
                if (i <= ATEM_maxInitPackageCount)
                {
                    if ((_missedInitializationPackages[i >> 3] & (1 << (i & 0x7))) == (1 << (i & 0x7)))
                    {
                        logger.info("Asking for package: {}", i);
                        this.clearPacketBuffer();
                        this._createCommandHeader(ATEM_headerCmd_RequestNextAfter, 12);
                        _packetBuffer[6] = BlackmagicAtemSwitcherPacketUtils.highByte(i - 1);  // Resend Packet ID, MSB
                        _packetBuffer[7] = BlackmagicAtemSwitcherPacketUtils.lowByte(i - 1);  // Resend Packet ID, LSB
                        _packetBuffer[8] = 0x01;

                        this.sendPacketBuffer(12);
                        waitingForIncoming = true;
                        break;
                    }
                } else
                {
                    break;
                }
            }
            if (!waitingForIncoming)
            {
                _hasInitialized = true;
                logger.info("ATEM _hasInitialized = TRUE");
            }
        }

        // If connection is gone anyway, try to reconnect:
        if (hasTimedOut(lastContactMsUtc, 5000))
        {
            logger.info("Connection to ATEM Switcher has timed out - reconnecting!");
            this.initializeConnectionToSwitcher();
        }
    }

    /**
     * Returns last Remote Packet ID
     */
    public int getATEM_lastRemotePacketId()
    {
        return _lastRemotePacketID;
    }

    /**
     * Get ATEM session ID
     */
    public int getSessionID()
    {
        return sessionID;
    }

    /**
     * If true, we had a response from the switcher when trying to send a hello packet.
     */
    public boolean isConnected()
    {
        return _isConnected;
    }

    /**
     * If true, the initial handshake and "stressful" information exchange has occured and now the switcher connection should be ready for operation.
     */
    public boolean hasInitialized()
    {
        return _hasInitialized;
    }

    /* *************
     * Buffer work
     **************/

    public void _createCommandHeader(int headerCmd, int lengthOfData)
    {
        _createCommandHeader(headerCmd, lengthOfData, 0);
    }

    public void _createCommandHeader(int headerCmd, int lengthOfData, int remotePacketID)
    {
        _packetBuffer[0] = (byte) ((headerCmd << 3) | (BlackmagicAtemSwitcherPacketUtils.highByte(lengthOfData) & 0x07));  // Command bits + length MSB
        _packetBuffer[1] = BlackmagicAtemSwitcherPacketUtils.lowByte(lengthOfData);  // length LSB

        _packetBuffer[2] = BlackmagicAtemSwitcherPacketUtils.highByte(sessionID);  // Session ID
        _packetBuffer[3] = BlackmagicAtemSwitcherPacketUtils.lowByte(sessionID);  // Session ID

        _packetBuffer[4] = BlackmagicAtemSwitcherPacketUtils.highByte(remotePacketID);  // Remote Packet ID, MSB
        _packetBuffer[5] = BlackmagicAtemSwitcherPacketUtils.lowByte(remotePacketID);  // Remote Packet ID, LSB

        if (!((headerCmd & (ATEM_headerCmd_HelloPacket | ATEM_headerCmd_Ack | ATEM_headerCmd_RequestNextAfter)) == (ATEM_headerCmd_HelloPacket | ATEM_headerCmd_Ack | ATEM_headerCmd_RequestNextAfter)))
        {
            _localPacketIdCounter++;
//		if ((_localPacketIdCounter & 0xF) == 0xF) _localPacketIdCounter++;	// Uncommenting this line will jump the local package ID counter every 15 command - thereby introducing a stress test of the robustness of the "resent package" function from the ATEM switcher.
            _packetBuffer[10] = BlackmagicAtemSwitcherPacketUtils.highByte(_localPacketIdCounter);  // Local Packet ID, MSB
            _packetBuffer[11] = BlackmagicAtemSwitcherPacketUtils.lowByte(_localPacketIdCounter);  // Local Packet ID, LSB
        }
    }

    /**
     * Sets all zeros in packet buffer:
     */
    private void clearPacketBuffer()
    {
        Arrays.fill(_packetBuffer, 0, ATEM_packetBufferLength, (byte) 0);
    }

    /**
     * Reads from UDP channel to buffer. Will fill the buffer to the max or to the size of the current segment being parsed
     * Returns false if there are no more bytes, otherwise true
     */
    protected boolean _readToPacketBuffer()
    {
        return _readToPacketBuffer(ATEM_packetBufferLength);
    }

    protected boolean _readToPacketBuffer(int maxBytes)
    {
        maxBytes = Math.min(maxBytes, ATEM_packetBufferLength);
        int remainingBytes = _cmdLength - 8 - _cmdPointer;

        if (remainingBytes > 0)
        {
            if (remainingBytes <= maxBytes)
            {
                this.udpInterface.readIntoBuffer(_packetBuffer, remainingBytes);
                _cmdPointer += remainingBytes;
                return false;    // Returns false if finished.
            } else
            {
                this.udpInterface.readIntoBuffer(_packetBuffer, maxBytes);
                _cmdPointer += maxBytes;
                return true;    // Returns true if there are still bytes to be read.
            }
        } else
        {
            return false;
        }
    }

    /**
     * If a package longer than a normal acknowledgement is received from the ATEM Switcher we must read through the contents.
     * Usually such a package contains updated state information about the mixer
     * Selected information is extracted in this function and transferred to internal variables in this library.
     */
    private void _parsePacket(int packetLength)
    {
        // If packet is more than an ACK packet (= if its longer than 12 bytes header), lets parse it:
        int indexPointer = 12;    // 12 bytes has already been read from the packet...
        while (indexPointer < packetLength)
        {
            // Read the length of segment (first word):
            this.udpInterface.readIntoBuffer(_packetBuffer, 8);
            _cmdLength = BlackmagicAtemSwitcherPacketUtils.word(_packetBuffer[0], _packetBuffer[1]);
            _cmdPointer = 0;
            logger.info("_parsePacket read 8 byte word of {}, made up of {} and {}", _cmdLength, _packetBuffer[0], _packetBuffer[1]);

            // Get the "command string", basically this is the 4 char variable name in the ATEM memory holding the various state values of the system:
            final String cmdStr = new String(_packetBuffer, 4, 4); // {_packetBuffer[4] _packetBuffer[5], _packetBuffer[6], _packetBuffer[7], '\0'};
            logger.info("_parsePacket command string {}", cmdStr);

            // If length of segment larger than 8 (should always be...!)
            if (_cmdLength > 8)
            {
                _parseGetCommands(cmdStr);

                while (_readToPacketBuffer())
                {
                }    // Empty, if not done yet.
                indexPointer += _cmdLength;
            } else
            {
                indexPointer = 2000;

                logger.info("Bad CMD length, flushing...");

                // Flushing the buffer
                this.udpInterface.flushReceivedBuffer();
            }
        }
    }

    /**
     * This method should be overloaded in subclasses in order to handle specific get-commands
     */
    private void _parseGetCommands(String cmdString)
    {
//	uint8_t mE, keyer, mediaPlayer, aUXChannel, windowIndex, multiViewer, memory, colorGenerator, box;
//	uint16_t audioSource, videoSource;
//	long temp;

        int numberOfReads = 1;
        while (_readToPacketBuffer())
        {
            numberOfReads++;
        }

        logger.info("{}, len: {}, rds: {}", cmdString, _cmdLength, numberOfReads);
    }

    /**
     * TODO
     *
     * @param cmdString
     * @param cmdBytes
     */
    protected void _prepareCommandPacket(String cmdString, int cmdBytes)
    {
        clearPacketBuffer();

        _returnPacketLength = 12 + _cBBO + (4 + 4 + cmdBytes);

        // Because we increased length of command, we need to check for buffer overflow:
        if (_returnPacketLength > ATEM_packetBufferLength)
        {
            logger.info("FATAL ERROR: Packet Buffer Overflow in the ATEM Library! Too long or too many commands bundled!\n HALT");
            while (true)
            {
            }    // STOP!
        }

        // Copy Command String:
        if (cmdString.length() == 4)
        {
            System.arraycopy(cmdString.getBytes(), 0, _packetBuffer, 12 + _cBBO + 4, 4);
        } else
        {
            logger.info("Command Length > 4 ERROR");
        }

        // Command length:
        _packetBuffer[12 + _cBBO] = 0;    // MSB - but it's always under 256, so....
        _packetBuffer[12 + 1 + _cBBO] = (byte) (4 + 4 + cmdBytes);    // LSB
    }

    /**
     * TODO
     */
    protected void _finishCommandPacket() throws Exception
    {
        this._createCommandHeader(ATEM_headerCmd_AckRequest, _returnPacketLength);
        this.sendPacketBuffer(_returnPacketLength);
        _returnPacketLength = 0;
    }

    /**
     * Timeout check
     */
    public boolean hasTimedOut(long time, long timeout)
    {
        // This should "wrap around" if time+timout is larger than the size of unsigned-longs, right?
        if (time + timeout <= System.currentTimeMillis())
        {
            return true;
        } else
        {
            return false;
        }
    }

    public int getATEMmodel()
    {
        return _ATEMmodel;
    }
}
