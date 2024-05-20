package com.calvaryventura.broadcast.uiwidgets;

import javax.swing.JSplitPane;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * Simple utility for configuring a {@link JSplitPane} bar
 * to something that's not the usual boring one.
 */
public class SplitPaneBarColorizer
{
    private static final int VERTICAL_GAP_PIX_AROUND_SPLIT_PANE_BAR = 5;

    /**
     * Simple utility for showing a striped split pane bar instead of the usual boring one.
     *
     * @param splitPane split pane to update
     * @param color     color to incorporate into the new bar
     */
    public static void setSplitPaneBarStriped(JSplitPane splitPane, Color color)
    {
        splitPane.setUI(new BasicSplitPaneUI()
        {
            @Override
            public BasicSplitPaneDivider createDefaultDivider()
            {
                return new BasicSplitPaneDivider(this)
                {
                    // no border around the divider
                    public void setBorder(Border b)
                    {
                    }

                    @Override
                    public void paint(Graphics g)
                    {
                        ((Graphics2D) g).setPaint(new GradientPaint(5, 15, color.darker().darker(), 10, 2, Color.black, true));
                        g.fillRect(0, VERTICAL_GAP_PIX_AROUND_SPLIT_PANE_BAR, getSize().width, getSize().height - 2 * VERTICAL_GAP_PIX_AROUND_SPLIT_PANE_BAR);
                        super.paint(g);
                    }
                };
            }
        });
    }
}
