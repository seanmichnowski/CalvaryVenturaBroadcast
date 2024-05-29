package com.calvaryventura.broadcast.uiwidgets;

import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * Simple utility for interacting with JTextField's.
 */
public class TextFieldUtils
{
    /**
     * Utility for notifying the parent class whenever the user changes the text field text.
     *
     * @param textField         JTextField to attach this listener
     * @param textChangedAction fired when the user modifies the text in any way
     */
    public static void attachTextListener(JTextField textField, Consumer<String> textChangedAction)
    {
        // fire the action whenever the user manipulates the text in the text field
        textField.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void insertUpdate(DocumentEvent e)
            {
                SwingUtilities.invokeLater(() -> textChangedAction.accept(textField.getText()));
            }

            @Override
            public void removeUpdate(DocumentEvent e)
            {
                SwingUtilities.invokeLater(() -> textChangedAction.accept(textField.getText()));
            }

            @Override
            public void changedUpdate(DocumentEvent e)
            {
                SwingUtilities.invokeLater(() -> textChangedAction.accept(textField.getText()));
            }
        });

        // fire the action when the user presses ENTER in the text field
        textField.addActionListener(e -> SwingUtilities.invokeLater(() -> textChangedAction.accept(textField.getText())));

        // when the user clicks inside the text field, highlight all the text
        textField.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                super.mousePressed(e);
                textField.setSelectionStart(0);
                textField.setSelectionEnd(textField.getText().length());
            }
        });
    }

    /**
     * Utility for notifying the parent class whenever the user double-clicks the text field.
     *
     * @param textField JTextField to attach this listener
     * @param action    fired when the user double-clicks inside the component's document
     */
    public static void attachMouseDoubleClickAction(JTextField textField, Runnable action)
    {
        textField.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                super.mousePressed(e);
                if (e.getClickCount() == 2)
                {
                    textField.setSelectionStart(0); // remove any selection highlighting
                    textField.setSelectionEnd(0);
                    action.run();
                }
            }
        });
    }
}
