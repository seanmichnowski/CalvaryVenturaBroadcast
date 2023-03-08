package com.calvaryventura.broadcast.switcher.control.test;

import com.calvaryventura.broadcast.switcher.control.BlackmagicAtemSwitcherUserLayer;
import org.apache.log4j.BasicConfigurator;

public class TestSwitcherControllerSean
{
    public TestSwitcherControllerSean() throws Exception
    {
        final BlackmagicAtemSwitcherUserLayer broadcastSwitcherUserLayer = new BlackmagicAtemSwitcherUserLayer();
        broadcastSwitcherUserLayer.initialize("192.168.86.222");
        Thread.sleep(1000);
        broadcastSwitcherUserLayer.setProgramVideo(4);
        //Thread.sleep(1000);
        //broadcastSwitcherUserLayer.setPreviewVideo(7);
    }

    public static void main(String[] args) throws Exception
    {
        BasicConfigurator.configure();
        new TestSwitcherControllerSean();
    }
}
