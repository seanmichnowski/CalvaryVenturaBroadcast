package com.calvaryventura.broadcast.switcher.control;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

/**
 * Simple implementation of a native Java UDP transmitter/receiver.
 */
public class BlackmagicAtemSwitcherNetworkLayer
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final BlockingQueue<BlackmagicAtemSwitcherPacket> receivedPacketQueue = new LinkedBlockingQueue<>();
    private final ExecutorService receiveExecutor = Executors.newSingleThreadExecutor();
    private final DatagramSocket txRxSocket;
    private final DatagramPacket transmitPacket;
    private final Consumer<BlackmagicAtemSwitcherPacket> rxPacketConsumer;

    /**
     * Sets up the UDP socket for transmit and receive.
     * Starts the rx thread to listen to new UDP packets.
     *
     * @param transmitDestPort the port to send data out on
     * @param transmitDestIP the destination IP address to send to
     * @param rxPacketConsumer notifies the parent when a new packet has been received
     *
     * @throws Exception on network error
     */
    public BlackmagicAtemSwitcherNetworkLayer(int transmitDestPort, String transmitDestIP, Consumer<BlackmagicAtemSwitcherPacket> rxPacketConsumer) throws Exception
    {
        // create the socket, bind to any available local port
        this.txRxSocket = new DatagramSocket();
        this.txRxSocket.setSoTimeout(0); // infinite

        // create the outgoing packet, set or the destination IP and port
        final InetAddress transmitAddress = InetAddress.getByName(transmitDestIP);
        this.transmitPacket = new DatagramPacket(new byte[0], 0, transmitAddress, transmitDestPort);

        // start the background receiver thread
        logger.info("Starting UDP reception bound to local port: {}", this.txRxSocket.getLocalPort());
        this.rxPacketConsumer = rxPacketConsumer;
        this.doUdpReceiveThread();
    }

    /**
     * Called once the client gets started and connected to the server.
     */
    private void doUdpReceiveThread()
    {
        this.receiveExecutor.submit(() ->
        {
            // new packet for receiving
            final byte[] receiveData = new byte[1 << 16];
            final DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);

            // breaks out of the loop if user requests to close
            while (!Thread.currentThread().isInterrupted())
            {
                // receive the packet, break on error
                try
                {
                    // blocks waiting on a new RX packet
                    this.txRxSocket.receive(packet);

                    // parse packet
                    final BlackmagicAtemSwitcherPacket p = new BlackmagicAtemSwitcherPacket(packet.getData(), packet.getLength());
                    this.receivedPacketQueue.put(p);
                    this.rxPacketConsumer.accept(p);
                } catch (SocketException e)
                {
                    logger.info("HTL receive UDP socket closed");
                    txRxSocket.close();
                } catch (Exception e)
                {
                    logger.error("Error on UDP receive thread", e);
                    txRxSocket.close();
                    break;
                }
            }
        });
    }

    /**
     * TODO
     * @return
     */
    public BlockingQueue<BlackmagicAtemSwitcherPacket> getReceivedPacketQueue()
    {
        return this.receivedPacketQueue;
    }

    /**
     * @param packet source of data we will send
     * @throws Exception on error sending, buffer too large, etc.
     */
    public void sendBlocking(BlackmagicAtemSwitcherPacket packet) throws Exception
    {
        this.transmitPacket.setData(packet.getFullPacketBytes());
        this.txRxSocket.send(this.transmitPacket); // TODO https://www.baeldung.com/java-nio-datagramchannel start here
    }
}
