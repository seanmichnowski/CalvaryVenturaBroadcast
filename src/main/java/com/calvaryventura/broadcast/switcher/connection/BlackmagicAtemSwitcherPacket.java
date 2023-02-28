package com.calvaryventura.broadcast.switcher.connection;

import javax.xml.bind.DatatypeConverter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * See website <a href="https://docs.openswitcher.org/udptransport.html">link</a>
 * for a description of the header flags, header fields, and packet structure.
 */
public class BlackmagicAtemSwitcherPacket
{
    private static final int HEADER_LEN = 12;
    private static final int HEADER_FLAGS_RELIABLE = 0x08; // bit 0 (msb)
    private static final int HEADER_FLAGS_SYN = 0x10; // bit 1
    private static final int HEADER_FLAGS_RETRANSMISSION = 0x20; // bit 2
    private static final int HEADER_FLAGS_REQUEST_RETRANSMISSION = 0x40; // bit 3
    private static final int HEADER_FLAGS_ACK = 0x80; // bit 4

    // header flags
    private final boolean flag0Reliable;
    private final boolean flag1SYN;
    private final boolean flag2Retransmission;
    private final boolean flag3RequestRetransmission;
    private final boolean flag4ACK;

    // header fields
    private final int packetLength;
    private final int sessionId;
    private final int acknowledgementNumber;
    private final int remoteSequenceNumber;
    private final int localSequenceNumber;

    // byte fields
    private final byte[] rawPayloadBytes;
    private final byte[] fullPacketBytes;

    /**
     * Private constructor, sets all header fields.
     *
     * @param flag0Reliable              indication
     * @param flag1SYN                   indication
     * @param flag2Retransmission        indication
     * @param flag3RequestRetransmission indication
     * @param flag4ACK                   indication
     * @param sessionId                  id provided by the switcher on first connection
     * @param acknowledgementNumber      count..
     * @param remoteSequenceNumber       count..
     * @param localSequenceNumber        count..
     * @param payloadFields              payload entry(s) within this packet
     * @param rawPayloadBytes            raw bytes used during initialization sequence
     */
    public BlackmagicAtemSwitcherPacket(boolean flag0Reliable, boolean flag1SYN, boolean flag2Retransmission,
                                         boolean flag3RequestRetransmission, boolean flag4ACK,
                                         int sessionId, int acknowledgementNumber, int remoteSequenceNumber,
                                         int localSequenceNumber, Map<String, byte[]> payloadFields, byte[] rawPayloadBytes)
    {
        this.flag0Reliable = flag0Reliable;
        this.flag1SYN = flag1SYN;
        this.flag2Retransmission = flag2Retransmission;
        this.flag3RequestRetransmission = flag3RequestRetransmission;
        this.flag4ACK = flag4ACK;
        this.sessionId = sessionId;
        this.acknowledgementNumber = acknowledgementNumber;
        this.remoteSequenceNumber = remoteSequenceNumber;
        this.localSequenceNumber = localSequenceNumber;

        // there are two types of acceptable payloads
        if (payloadFields == null)
        {
            this.packetLength = HEADER_LEN + rawPayloadBytes.length;
            this.rawPayloadBytes = rawPayloadBytes;
        } else
        {
            // compute length of payload, comprised of repeating fields, each field has 8 bytes fixed and N bytes data.
            final AtomicInteger payloadLen = new AtomicInteger();
            payloadFields.forEach((commandMnemonic, fieldBytes) -> payloadLen.addAndGet((8 + fieldBytes.length)));
            this.packetLength = HEADER_LEN + payloadLen.get();
            this.rawPayloadBytes = new byte[payloadLen.get()];
        }

        // set the header flags
        this.fullPacketBytes = new byte[this.packetLength];
        fullPacketBytes[0] = this.flag0Reliable ? (byte) (fullPacketBytes[0] | HEADER_FLAGS_RELIABLE) : fullPacketBytes[0];
        fullPacketBytes[0] = this.flag1SYN ? (byte) (fullPacketBytes[0] | HEADER_FLAGS_SYN) : fullPacketBytes[0];
        fullPacketBytes[0] = this.flag2Retransmission ? (byte) (fullPacketBytes[0] | HEADER_FLAGS_RETRANSMISSION) : fullPacketBytes[0];
        fullPacketBytes[0] = this.flag3RequestRetransmission ? (byte) (fullPacketBytes[0] | HEADER_FLAGS_REQUEST_RETRANSMISSION) : fullPacketBytes[0];
        fullPacketBytes[0] = this.flag4ACK ? (byte) (fullPacketBytes[0] | HEADER_FLAGS_ACK) : fullPacketBytes[0];

        // set header fields
        fullPacketBytes[0] |= (this.packetLength >> 8) & 0x03;
        fullPacketBytes[1] = (byte) (this.packetLength & 0xFF);
        fullPacketBytes[2] = (byte) ((this.sessionId >> 8) & 0xFF);
        fullPacketBytes[3] = (byte) (this.sessionId & 0xFF);
        fullPacketBytes[4] = (byte) ((this.acknowledgementNumber >> 8) & 0xFF);
        fullPacketBytes[5] = (byte) (this.acknowledgementNumber & 0xFF);
        fullPacketBytes[8] = (byte) ((this.remoteSequenceNumber >> 8) & 0xFF);
        fullPacketBytes[9] = (byte) (this.remoteSequenceNumber & 0xFF);
        fullPacketBytes[10] = (byte) ((this.localSequenceNumber >> 8) & 0xFF);
        fullPacketBytes[11] = (byte) (this.localSequenceNumber & 0xFF);

        // copy payload into outgoing buffer
        if (payloadFields == null)
        {
            // copy payload bytes into outgoing buffer
            System.arraycopy(this.rawPayloadBytes, 0, fullPacketBytes, HEADER_LEN, this.rawPayloadBytes.length);
        } else
        {
            // payload fields get put into the outgoing buffer
            final AtomicInteger txBufferOffset = new AtomicInteger(HEADER_LEN);
            payloadFields.forEach((commandMnemonic, fieldBytes) -> {
                // payload's length field includes the 8 fixed bytes
                final int payloadFullFieldLen = 8 + fieldBytes.length;

                // bytes 0 and 1 of the payload field entry are the payload field's length
                fullPacketBytes[txBufferOffset.get()] = (byte) ((payloadFullFieldLen >> 8) & 0xFF);
                fullPacketBytes[txBufferOffset.get() + 1] = (byte) (payloadFullFieldLen & 0xFF);

                // bytes 4-7 are the 4 letter string mnemonic
                System.arraycopy(commandMnemonic.getBytes(), 0, fullPacketBytes, txBufferOffset.get() + 4, 4);

                // next is the variable length data portion of this payload field
                System.arraycopy(fieldBytes, 0, fullPacketBytes, txBufferOffset.get() + 8, fieldBytes.length);

                // increment the buffer offset for next payload field entry
                txBufferOffset.addAndGet(payloadFullFieldLen);
            });
        }
    }

