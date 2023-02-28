package com.calvaryventura.broadcast.uiwidgets;import javax.swing.*;import java.awt.*;import java.util.List;import java.awt.event.MouseAdapter;import java.awt.event.MouseEvent;import java.awt.geom.Ellipse2D;import java.awt.geom.Rectangle2D;import java.util.ArrayList;import java.util.function.Consumer;/** * Custom UI widget for showing progress for the frequency and overall mission sweeps. * Both frequency and mission are displayed in the same plot. */public class HorizontalTouchSliderUi extends JComponent{    private static final Color BACKGROUND_COLOR = Color.GRAY.darker();    private static final Color LINE_COLOR = new Color(150, 255, 150);    private static final Color CENTER_DOT_COLOR = new Color(150, 150, 255);    private static final Color CENTER_DOT_MOVING_COLOR = new Color(255, 190, 0);    private static final Double centerCirclePercentCoverage = 0.7;    private static final int LINE_WIDTH_PIX = 1;    private final List<Consumer<Double>> valueChangedConsumers = new ArrayList<>();    private String displayMessageCenter = null;    private String displayMessageLeft = null;    private String displayMessageRight = null;    private Color displayMessageColor = Color.WHITE;    private Integer mouseDragXPixel;    private double circleSizePix;    /**     * Creates the progress indicator and sets up general UI component properties.     */    public HorizontalTouchSliderUi()    {        this.setLayout(null);        this.setOpaque(false);        this.setBackground(BACKGROUND_COLOR);        this.setBorder(BorderFactory.createRaisedBevelBorder());        this.addMouseListener(createMouseEventHandler());        this.addMouseMotionListener(createMouseEventHandler());    }    /**     * @param valueChangedConsumer used to send the latest X thumb location back to the parent     */    public void addValueChangedConsumer(Consumer<Double> valueChangedConsumer)    {        this.valueChangedConsumers.add(valueChangedConsumer);    }    /**     * Writes a message to the upper half of the UI panel.     *     * @param msgCenter center message to show on the dot itself     * @param msgLeft the text to display (NULL to clear)     * @param msgRight rightmost message to display     * @param c   color to display the text     */    public void setDisplayMessage(String msgCenter, String msgLeft, String msgRight, Color c)    {        this.displayMessageCenter = msgCenter;        this.displayMessageLeft = msgLeft;        this.displayMessageRight = msgRight;        this.displayMessageColor = c;        this.repaint();    }    /**     * This class extends a JComponent. Override the UI component's 'paintComponent' method     * which allows us to insert out own custom code for drawing graphics onto the     * panel. Note that using this requires setting a 'null' layout for the panel.     * {@inheritDoc}     */    @Override    public void paintComponent(Graphics g)    {        // draw the solid background        super.paintComponent(g);        final Graphics2D g2d = (Graphics2D) g;        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // TODO        g2d.setColor(BACKGROUND_COLOR);        g2d.fillRect(0, 0, getWidth(), getHeight());        // draw the thin lines        g2d.setColor(LINE_COLOR);        g2d.setStroke(new BasicStroke(LINE_WIDTH_PIX));        g2d.drawLine(getWidth() / 2, 0, getWidth() / 2, getHeight());        g2d.drawLine(0, getHeight() / 2, getWidth(), getHeight() / 2);        // draw the ellipse line        this.circleSizePix = centerCirclePercentCoverage * getHeight();        g2d.draw(new Ellipse2D.Double(                (float) getWidth() / 2 - circleSizePix / 2,                (float) getHeight() / 2 - circleSizePix / 2,                circleSizePix,                circleSizePix));        // draw the center ellipse fill        g2d.setColor(this.mouseDragXPixel == null ? CENTER_DOT_COLOR : CENTER_DOT_COLOR.darker());        g2d.fill(new Ellipse2D.Double(                (float) getWidth() / 2 - circleSizePix / 2 + LINE_WIDTH_PIX,                (float) getHeight() / 2 - circleSizePix / 2 + LINE_WIDTH_PIX,                circleSizePix - 2 * LINE_WIDTH_PIX,                circleSizePix - 2 * LINE_WIDTH_PIX));        // draw the left/right text        if (this.displayMessageLeft != null)        {            final Font font = new Font("Arial", Font.BOLD, 20);            final Rectangle2D rect = font.getStringBounds(this.displayMessageLeft, g2d.getFontRenderContext());            final int blockStartX = 10;            final int blockStartY = (int) (getHeight() / 4 + rect.getHeight() / 2 - 3);            g2d.setColor(this.displayMessageColor);            g2d.setFont(font);            g2d.drawString(this.displayMessageLeft, blockStartX, blockStartY);        }        if (this.displayMessageRight != null)        {            final Font font = new Font("Arial", Font.BOLD, 20);            final Rectangle2D rect = font.getStringBounds(this.displayMessageRight, g2d.getFontRenderContext());            final int blockStartX = (int) (getWidth() - 10 - rect.getWidth());            final int blockStartY = (int) (getHeight() / 4 + rect.getHeight() / 2 - 3);            g2d.setColor(this.displayMessageColor);            g2d.setFont(font);            g2d.drawString(this.displayMessageRight, blockStartX, blockStartY);        }        // draw the moving ellipse fill        if (this.mouseDragXPixel != null)        {            g2d.setColor(CENTER_DOT_MOVING_COLOR);            g2d.fill(new Ellipse2D.Double(                    (float) this.mouseDragXPixel - circleSizePix / 2 + LINE_WIDTH_PIX,                    (float) getHeight() / 2 - circleSizePix / 2 + LINE_WIDTH_PIX, // Y fixed at midpoint                    circleSizePix - 2 * LINE_WIDTH_PIX,                    circleSizePix - 2 * LINE_WIDTH_PIX));        }        // draw text        if (this.displayMessageCenter != null)        {            final Font font = new Font("Arial", Font.BOLD, 16);            final Rectangle2D rect = font.getStringBounds(this.displayMessageCenter, g2d.getFontRenderContext());            final int blockStartX = (int) (getWidth() / 2 - rect.getWidth() / 2);            final int blockStartY = (int) (getHeight() / 2 + rect.getHeight() / 2 - 3);            g2d.setColor(this.displayMessageColor);            g2d.setFont(font);            g2d.drawString(this.displayMessageCenter, blockStartX, blockStartY);        }        // required for Linux/Apple computers        Toolkit.getDefaultToolkit().sync();        g.dispose();    }    /**     * Sets up a simple mouse press and drag action, which updates the global     * variable 'mouseDragPt' when the user moves the mouse outside the center zone.     *     * @return a mouse handler interface ready to be used by this panel     */    private MouseAdapter createMouseEventHandler()    {        return new MouseAdapter()        {            /**             * Takes in the mouse location and sets the global 'mouseDragPt' property.             *             * @param p location of the mouse press or drag             */            private void updateMouseMovePoint(Point p)            {                final double x = Math.min(Math.max(p.getX(), circleSizePix / 2), getWidth() - circleSizePix / 2);                final double xPercent = 2 * (x - (double) getWidth() / 2) / (getWidth() - circleSizePix);   // multiply by 2 to get percentages                final boolean movementInX = Math.abs(x - (double) getWidth() / 2) > circleSizePix / 3;                mouseDragXPixel = !movementInX ? null : (int) x;                valueChangedConsumers.forEach(c -> c.accept(movementInX ? xPercent : 0));                repaint();            }            @Override            public void mouseReleased(MouseEvent e)            {                mouseDragXPixel = null;                valueChangedConsumers.forEach(c -> c.accept(0.0));                repaint();            }            @Override            public void mousePressed(MouseEvent e)            {                updateMouseMovePoint(e.getPoint());            }            @Override            public void mouseDragged(MouseEvent e) {                updateMouseMovePoint(e.getPoint());            }        };    }    /**     * @param args no arguments     */    public static void main(String[] args)    {        final HorizontalTouchSliderUi uut = new HorizontalTouchSliderUi();        final JFrame f = new JFrame("Horizontal touch slider test");        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);        f.add(uut);        f.setSize(600, 100);        f.setLocationRelativeTo(null);        f.setVisible(true);        // print out whenever the consumer fires        uut.addValueChangedConsumer(horizontalLocation ->                System.out.printf("LOCATION = %.0f%%\n", horizontalLocation * 100));        // draw text        uut.setDisplayMessage("NOT CONNECTED", "LEFT", "RIGHT", Color.RED);    }}