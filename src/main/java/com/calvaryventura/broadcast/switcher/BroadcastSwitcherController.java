package com.calvaryventura.broadcast.switcher;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Simple mechanism for sending HTTP GET requests with a button press
 * command using {@link #sendHttpGetCommand(int)} as the main entry point.
 */
public class BroadcastSwitcherController
{
    private final DefaultHttpClient httpClient;
    private final HttpHost host;
    private final JLabel connectionStatusLabel;
    private int commandCounter;

    /**
     * Creates the HTTP client.
     *
     * @param ipAddress             address of the camera
     * @param connectionStatusLabel label to update with status of the connection
     */
    public BroadcastSwitcherController(String ipAddress, JLabel connectionStatusLabel)
    {
        this.connectionStatusLabel = connectionStatusLabel;
        this.host = new HttpHost(ipAddress, 8888, "http");
        this.httpClient = new DefaultHttpClient();

        // set a timeout of 100ms for sending commands, since the UI is held up during command send,
        // 100ms is generally still able to produce user motion and feedback on the UI controls
        HttpParams httpParams = httpClient.getParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 100);
        HttpConnectionParams.setSoTimeout(httpParams, 100);
    }

    /**
     * @param buttonNumber number of the button (on bank #1) that we want to press
     * @return indication of command success (not guaranteed that the camera actually moved though)
     */
    public boolean sendHttpGetCommand(int buttonNumber)
    {
        // send the command
        try
        {
            // send command
            System.out.printf("Sending blackmagic command button number: %d\n", buttonNumber);
            final HttpGet httpPostRequest = new HttpGet("/press/bank/1/" + buttonNumber);
            final HttpResponse response = httpClient.execute(host, httpPostRequest);
            EntityUtils.consumeQuietly(response.getEntity());

            // handle response, 200 is the OK return code
            if (response.getStatusLine().getStatusCode() != 200)
            {
                throw new RuntimeException("Failed, error code: " + response);
            }

            // set status label green
            this.connectionStatusLabel.setForeground(Color.WHITE);
            this.connectionStatusLabel.setText("Command counter: " + ++this.commandCounter);
            return true;
        } catch (Exception e)
        {
            this.connectionStatusLabel.setText(e.getMessage());
            this.connectionStatusLabel.setForeground(Color.RED.darker());
            e.printStackTrace();
            return false;
        }
    }
}
