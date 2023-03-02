package com.calvaryventura.broadcast.switcher.control;

/**
 * IO packet utils.
 */
public class BlackmagicAtemSwitcherPacketUtils
{
    /**
     * Combines two bytes into a 16 bit word.
     *
     * @param b0 upper byte
     * @param b1 lower byte
     *
     * @return word concatenated from high and low bytes
     */
    protected static int word(byte b0, byte b1)
    {
        return ((b0 & 0xFF) << 8) | (b1 & 0xFF);
    }

    /**
     * Pulls the upper/high byte from a 16 bit word.
     *
     * @param word 16 bit word to pull from
     *
     * @return upper byte (MSB)
     */
    protected static byte highByte(int word)
    {
        return (byte) (((word & 0xFFFF) >> 8) & 0xFF);
    }

    /**
     * Pulls the lower byte from a 16 bit word.
     *
     * @param word 16 bit word to pull from
     *
     * @return lower byte (LSB)
     */
    protected static byte lowByte(int word)
    {
        return (byte) (word & 0xFF);
    }
}
