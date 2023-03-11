package com.calvaryventura.broadcast.ptzcamera.control;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.lang.invoke.MethodHandles;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Native Java TCP client for sending/receiving serialized objects.
 * The object (base class) to send/receive is defined by the parameter "T".
 * <p>
 * NOTE: for Java object serialization to/from a stream, do not use the method
 * "readObject()" or "writeObject()", use "readUnshared()" and "writeUnshared()"
 * because this sends a completely new and unique object every time.
 * <p>
 * This client/server connection DOES NOT support asynchronous transfers.
 *   - The client initiates every request
 *   - Every client request results in the server responding with data or client timeout
 *   - The send/receive methods in this class are synchronized to prevent simultaneous requests
 *
 * @since 0.1.0
 */
public class PtzCameraViscaTcpNetworkInterface
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final ScheduledExecutorService connectionThread = Executors.newSingleThreadScheduledExecutor();
    private final List<Consumer<Boolean>> connectionStateListeners = new ArrayList<>();
    private final BlockingQueue<byte[]> rxProcessingQueue = new LinkedBlockingQueue<>();
    private final String displayName;
    private final InetSocketAddress serverSocketAddress;
    private ScheduledFuture<?> connectionThreadFuture;
    private Socket clientSocket = new Socket();
    private DataOutputStream outToServer = null;
    private DataInputStream inFromServer = null;
    private Boolean lastConnectionState = null;
    private final Object writeLock = new Object();

    /**
     * @param displayName human-readable name designator of this camera
     * @param ipAddress address of the PTZ camera
     * @param port port of the PTZ camera's VISCA interface
     */
    public PtzCameraViscaTcpNetworkInterface(String displayName, String ipAddress, int port)
    {
        this.displayName = displayName;
        this.serverSocketAddress = new InetSocketAddress(ipAddress, port);
        this.start();
    }

    /**
     * @param connectionStateListener notifies parent of connection/disconnection change
     */
    public void addConnectionListener(Consumer<Boolean> connectionStateListener)
    {
        this.connectionStateListeners.add(connectionStateListener);
    }

    /**
     * Attempts to connect to the TCP server. Upon connection this fires
     * a callback using the connection state listener consumer.
     * This design will attempt to continually connect until {@link #stop()} is called.
     */
    private void start()
    {
        // stop any previous connection if this method gets called again
        if (this.connectionThreadFuture != null && !this.connectionThreadFuture.isDone())
        {
            this.stop();
        }

        // status printout
        logger.info("Attempting connection to PTZ camera '{}' at {}...", this.displayName, this.serverSocketAddress);

        // start a background thread to create and manage the connection
        this.connectionThreadFuture = this.connectionThread.scheduleWithFixedDelay(() -> {
            try
            {
                if (this.clientSocket != null && (this.clientSocket.isConnected() || !this.clientSocket.isClosed()))
                {
                    this.clientSocket.close();
                }

                this.clientSocket = new Socket();
                this.clientSocket.connect(this.serverSocketAddress, 1000);
                this.outToServer = new DataOutputStream(this.clientSocket.getOutputStream());
                this.inFromServer = new DataInputStream(this.clientSocket.getInputStream());
                this.clientSocket.setSoTimeout(0); // infinite timeout on socket read methods
                logger.info("{} is connected!", this.displayName);
                notifyConnectionChange(true);

                // read the latest incoming object from the server
                final byte[] rxBuf = new byte[1 << 16];
                while (!this.connectionThreadFuture.isCancelled())
                {
                    final int nRead = this.inFromServer.read(rxBuf);
                    final byte[] rx = new byte[nRead];
                    System.arraycopy(rxBuf, 0, rx, 0, nRead);
                    logger.info("Received {} bytes from PTZ camera '{}'. Message: {}", nRead, this.displayName, DatatypeConverter.printHexBinary(rx));
                    this.rxProcessingQueue.put(rx);
                }
            } catch (SocketTimeoutException ignored)
            {
            } catch (SocketException e)
            {
                // socket was closed, this periodically happens when the network is down, so don't print a log message
                notifyConnectionChange(false);
            } catch (EOFException e)
            {
                logger.error("The {} connection forcibly closed. Please try restarting the server.", this.displayName);
                notifyConnectionChange(false);
            } catch (Throwable e)
            {
                logger.error("{} error. Resetting connection...", this.displayName, e);
                notifyConnectionChange(false);
            }
        }, 0, 3, TimeUnit.SECONDS);
    }

    /**
     * Called to stop the client's TCP connection.
     */
    private void stop()
    {
        try
        {
            if (clientSocket != null)
            {
                clientSocket.close();
                clientSocket = null;
                logger.info("Closing the {} connection.", this.displayName);
            } else
            {
                logger.info("Unable to close {}, no connection active", this.displayName);
            }
            if (connectionThreadFuture != null && !connectionThreadFuture.isDone())
            {
                connectionThreadFuture.cancel(true);
                connectionThreadFuture = null;
            }
            this.notifyConnectionChange(false);
        } catch (Throwable e)
        {
            logger.error("{} cannot close connection", this.displayName, e);
        }
    }

    /**
     * Called upon starting/stopping the client's TCP connection.
     *
     * @param newConnectionState indication if we are connected/disconnected
     */
    private void notifyConnectionChange(boolean newConnectionState)
    {
        if (this.lastConnectionState == null || newConnectionState != this.lastConnectionState)
        {
            this.lastConnectionState = newConnectionState;
            this.connectionStateListeners.forEach(c -> Executors.newSingleThreadExecutor().execute(() -> c.accept(newConnectionState)));
        }
    }

    /**
     * Send a byte array to the connected server.
     * If we cannot send, then attempt to restart the connection.
     *
     * @param message the data array to send out
     *
     * @return indication the message was sent successfully
     */
    public boolean send(byte[] message)
    {
        synchronized (this.writeLock)
        {
            try
            {
                // sanity check
                if (this.outToServer == null)
                {
                    throw new Exception("Not connected to camera " + this.displayName);
                }

                // send the command
                logger.info("Sending PTZ camera '{}' the message {}", this.displayName, DatatypeConverter.printHexBinary(message));
                this.rxProcessingQueue.clear();
                this.outToServer.write(message);
                this.outToServer.flush();

                // confirm the ACK message and the COMMAND FINISHED message, example "0x9041FF" and "0x9051FF"
                final byte[] poll = this.rxProcessingQueue.poll(1, TimeUnit.SECONDS);
                if (poll == null || poll.length < 3)
                {
                    throw new Exception("Timeout on receiving ACK from " + this.displayName);
                } else
                {
                    if (poll.length >= 6) // the FINISHED command was sent in the same packet as the ACK
                    {
                        // example ACK is "0x9041FF" where the '1' is the socket address and could change
                        if (!(poll[0] == (byte) 0x90 && (poll[1] & 0xF0) == 0x40 && poll[2] == (byte) 0xFF))
                        {
                            throw new Exception("Did not receive ACK from camera");
                        }
                        // example command finished command is "0x9051FF" where the '1' is the socket address and could change
                        if (!(poll[3] == (byte) 0x90 && (poll[4] & 0xF0) == 0x50 && poll[5] == (byte) 0xFF))
                        {
                            throw new Exception("Did not receive COMMAND FINISHED from camera");
                        }
                    } else // look for the ACK in this smaller packet
                    {
                        // example ACK is "0x9041FF" where the '1' is the socket address and could change
                        if (!(poll[0] == (byte) 0x90 && (poll[1] & 0xF0) == 0x40 && poll[2] == (byte) 0xFF))
                        {
                            throw new Exception("Did not receive ACK from camera");
                        }
                        // now wait again for the command FINISHED packet to be sent in
                        // example command finished command is "0x9051FF" where the '1' is the socket address and could change
                        final byte[] poll1 = this.rxProcessingQueue.poll(3, TimeUnit.SECONDS);
                        if (poll1 == null || !(poll1[0] == (byte) 0x90 && (poll1[1] & 0xF0) == 0x50 && poll1[2] == (byte) 0xFF))
                        {
                            throw new Exception("Did not receive COMMAND FINISHED from camera");
                        }
                    }
                }
                return true;
            } catch (Exception e)
            {
                logger.error("Cannot send PTZ message to camera {}, message={}. Error={}. Restarting...",
                        this.displayName, DatatypeConverter.printHexBinary(message), e.getMessage());
                this.start();
                return false;
            }
        }
    }
}