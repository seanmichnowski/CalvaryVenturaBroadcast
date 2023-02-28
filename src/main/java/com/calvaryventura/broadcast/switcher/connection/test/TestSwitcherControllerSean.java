package com.calvaryventura.broadcast.switcher.connection.test;

import com.calvaryventura.broadcast.switcher.connection.BlackmagicAtemSwitcherUserLayer;
import org.apache.log4j.BasicConfigurator;

public class TestSwitcherControllerSean
{
    public TestSwitcherControllerSean() throws Exception
    {
        final BlackmagicAtemSwitcherUserLayer broadcastSwitcherUserLayer = new BlackmagicAtemSwitcherUserLayer("192.168.86.222");
        Thread.sleep(1000);
        broadcastSwitcherUserLayer.setProgramVideo(3);
    }

    public static void main(String[] args) throws Exception
    {
        BasicConfigurator.configure();
        new TestSwitcherControllerSean();
    }
}
