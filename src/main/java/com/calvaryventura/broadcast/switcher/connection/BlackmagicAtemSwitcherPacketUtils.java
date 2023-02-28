package com.calvaryventura.broadcast.switcher.connection;

/**
 * IO packet utils.
 */
public class BlackmagicAtemSwitcherPacketUtils
{
    protected static int word(byte b0, byte b1)
    {
        return ((b0 & 0xFF) << 8) | (b1 & 0xFF);
    }

    protected static byte highByte(int word)
    {
        return (byte) (((word & 0xFFFF) >> 8) & 0xFF);
    }

    protected static byte lowByte(int word)
    {
        return (byte) (word & 0xFF);
    }
}
