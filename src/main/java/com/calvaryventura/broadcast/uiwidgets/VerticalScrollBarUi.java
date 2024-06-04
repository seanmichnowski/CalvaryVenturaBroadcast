package com.calvaryventura.broadcast.uiwidgets;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.Objects;

/**
 * Simple class for custom decorating a vertical scroll bar.
 */
public class VerticalScrollBarUi
{
    // load the up and down arrow images from disk on program startup
    private static final Image UP_ARROW_IMAGE = new ImageIcon(Objects.requireNonNull(VerticalScrollBarUi.class.getResource("/icons/green_up_arrow_16x16.png"))).getImage();
    private static final Image DOWN_ARROW_IMAGE = new ImageIcon(Objects.requireNonNull(VerticalScrollBarUi.class.getResource("/icons/green_down_arrow_16x16.png"))).getImage();
    private static final int ARROW_WIDTH_PIX = DOWN_ARROW_IMAGE.getWidth(null);
    private static final int ARROW_HEIGHT_PIX = DOWN_ARROW_IMAGE.getHeight(null);

    /**
     * @param scrollPane specify the scroll pane for which we
     *                   want to create a custom vertical scroll bar
     */
    public static void setSc(JScrollPane scrollPane)
    {
        scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI()
        {
            @Override
            protected JButton createDecreaseButton(int orientation)
            {
                return new JButton()
                {
                    @Override
                    public Dimension getPreferredSize()
                    {
                        return new Dimension();
                    }
                };
            }

            @Override
            protected JButton createIncreaseButton(int orientation)
            {
                return new JButton()
                {
                    @Override
                    public Dimension getPreferredSize()
                    {
                        return new Dimension();
                    }
                };
            }

            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds)
            {
                g.setColor(Color.BLACK);
                g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
            }

            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle r)
            {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                Color color;
                JScrollBar sb = (JScrollBar) c;
                if (!sb.isEnabled() || r.width > r.height)
                {
                    return;
                } else if (super.isDragging)
                {
                    color = new Color(200, 200, 100); // dark yellow
                } else
                {
                    color = new Color(255, 255, 100); // light yellow
                }

                g2.setPaint(color);
                g2.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
                g2.setPaint(Color.WHITE);
                g2.drawRoundRect(r.x, r.y, r.width, r.height, 10, 10);

                // draw little green up and down arrows at the top and bottom of the thumb
                final int arrowXPix = r.width / 2 - ARROW_WIDTH_PIX / 2;
                g2.drawImage(UP_ARROW_IMAGE, arrowXPix, r.y, null);
                g2.drawImage(DOWN_ARROW_IMAGE, arrowXPix, r.y + r.height - ARROW_HEIGHT_PIX, null);
                g2.dispose();
            }

            @Override
            protected void setThumbBounds(int x, int y, int width, int height)
            {
                super.setThumbBounds(x, y, width, height);
                super.scrollbar.repaint();
            }
        });
    }
}