    public boolean isFlag0Reliable()
    {
        return this.flag0Reliable;
    }

    public boolean isFlag1SYN()
    {
        return this.flag1SYN;
    }

    public boolean isFlag2Retransmission()
    {
        return this.flag2Retransmission;
    }

    public boolean isFlag3RequestRetransmission()
    {
        return this.flag3RequestRetransmission;
    }

    public boolean isFlag4ACK()
    {
        return this.flag4ACK;
    }

    public int getPacketLength()
    {
        return this.packetLength;
    }

    public int getSessionId()
    {
        return this.sessionId;
    }

    public int getAcknowledgementNumber()
    {
        return this.acknowledgementNumber;
    }

    public int getRemoteSequenceNumber()
    {
        return this.remoteSequenceNumber;
    }

    public int getLocalSequenceNumber()
    {
        return this.localSequenceNumber;
    }

    public byte[] getRawPayloadBytes()
    {
        return this.rawPayloadBytes;
    }

    /**
     * Converts all the header and payload data into an outgoing packet.
     *
     * @return packet that is ready to send to the blackmagic device
     */
    public byte[] getFullPacketBytes()
    {
        return this.fullPacketBytes;
    }

    /**
     * Pulls from the local {@link #rawPayloadBytes} and parses into a map
     * of command mnemonics and command data sections.
     *
     * @return map of [commandMnemonic, dataBytes]
     *
     * @throws Exception on a parsing error or length field mismatch
     */
    public Map<String, byte[]> getPayloadFields() throws Exception
    {
        // parse payload fields
        int rxCounter = 0;
        final Map<String, byte[]> payloadFields = new HashMap<>();
        while (rxCounter < this.rawPayloadBytes.length)
        {
            // sanity check
            if (rxCounter + 8 > this.rawPayloadBytes.length)
            {
                throw new Exception("Not enough received bytes to read payload field's mnemonic");
            }

            // pull out the payload field length and mnemonic
            final int payloadFieldLen = BlackmagicAtemSwitcherPacketUtils.word(this.rawPayloadBytes[rxCounter], this.rawPayloadBytes[rxCounter + 1]);
            final String cmdMnemonic = new String(this.rawPayloadBytes, rxCounter + 4, 4);
            final int fieldDataLen = payloadFieldLen - 8; // field data starts after 8 bytes

            // sanity check
            if (rxCounter + 8 + fieldDataLen > this.packetLength)
            {
                throw new Exception("Not enough received bytes to read payload field's data for mnemonic: " + cmdMnemonic);
            }

            // pull out payload field's data
            final byte[] fieldData = new byte[fieldDataLen];
            System.arraycopy(this.rawPayloadBytes, rxCounter + 8, fieldData, 0, fieldDataLen);
            payloadFields.put(cmdMnemonic, fieldData);

            // increment counter to advance to the next payload field
            rxCounter += payloadFieldLen;
        }
        return payloadFields;
    }

