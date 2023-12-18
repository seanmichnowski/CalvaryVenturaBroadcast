package com.calvaryventura.broadcast.uiwidgets;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.plaf.basic.BasicLabelUI;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

/**
 * Rotates a JLabel text vertically.
 * To use 'label.setUI(new VerticalLabelUI(boolean))'.
 */
public class VerticalLabelUI extends BasicLabelUI
{
    static
    {
        labelUI = new VerticalLabelUI(false);
    }

    private static final Rectangle paintIconR = new Rectangle();
    private static final Rectangle paintTextR = new Rectangle();
    private static final Rectangle paintViewR = new Rectangle();
    private static Insets paintViewInsets = new Insets(0, 0, 0, 0);
    protected boolean clockwise;

    /**
     * @param clockwise indication if we should draw the label rotated clockwise
     */
    public VerticalLabelUI(boolean clockwise)
    {
        super();
        this.clockwise = clockwise;
    }

    /**
     * Reverses the height/width for the modified preferred size
     *
     * {@inheritDoc}
     */
    @Override
    public Dimension getPreferredSize(JComponent c)
    {
        final Dimension dim = super.getPreferredSize(c);
        return new Dimension((int) dim.getHeight(), (int) dim.getWidth());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void paint(Graphics g, JComponent c)
    {
        final JLabel label = (JLabel) c;
        final String text = label.getText();
        final Icon icon = (label.isEnabled()) ? label.getIcon() : label.getDisabledIcon();

        if ((icon == null) && (text == null))
        {
            return;
        }

        final FontMetrics fm = g.getFontMetrics();
        paintViewInsets = c.getInsets(paintViewInsets);

        paintViewR.x = paintViewInsets.left;
        paintViewR.y = paintViewInsets.top;

        // Use inverted height & width
        paintViewR.height = c.getWidth() - (paintViewInsets.left + paintViewInsets.right);
        paintViewR.width = c.getHeight() - (paintViewInsets.top + paintViewInsets.bottom);

        paintIconR.x = paintIconR.y = paintIconR.width = paintIconR.height = 0;
        paintTextR.x = paintTextR.y = paintTextR.width = paintTextR.height = 0;

        final String clippedText = layoutCL(label, fm, text, icon, paintViewR, paintIconR, paintTextR);

        final Graphics2D g2 = (Graphics2D) g;
        final AffineTransform tr = g2.getTransform();
        if (clockwise)
        {
            g2.rotate(Math.PI / 2);
            g2.translate(0, -c.getWidth());
        } else
        {
            g2.rotate(-Math.PI / 2);
            g2.translate(-c.getHeight(), 0);
        }

        if (icon != null)
        {
            icon.paintIcon(c, g, paintIconR.x, paintIconR.y);
        }

        if (text != null)
        {
            int textX = paintTextR.x;
            int textY = paintTextR.y + fm.getAscent();

            if (label.isEnabled())
            {
                paintEnabledText(label, g, clippedText, textX, textY);
            } else
            {
                paintDisabledText(label, g, clippedText, textX, textY);
            }
        }
        g2.setTransform(tr);
    }
}
