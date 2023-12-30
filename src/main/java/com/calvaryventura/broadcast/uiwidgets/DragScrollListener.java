package com.calvaryventura.broadcast.uiwidgets;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;

/**
 * Listener to allow for iPhone like drag scrolling of a Component within a JScrollPane.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
public class DragScrollListener extends MouseAdapter
{
    //flags used to turn on/off draggable scrolling directions
    public static final int DRAGABLE_HORIZONTAL_SCROLL_BAR = 1;
    public static final int DRAGABLE_VERTICAL_SCROLL_BAR = 2;

    // flags used to drag/scroll based on mouse button
    public static final int RESPOND_TO_LEFT_MOUSE_BUTTON = 1;
    public static final int RESPOND_TO_MIDDLE_MOUSE_BUTTON = 2;
    public static final int RESPOND_TO_RIGHT_MOUSE_BUTTON = 4;

    // interval vars
    private int scrollingIntensity = 10;     // defines the intensity of automatic scrolling.
    private double damping = 0.05;           // value used to descrease scrolling intensity during animation
    private int animationSpeed = 20;         // indicates the number of milliseconds between animation updates.
    private javax.swing.Timer animationTimer = null;
    private long lastDragTime = 0;           // the time of the last mouse drag event
    private Point lastDragPoint = null;
    private double pixelsPerMSX;  // animation rates
    private double pixelsPerMSY;  // animation rates
    private final Component draggableComponent;  // the draggable component
    private JScrollPane scroller = null;         // the JScrollPane containing the component - programmatically determined
    private Cursor defaultCursor;                // the default cursor
    private boolean hideMyScrollBars = false;    // allows hiding of the internal scroll bars (can use external instead)
    private boolean allowMouseCursorChanged = true;
    private long lastMoveTimeMS = 0;

    // flag which defines the draggable scroll directions
    private int scrollBarMask = DRAGABLE_HORIZONTAL_SCROLL_BAR | DRAGABLE_VERTICAL_SCROLL_BAR;

    // flag which defines which mouse buttons we respond to
    private int mouseButtonMask = RESPOND_TO_LEFT_MOUSE_BUTTON | RESPOND_TO_MIDDLE_MOUSE_BUTTON | RESPOND_TO_RIGHT_MOUSE_BUTTON;

    // List of drag speeds used to calculate animation speed
    // Uses the Point2D class to represent speeds rather than locations
    private final java.util.List<Point2D.Double> dragSpeeds = new ArrayList<>();

    /**
     * See usage example at the bottom of this class, create a new instance of
     * this class, and pass the panel/component you want to scroll as the argument.
     *
     * NOTE: the component you want to scroll, must already be placed
     *       into an existing JScrollPane, then this class can be called
     *
     * @param c the component/panel/etc. that you want to scroll
     */
    public DragScrollListener(Component c)
    {
        draggableComponent = c;
        defaultCursor = draggableComponent.getCursor();
        draggableComponent.addPropertyChangeListener(e -> {
            setScroller();
            defaultCursor = draggableComponent.getCursor();
        });
        setScroller();

        // set up mouse listeners
        draggableComponent.addMouseListener(this);
        draggableComponent.addMouseMotionListener(this);
        this.setEnabled(true);
    }

    /**
     * @param en enables or disables letting the target panel/component be mouse-scrolled
     */
    public void setEnabled(boolean en)
    {
        if (en)
        {
            // detect if we already have registered mouse listeners in this class
            for (MouseListener ml : draggableComponent.getMouseListeners())
            {
                if (ml.equals(this))
                {
                    return;
                }
            }

            // getting here, we do not have this class listening, so set that up!
            draggableComponent.addMouseListener(this);
            draggableComponent.addMouseMotionListener(this);

            // reset the last starting drag point
            this.lastDragPoint = null;
        } else
        {
            draggableComponent.removeMouseListener(this);
            draggableComponent.removeMouseMotionListener(this);
        }
    }

    /**
     * Locates the scroll pane and sets up the horizontal/vertical scroll bars
     */
    private void setScroller()
    {
        scroller = getParentScroller(draggableComponent);

        if (hideMyScrollBars)
        {
            scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        } else
        {
            scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        }
    }

    /**
     * @param doHide allows hiding of the internal scroll bars (can use external instead)
     */
    public void hideScrollBars(boolean doHide)
    {
        this.hideMyScrollBars = doHide;
        setScroller();
    }

    /**
     * Sets the Draggable elements - the Horizontal or Vertical Direction. One
     * can use a bitmasked 'or' (HORIZONTAL_SCROLL_BAR | VERTICAL_SCROLL_BAR ).
     *
     * @param mask One of HORIZONTAL_SCROLL_BAR, VERTICAL_SCROLL_BAR, or HORIZONTAL_SCROLL_BAR | VERTICAL_SCROLL_BAR
     */
    public void setDraggableElements(int mask)
    {
        scrollBarMask = mask;
    }

    /**
     * Sets the scrolling intensity - the default value being 5. Note, that this has an
     * inverse relationship to intensity (1 has the biggest difference, higher numbers having
     * less impact).
     *
     * @param intensity The new intensity value (Note the inverse relationship)
     */
    public void setScrollingIntensity(int intensity)
    {
        scrollingIntensity = intensity;
    }

    /**
     * Sets how frequently the animation will occur in milliseconds. Default
     * value is 30 milliseconds. 60+ will get a bit flickery.
     *
     * @param timing The timing, in milliseconds
     */
    public void setAnimationTiming(int timing)
    {
        animationSpeed = timing;
    }

    /**
     * @param damping sets the animation damping
     */
    public void setDamping(double damping)
    {
        this.damping = damping;
    }

    /**
     * @param en allow changing of the mouse cursor into a plus sign
     *           whenever the mouse is inside of the dragging panel.
     *           If this is false, we never touch the mouse cursor.
     */
    public void setAllowMouseCursorChanged(boolean en)
    {
        this.allowMouseCursorChanged = en;
    }

    /**
     * @return boolean indicating if this dragger is moving.
     *         This is typically used to NOT PROCEED with any mouse-clicks
     *         in the main application until the animation is over.
     */
    public boolean isMoving()
    {
        return lastMoveTimeMS + 100 > System.currentTimeMillis();
    }

    /**
     * @param mask may be {RESPOND_TO_LEFT_MOUSE_BUTTON|RESPOND_TO_MIDDLE_MOUSE_BUTTON|RESPOND_TO_RIGHT_MOUSE_BUTTON}
     */
    public void setRespondToMouseButton(int mask)
    {
        this.mouseButtonMask = mask;
    }

    /**
     * Allow Mouse Button Event
     * This method compares the currently-selected mouse button
     * with an internal mask of allowable buttons (left/middle/right)
     *
     * @param e mouse event containing pointer location, etc.
     */
    private boolean allowMouseEvent(MouseEvent e)
    {
        boolean allowLeftButton = (mouseButtonMask & RESPOND_TO_LEFT_MOUSE_BUTTON) != 0;
        boolean allowMiddleButton = (mouseButtonMask & RESPOND_TO_MIDDLE_MOUSE_BUTTON) != 0;
        boolean allowRightButton = (mouseButtonMask & RESPOND_TO_RIGHT_MOUSE_BUTTON) != 0;
        if (SwingUtilities.isLeftMouseButton(e) && allowLeftButton) return true;
        if (SwingUtilities.isMiddleMouseButton(e) && allowMiddleButton) return true;
        return SwingUtilities.isRightMouseButton(e) && allowRightButton;
    }

    /**
     * Mouse pressed implementation
     *
     * @param e mouse event containing pointer location, etc.
     *
     * TODO: edit so that is is moving, we stop, if not moving, dispatch mouse event through the dragscrolllistener...
     */
    @Override
    public void mousePressed(MouseEvent e)
    {
        if (allowMouseEvent(e))
        {
            if (animationTimer != null && animationTimer.isRunning())
            {
                animationTimer.stop();
                this.lastMoveTimeMS = System.currentTimeMillis();
            }
            if (this.allowMouseCursorChanged)
            {
                draggableComponent.setCursor(new Cursor(Cursor.MOVE_CURSOR));
            }
            dragSpeeds.clear();
            lastDragPoint = e.getPoint();
        }
    }

    /**
     * Mouse released implementation. This determines if further animation
     * is necessary and launches the appropriate times.
     *
     * @param e mouse event containing pointer location, etc.
     */
    @Override
    public void mouseReleased(MouseEvent e)
    {
        if (allowMouseEvent(e))
        {
            if (this.allowMouseCursorChanged)
            {
                draggableComponent.setCursor(defaultCursor);
            }
            if (scroller == null)
            {
                return;
            }

            //make sure the mouse ended in a dragging event
            long durationSinceLastDrag = System.currentTimeMillis() - lastDragTime;
            if (durationSinceLastDrag > 20)
            {
                return;
            }

            //get average speed for last few drags
            pixelsPerMSX = 0;
            pixelsPerMSY = 0;
            int j = 0;
            for (int i = dragSpeeds.size() - 1; i >= 0 && i > dragSpeeds.size() - 6; i--, j++)
            {
                pixelsPerMSX += dragSpeeds.get(i).x;
                pixelsPerMSY += dragSpeeds.get(i).y;
            }
            pixelsPerMSX /= -(double) j;
            pixelsPerMSY /= -(double) j;

            //start the timer
            if (Math.abs(pixelsPerMSX) > 0 || Math.abs(pixelsPerMSY) > 0)
            {
                animationTimer = new javax.swing.Timer(animationSpeed, new ScrollAnimator());
                animationTimer.start();
            }
        }
    }

    /**
     * MouseDragged implementation. Sets up timing and frame animation
     *
     * @param e mouse event containing pointer location, etc.
     */
    @Override
    public void mouseDragged(MouseEvent e)
    {
        if (allowMouseEvent(e))
        {
            if (scroller == null)
            {
                return;
            }

            Point p = e.getPoint();
            if (lastDragPoint == null) return;
            int diffx = p.x - lastDragPoint.x;
            int diffy = p.y - lastDragPoint.y;
            lastDragPoint = e.getPoint();

            //scroll the x axis
            if ((scrollBarMask & DRAGABLE_HORIZONTAL_SCROLL_BAR) != 0)
            {
                getHorizontalScrollBar().setValue(getHorizontalScrollBar().getValue() - diffx);
            }

            //the Scrolling affects mouse locations - offset the last drag point to compensate
            lastDragPoint.x = lastDragPoint.x - diffx;

            //scroll the y axis
            if ((scrollBarMask & DRAGABLE_VERTICAL_SCROLL_BAR) != 0)
            {
                getVerticalScrollBar().setValue(getVerticalScrollBar().getValue() - diffy);
            }

            //the Scrolling affects mouse locations - offset the last drag point to compensate
            lastDragPoint.y = lastDragPoint.y - diffy;

            //add a drag speed
            dragSpeeds.add(new Point2D.Double(
                    (e.getPoint().x - lastDragPoint.x),
                    (e.getPoint().y - lastDragPoint.y)));
            lastDragTime = System.currentTimeMillis();
        }
    }

    /**
     * Private inner class which accomplishes the animation.
     */
    private class ScrollAnimator implements ActionListener
    {
        /**
         * Performs the animation through the setting of the JScrollBar values
         *
         * @param e the unused action event
         */
        public void actionPerformed(ActionEvent e)
        {
            //damp the scrolling intensity
            pixelsPerMSX -= pixelsPerMSX * damping;
            pixelsPerMSY -= pixelsPerMSY * damping;

            //check to see if timer should stop.
            if (Math.abs(pixelsPerMSX) < 0.01 && Math.abs(pixelsPerMSY) < 0.01)
            {
                animationTimer.stop();
                lastMoveTimeMS = System.currentTimeMillis();
                return;
            }

            //calculate new X value
            int nValX = getHorizontalScrollBar().getValue() + (int) (pixelsPerMSX * scrollingIntensity);
            int nValY = getVerticalScrollBar().getValue() + (int) (pixelsPerMSY * scrollingIntensity);

            //Deal with out of scroll bounds
            if (nValX <= 0)
            {
                nValX = 0;
            } else if (nValX >= getHorizontalScrollBar().getMaximum())
            {
                nValX = getHorizontalScrollBar().getMaximum();
            }

            if (nValY <= 0)
            {
                nValY = 0;
            } else if (nValY >= getVerticalScrollBar().getMaximum())
            {
                nValY = getVerticalScrollBar().getMaximum();
            }

            //Check again to see if timer should stop
            if ((nValX == 0 || nValX == getHorizontalScrollBar().getMaximum()) && Math.abs(pixelsPerMSY) < 1)
            {
                animationTimer.stop();
                lastMoveTimeMS = System.currentTimeMillis();
                return;
            }

            if ((nValY == 0 || nValY == getVerticalScrollBar().getMaximum()) && Math.abs(pixelsPerMSX) < 1)
            {
                animationTimer.stop();
                lastMoveTimeMS = System.currentTimeMillis();
                return;
            }

            //Set new values
            if ((scrollBarMask & DRAGABLE_HORIZONTAL_SCROLL_BAR) != 0)
            {
                getHorizontalScrollBar().setValue(nValX);
            }

            if ((scrollBarMask & DRAGABLE_VERTICAL_SCROLL_BAR) != 0)
            {
                getVerticalScrollBar().setValue(nValY);
            }
        }
    }

    /**
     * @return retrieve the Horizontal Scroll Bar
     */
    public JScrollBar getHorizontalScrollBar()
    {
        return scroller.getHorizontalScrollBar();
    }

    /**
     * @return retrieve the Vertical Scroll Bar
     */
    public JScrollBar getVerticalScrollBar()
    {
        return scroller.getVerticalScrollBar();
    }

    /**
     * @param c the parent JScrollPane used to scroll everything
     */
    private JScrollPane getParentScroller(Component c)
    {
        Container parent = c.getParent();
        if (parent != null)
        {
            if (parent instanceof JScrollPane)
            {
                return (JScrollPane) parent;
            } else
            {
                return getParentScroller(parent);
            }
        }

        // cannot use the drag scroll listener
        throw new RuntimeException("Cannot find parent JScrollPane. Cannot use " + this.getClass().getSimpleName());
    }

    /**
     * ***************************************************************************************
     * ******************** MAIN - TEST OF THE DRAG SCROLL LISTENER **************************
     * ***************************************************************************************
     */

    public static void main(String[] args)
    {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Drawer dr = new Drawer();
        JScrollPane pane = new JScrollPane(dr);
        new DragScrollListener(dr); // main entry
        pane.setPreferredSize(new Dimension(300, 300));
        frame.getContentPane().add(pane);
        frame.pack();
        frame.setVisible(true);
        pane.getHorizontalScrollBar().setValue(10);
    }

    // Testing inner class that draws several squares of randomly selected colors.
    public static class Drawer extends JPanel
    {
        static final long serialVersionUID = 43214321L;

        int width = 10000;
        int height = 5000;
        Color[][] colors;

        // Constructs the JPanel and random colors
        public Drawer()
        {
            super();
            setPreferredSize(new Dimension(width, height));
            colors = new Color[width / 100][height / 100];
            for (int i = 0; i < colors.length; i++)
            {
                for (int j = 0; j < colors[i].length; j++)
                {
                    int r = (int) ((255) * Math.random());
                    int g = (int) ((255) * Math.random());
                    int b = (int) ((255) * Math.random());
                    colors[i][j] = new Color(r, g, b, 150);
                }
            }
        }

        @Override
        public void paintComponent(Graphics g)
        {
            super.paintComponent(g);
            for (int i = 0; i < width; i += 100)
            {
                for (int j = 0; j < height; j += 100)
                {
                    g.setColor(colors[i / 100][j / 100]);
                    g.fillRect(i + 5, j + 5, 95, 95);
                }
            }
        }
    }
}
