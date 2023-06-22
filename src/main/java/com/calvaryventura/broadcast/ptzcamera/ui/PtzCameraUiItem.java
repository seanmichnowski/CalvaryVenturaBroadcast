package com.calvaryventura.broadcast.ptzcamera.ui;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serializable;
import java.util.concurrent.Callable;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Structure containing everything for ONE camera preset item.
 * Each of these items will appear in the {@link PtzCameraUi}.
 */
public class PtzCameraUiItem extends JPanel implements Serializable
{
    private final int presetIdx;

    /**
     * @param presetIdx numerical unique value for this preset (always stays constant regardless of name change)
     * @param presetNameChangedAction fired when we change the name of this preset
     */
    public PtzCameraUiItem(int presetIdx, Runnable presetNameChangedAction)
    {
        this.presetIdx = presetIdx;
        this.initComponents();
        this.textFieldName.setText(String.valueOf(presetIdx));
        this.highlightTextField(this.textFieldName);

        // set up listener for the preset name changed action
        this.textFieldName.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void insertUpdate(DocumentEvent e)
            {
                presetNameChangedAction.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e)
            {
                presetNameChangedAction.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e)
            {
                presetNameChangedAction.run();
            }
        });
    }

    /**
     * Track the press/release time for the SET button, and only allow
     * the SET action to proceed if the button was pressed for ~2 seconds.
     * Internally use 1500ms since there's a UI delay it seems.
     *
     * @param presetSetAction fired when the user clicks the "Set" button
     */
    public void onPresetSetClicked(Runnable presetSetAction)
    {
        this.buttonSet.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(buttonSet, "Confirm setting new camera preset?\nPreset name: " + this.textFieldName.getText().trim(),
                    "Camera Preset", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
            {
                presetSetAction.run();
            }
        });
    }

    /**
     * @param presetCalledAction fired when the user clicks the "GoTo" button
     */
    public void onPresetCalledClicked(Callable<Boolean> presetCalledAction)
    {
        this.buttonGoTo.addActionListener(e -> {
            try
            {
                this.buttonGoTo.setEnabled(false); // grey out
                presetCalledAction.call();         // attempt to move the camera
                this.buttonGoTo.setEnabled(true);  // reenable
            } catch (Exception ex)
            {
                this.buttonGoTo.setEnabled(true);
                System.out.printf("Cannot move camera! Error=%s\n", ex.getMessage());
            }
        });
    }

    /**
     * @param textField the text field to highlight
     */
    private void highlightTextField(JTextField textField)
    {
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
     * @return name of this preset item
     */
    public String getPresetName()
    {
        return this.textFieldName.getText().trim();
    }

    /**
     * @param name name of this preset item
     */
    public void setPresetName(String name)
    {
        this.textFieldName.setText(name);
    }

    /**
     * @param color color to set as background for this preset item (or NULL for default/no color)
     */
    public void setContentPanelColor(Color color)
    {
        // background panel color
        this.panelContent.setBackground(color == null ? Color.BLACK : color);

        // set the border color around this preset UI item (default when color is NULL is a purple color, see JFD form)
        final Color borderColor = color == null ? new Color(0xcc00cc) : color.darker();
        this.panelContent.setBorder(new CompoundBorder(
            new LineBorder(borderColor, 2),
            new CompoundBorder(
                    new SoftBevelBorder(SoftBevelBorder.LOWERED),
                    new EmptyBorder(2, 2, 2, 2))));
    }

    /**
     * @return ID of this preset used to send to the camera, stays the same regardless of name change
     */
    public int getPresetIdx()
    {
        return presetIdx;
    }

    /**
     * JFormDesigner Auto-Generated Code.
     */
    @SuppressWarnings("all")
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        panelContent = new JPanel();
        buttonGoTo = new JButton();
        textFieldName = new JTextField();
        buttonSet = new JButton();

        //======== this ========
        setBorder(new EmptyBorder(6, 0, 6, 0));
        setPreferredSize(new Dimension(269, 70));
        setMinimumSize(new Dimension(249, 70));
        setOpaque(false);
        setName("this");
        setLayout(new BorderLayout());

        //======== panelContent ========
        {
            panelContent.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xcc00cc), 2),
                new CompoundBorder(
                    new SoftBevelBorder(SoftBevelBorder.LOWERED),
                    new EmptyBorder(2, 2, 2, 2))));
            panelContent.setBackground(Color.black);
            panelContent.setMinimumSize(new Dimension(249, 70));
            panelContent.setPreferredSize(new Dimension(269, 70));
            panelContent.setName("panelContent");
            panelContent.setLayout(new GridBagLayout());
            ((GridBagLayout)panelContent.getLayout()).columnWidths = new int[] {0, 0, 0, 0};
            ((GridBagLayout)panelContent.getLayout()).rowHeights = new int[] {0, 0};
            ((GridBagLayout)panelContent.getLayout()).columnWeights = new double[] {0.0, 1.0, 0.0, 1.0E-4};
            ((GridBagLayout)panelContent.getLayout()).rowWeights = new double[] {1.0, 1.0E-4};

            //---- buttonGoTo ----
            buttonGoTo.setToolTipText("Goto this preset camera angle");
            buttonGoTo.setIcon(new ImageIcon(getClass().getResource("/icons/green_arrow_24x24.png")));
            buttonGoTo.setHorizontalTextPosition(SwingConstants.LEADING);
            buttonGoTo.setFont(new Font("Segoe UI", Font.BOLD, 16));
            buttonGoTo.setMaximumSize(new Dimension(50, 46));
            buttonGoTo.setMinimumSize(new Dimension(50, 46));
            buttonGoTo.setPreferredSize(new Dimension(50, 46));
            buttonGoTo.setForeground(new Color(0x00a52c));
            buttonGoTo.setBackground(Color.darkGray);
            buttonGoTo.setIconTextGap(2);
            buttonGoTo.setName("buttonGoTo");
            panelContent.add(buttonGoTo, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 6), 0, 0));

            //---- textFieldName ----
            textFieldName.setText("Name");
            textFieldName.setHorizontalAlignment(SwingConstants.CENTER);
            textFieldName.setFont(new Font("Segoe UI", Font.BOLD, 20));
            textFieldName.setForeground(new Color(0x00cccc));
            textFieldName.setBackground(Color.black);
            textFieldName.setSelectionColor(Color.yellow);
            textFieldName.setOpaque(false);
            textFieldName.setName("textFieldName");
            panelContent.add(textFieldName, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 6), 0, 0));

            //---- buttonSet ----
            buttonSet.setIcon(new ImageIcon(getClass().getResource("/icons/orange_location_flag_24x24.png")));
            buttonSet.setText("SET");
            buttonSet.setFont(new Font("Segoe UI", Font.BOLD, 16));
            buttonSet.setHorizontalTextPosition(SwingConstants.LEADING);
            buttonSet.setForeground(new Color(0xcc6600));
            buttonSet.setPreferredSize(new Dimension(100, 30));
            buttonSet.setMinimumSize(new Dimension(100, 30));
            buttonSet.setMaximumSize(new Dimension(100, 30));
            buttonSet.setBackground(Color.darkGray);
            buttonSet.setIconTextGap(2);
            buttonSet.setName("buttonSet");
            panelContent.add(buttonSet, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));
        }
        add(panelContent, BorderLayout.CENTER);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel panelContent;
    private JButton buttonGoTo;
    private JTextField textFieldName;
    private JButton buttonSet;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
