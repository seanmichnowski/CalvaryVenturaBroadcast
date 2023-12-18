package com.calvaryventura.broadcast.ptzcamera.ui;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Use this class simply to pop up a linear color chooser. This consists of a
 * JSlider with a rainbow background. When the user slides the slider's thumb,
 * we generate a callback for the current color. To use, only implement the
 * constructor, the callback is defined there.
 * TODO
 */
public class BroadcastPopupComboboxUi
{
    /**
     * Create the color chooser slider panel and set its
     * thumb closest to the specified starting color.
     *
     * TODO
     */
    public BroadcastPopupComboboxUi(List<String> selectionOptions, String initialSelection, Consumer<String> selectionCallback)
    {
        // set up the combobox and its text options
        final List<String> options = selectionOptions.stream().map(String::trim).collect(Collectors.toList());
        final JComboBox<String> combo = new JComboBox<>();
        combo.setFont(new Font("Arial", Font.BOLD, 24));
        options.forEach(combo::addItem);
        if (!options.contains(initialSelection.trim()))
        {
            combo.addItem(initialSelection);
        }
        combo.setSelectedItem(initialSelection.trim());

        // set up the parent panel for the combobox
        final JPanel comboPanel = new JPanel();
        comboPanel.setLayout(new BorderLayout());
        comboPanel.setBackground(Color.CYAN);
        comboPanel.setPreferredSize(new Dimension(300, 35));
        comboPanel.add(combo, BorderLayout.CENTER);

        // set up the popup
        final JPopupMenu popup = new JPopupMenu();
        popup.add(comboPanel, BorderLayout.CENTER);
        popup.setBorder(new CompoundBorder(new LineBorder(Color.CYAN, 5), new LineBorder(Color.DARK_GRAY, 5)));
        popup.setBorderPainted(true);

        // popup action: fire the callback, delay a little, then hide the popup
        combo.addActionListener(e -> SwingUtilities.invokeLater(() -> {
            selectionCallback.accept((String) combo.getSelectedItem());
            try
            {
                Thread.sleep(250);
            } catch (InterruptedException ignored)
            {
            }
            popup.setVisible(false);
        }));

        popup.addMouseListener(new MouseAdapter()
        {
            public void mousePressed(MouseEvent e)
            {
                System.out.printf("mouse X=%d, Y=%d\n", e.getX(), e.getY());
                if (!(e.getX() < popup.getVisibleRect().getMinX()) && !(e.getX() > popup.getVisibleRect().getMaxX()))
                {
                    //this.repaint();

                }
            }
        });

        // show the popup at the mouse pointer
        final Point p = MouseInfo.getPointerInfo().getLocation();
        popup.show(null, p.x, p.y);
    }

    /**
     * Simple test of the popup.
     *
     * @param args no arguments, standalone test
     */
    public static void main(String[] args)
    {
        final List<String> options = Arrays.asList("one", "two", "three");
        new BroadcastPopupComboboxUi(options, "four", selection ->
                System.out.printf("Selected! %s\n", selection));
    }
}
