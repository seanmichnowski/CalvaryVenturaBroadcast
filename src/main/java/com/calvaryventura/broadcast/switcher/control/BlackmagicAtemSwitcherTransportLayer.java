package com.calvaryventura.broadcast.switcher.control;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * This layer is the communication protocol with the Blackmagic ATEM device.
 */
public class BlackmagicAtemSwitcherTransportLayer
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final BlackmagicAtemSwitcherNetworkLayer udpInterface;
    private int localSequenceNumber;
    private int sessionID; // given by ATEM switcher upon connect
    private boolean initializationComplete;
    private final BiConsumer<String, byte[]> dataFieldAvailableConsumer;

    /**
     * Sets up the UDP connection to the switcher, and sets up the background
     * thread which keeps the connection alive and processes rx data.
     *
     * @param switcherIp IP address of the blackmagic ATEM switcher
     * @param dataFieldAvailableConsumer fired when a new data/status field is ready from the switcher
     *
     * @throws Exception on network error
     */
    protected BlackmagicAtemSwitcherTransportLayer(String switcherIp, BiConsumer<String, byte[]> dataFieldAvailableConsumer) throws Exception
    {
        this.udpInterface = new BlackmagicAtemSwitcherNetworkLayer(9910, switcherIp, this::rxPacketAvailable);
        this.dataFieldAvailableConsumer = dataFieldAvailableConsumer;

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
     * Gets called whenever we have received a new received packet from the switcher.
     * If there are any data fields inside the packet, notify the parent via the consumer.
     *
     * @param packet packet we just received from the switcher
     */
    private void rxPacketAvailable(BlackmagicAtemSwitcherPacket packet)
    {
        try
        {
            packet.getPayloadFields().forEach(this.dataFieldAvailableConsumer);
        } catch (Exception e)
        {
            logger.info("Cannot process packet payload", e);
        }
    }

    /**
     * Call this at the beginning. Sets the {@link #initializationComplete} flag.
     * Documentation: <a href="https://docs.openswitcher.org/udptransport.html">link</a>.
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
     * Sends a command, and ensures the switcher has acknowledged the packet.
     *
     * @param cmd  command mnemonic to send
     * @param data data associated with this command
     *
     * @throws Exception communication error
     */
    protected void sendCommand(String cmd, byte[] data) throws Exception
    {
        // create the outgoing command packet
        final Map<String, byte[]> map = new HashMap<>();
        map.put(cmd, data);
        final int localSeqNum = ++this.localSequenceNumber;
        final BlackmagicAtemSwitcherPacket packet = new BlackmagicAtemSwitcherPacket(true, false, false, false, false,
                this.sessionID, 0, 0, localSeqNum, map, null);

        // send the packet
        logger.info("Sending command '{}', data={}", cmd, DatatypeConverter.printHexBinary(data));
        this.udpInterface.getReceivedPacketQueue().clear();
        this.udpInterface.sendBlocking(packet);

        // receive the ACK for the packet
        final BlackmagicAtemSwitcherPacket rx = this.udpInterface.getReceivedPacketQueue().poll(3, TimeUnit.SECONDS);
        if (rx == null)
        {
            throw new Exception("Never received a response for the command....");
        } else if (!rx.isFlag4ACK() || rx.getAcknowledgementNumber() != localSeqNum)
        {
            throw new Exception("The received packet from the switcher did not acknowledge the command " + cmd);
        } else
        {
            logger.info("Received command response: {}", rx);
        }
    }
}
