package com.calvaryventura.broadcast.uiwidgets;

import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.Color;
import java.awt.Font;

/**
 * Simple utility for creating a custom border so that all panels can look uniform.
 */
public class TitledBorderCreator
{
    /**
     * Simple utility for creating a custom border so that all panels can look uniform.
     *
     * @param title description of the border we are to create
     */
    public static Border createTitledBorder(String title)
    {
        return new CompoundBorder(
                new TitledBorder(new LineBorder(Color.magenta, 3, true), title, TitledBorder.LEADING, TitledBorder.DEFAULT_POSITION,
                        new Font("Ubuntu", Font.BOLD, 20), Color.magenta),
                new EmptyBorder(0, 5, 5, 5));
    }
}
