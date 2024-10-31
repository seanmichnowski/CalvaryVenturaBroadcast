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
    private static Color BORDER_COLOR = Color.MAGENTA;

    /**
     * @param color color to show for ALL borders created using {@link #createTitledBorder(String)}
     */
    public static void setBorderColor(Color color)
    {
        BORDER_COLOR = color;
    }

    /**
     * Simple utility for creating a custom border so that all panels can look uniform.
     *
     * @param title description of the border we are to create
     */
    public static Border createTitledBorder(String title)
    {
        return new CompoundBorder(
                new TitledBorder(new LineBorder(BORDER_COLOR, 3, true), title, TitledBorder.LEADING, TitledBorder.DEFAULT_POSITION,
                        new Font("Ubuntu", Font.BOLD, 20), BORDER_COLOR),
                new EmptyBorder(0, 5, 5, 5));
    }
}
