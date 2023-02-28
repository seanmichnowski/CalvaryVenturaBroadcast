package com.calvaryventura.broadcast.ptz.commander;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Simple mechanism for sending HTTP GET requests with a ptz control address
 * and ptz camera control commands built in. Use {@link #sendHttpGetCommand(String)}
 * as the main entry point for sending all GET commands to the camera.
 */
public class PtzCameraHttpInterface
{
    // camera connection
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin";
    private static final int TIMEOUT_MS = 50;

    // HTTP vars
    private final DefaultHttpClient httpClient;
    private final HttpHost host;
    private String lastCommand = "";
    private final JLabel connectionStatusLabel;
    private int commandCounter;

    /**
     * Creates the HTTP client.
     *
     * @param ipAddress             address of the camera
     * @param connectionStatusLabel label to update with status of the connection
     */
    public PtzCameraHttpInterface(String ipAddress, JLabel connectionStatusLabel)
    {
        this.connectionStatusLabel = connectionStatusLabel;
        this.host = new HttpHost(ipAddress, 80, "http");
        final Credentials credentials = new UsernamePasswordCredentials(USERNAME, PASSWORD);
        this.httpClient = new DefaultHttpClient();
        this.httpClient.getCredentialsProvider().setCredentials(new AuthScope(host.getHostName(), host.getPort()), credentials);

        // set a timeout of 100ms for sending commands, since the UI is held up during command send,
        // 100ms is generally still able to produce user motion and feedback on the UI controls
        HttpParams httpParams = httpClient.getParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, TIMEOUT_MS);
        HttpConnectionParams.setSoTimeout(httpParams, TIMEOUT_MS);
    }

    /**
     * @param command this is the command portion of the HTTP GET request, for example, 'ptzcmd&right&10&10'
     * @return indication of command success (not guaranteed that the camera actually moved though)
     */
    public boolean sendHttpGetCommand(String command)
    {
        if (this.lastCommand.equals(command))
        {
            return true;
        }
        this.lastCommand = command;

        // send the command
        try
        {
            // send command
            final HttpGet httpPostRequest = new HttpGet("/cgi-bin/ptzctrl.cgi?" + command.trim());
            final HttpResponse response = httpClient.execute(host, httpPostRequest);
            EntityUtils.consumeQuietly(response.getEntity());

            // handle response, 200 is the OK return code
            if (response.getStatusLine().getStatusCode() != 200)
            {
                throw new RuntimeException("Failed, error code: " + response);
            }

            // increment the status label
            this.connectionStatusLabel.setForeground(Color.WHITE);
            this.connectionStatusLabel.setText("Command counter: " + ++this.commandCounter);
            return true;
        } catch (Exception e)
        {
            this.connectionStatusLabel.setText(e.getMessage());
            this.connectionStatusLabel.setForeground(Color.RED.darker());
            return false;
        }
    }
}