    /**
     * Creates a packet and parses all fields from incoming bytes.
     *
     * @param rx received bytes from the blackmagic switcher
     * @param len length of usable bytes in the 'rx' buffer
     *
     * @throws Exception parsing error or insufficient number of bytes
     */
    public BlackmagicAtemSwitcherPacket(byte[] rx, int len) throws Exception
    {
        // sanity check on parsing the header
        if (len < HEADER_LEN)
        {
            throw new Exception("Received packet has insufficient header bytes");
        }

        // parse header
        this.flag0Reliable = (rx[0] & HEADER_FLAGS_RELIABLE) == HEADER_FLAGS_RELIABLE;
        this.flag1SYN = (rx[0] & HEADER_FLAGS_SYN) == HEADER_FLAGS_SYN;
        this.flag2Retransmission = (rx[0] & HEADER_FLAGS_RETRANSMISSION) == HEADER_FLAGS_RETRANSMISSION;
        this.flag3RequestRetransmission = (rx[0] & HEADER_FLAGS_REQUEST_RETRANSMISSION) == HEADER_FLAGS_REQUEST_RETRANSMISSION;
        this.flag4ACK = (rx[0] & HEADER_FLAGS_ACK) == HEADER_FLAGS_ACK;
        this.packetLength = BlackmagicAtemSwitcherPacketUtils.word((byte) (rx[0] & 0b00000111), rx[1]);
        this.sessionId = BlackmagicAtemSwitcherPacketUtils.word(rx[2], rx[3]);
        this.acknowledgementNumber = BlackmagicAtemSwitcherPacketUtils.word(rx[4], rx[5]);
        this.remoteSequenceNumber = BlackmagicAtemSwitcherPacketUtils.word(rx[8], rx[9]);
        this.localSequenceNumber = BlackmagicAtemSwitcherPacketUtils.word(rx[10], rx[11]);

        // sanity check on parsing the header
        if (this.packetLength != len)
        {
            throw new Exception("Received packet's indicated length does not match received packet length");
        }

        // pull out payload
        final int payloadLen = this.packetLength - HEADER_LEN;
        this.rawPayloadBytes = new byte[payloadLen];
        System.arraycopy(rx, HEADER_LEN, this.rawPayloadBytes, 0, payloadLen);

        // copy full message in case we need it later
        this.fullPacketBytes = new byte[len];
        System.arraycopy(rx, 0, this.fullPacketBytes, 0, len);
    }

    /**
     * @return all fields for this packet
     */
    @Override
    public String toString()
    {
        final String payloadStr = DatatypeConverter.printHexBinary(rawPayloadBytes);
        return "BlackmagicAtemSwitcherPacket{" +
                "Reliable=" + flag0Reliable +
                ", SYN=" + flag1SYN +
                ", Retransmission=" + flag2Retransmission +
                ", RequestRetransmission=" + flag3RequestRetransmission +
                ", ACK=" + flag4ACK +
                ", packetLen=" + packetLength +
                ", sessionId=" + sessionId +
                ", acknowledgementNumber=" + acknowledgementNumber +
                ", remoteSeqNumber=" + remoteSequenceNumber +
                ", localSeqNumber=" + localSequenceNumber +
                ", payload=" + (payloadStr.length() > 100 ? payloadStr.substring(0, 100) + "..." : payloadStr) +
                '}';
    }
}
