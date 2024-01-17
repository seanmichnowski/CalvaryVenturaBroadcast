package com.calvaryventura.broadcast.uiwidgets;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * This class shows a simple combobox popup menu.
 * Options are provided and an optional initial selection may also be specified.
 * When the user selects an option, the popup closes and the callback is fired.
 * If the user clicks anywhere outside the popup, no callback is fired and the
 * popup is closed.
 */
public class PopupComboboxUi
{
    /**
     * Creates the popup based on the provided selection options.
     * See documentation {@link PopupComboboxUi}.
     *
     * @param selectionOptions  options to show in the combobox dropdown
     * @param initialSelection  option to show selected in the dropdown (or empty)
     * @param selectionCallback fired when the user clicks one of the options.
     */
    public static void showPopupSelectionOptions(List<String> selectionOptions, String initialSelection, Consumer<String> selectionCallback)
    {
        // set up the combobox and its text options
        final List<String> options = selectionOptions.stream().map(String::trim).collect(Collectors.toList());
        final JComboBox<String> comboBox = new JComboBox<>();
        comboBox.setFont(new Font("Arial", Font.BOLD, 24));
        Collections.reverse(options); // add items in reverse since the combobox scrolls DOWN to the latest ones
        options.forEach(comboBox::addItem);
        if (!options.contains(initialSelection.trim()))
        {
            comboBox.addItem(initialSelection);
        }
        comboBox.setSelectedItem(initialSelection.trim());

        // set up the parent panel for the combobox
        final JPanel comboBoxPanel = new JPanel();
        comboBoxPanel.setLayout(new BorderLayout());
        comboBoxPanel.setBackground(Color.CYAN);
        comboBoxPanel.setPreferredSize(new Dimension(300, 45));
        comboBoxPanel.setBorder(new CompoundBorder(new LineBorder(Color.CYAN, 3), new LineBorder(Color.DARK_GRAY, 5)));
        comboBoxPanel.add(comboBox, BorderLayout.CENTER);

        // create an UNDECORATED frame to be our popup
        final JFrame f = new JFrame();
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        f.setAlwaysOnTop(true);
        final Point p = MouseInfo.getPointerInfo().getLocation();
        f.setLocation(p.x, p.y);
        f.setUndecorated(true);
        f.add(comboBoxPanel);
        f.pack();

        // popup action: fire the callback, delay a little, then hide the popup
        comboBox.addActionListener(e -> SwingUtilities.invokeLater(() -> {
            selectionCallback.accept((String) comboBox.getSelectedItem());
            try
            {
                Thread.sleep(250);
            } catch (InterruptedException ignored)
            {
            }
            f.setVisible(false);
        }));

        // when the user clicks anywhere OUTSIDE the popup menu, close the whole frame!
        // also, after the popup menu initializes, automatically expand the option dropdown
        comboBox.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                comboBox.showPopup();
            }

            @Override
            public void focusLost(FocusEvent e)
            {
                f.setVisible(false);
            }
        });

        // if the user cancels the combobox popup menu, just close the whole popup frame
        comboBox.addPopupMenuListener(new PopupMenuListener()
        {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e)
            {
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
            {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e)
            {
                f.setVisible(false);
            }
        });

        // show the popup (does not block)
        f.setVisible(true);
    }

    /**
     * Simple test of the popup.
     *
     * @param args no arguments, standalone test
     */
    public static void main(String[] args)
    {
        final List<String> options = Arrays.asList("one", "two", "three");
        PopupComboboxUi.showPopupSelectionOptions(options, "four", selection ->
                System.out.printf("Selected! %s\n", selection));
    }
}
