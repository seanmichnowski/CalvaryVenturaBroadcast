package com.calvaryventura.broadcast.ptz.ui;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serializable;
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
    private int presetIdx;
    private PtzCameraUiItemState state;

    /**
     * @param name default name of the preset
     */
    public PtzCameraUiItem(String name, Runnable presetNameChangedAction)
    {
        this.initComponents();
        this.textFieldName.setText(name.trim());
        this.highlightTextField(this.textFieldName);
        this.state = PtzCameraUiItemState.NOT_SELECTED;

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
    public void onPresetCalledClicked(Runnable presetCalledAction)
    {
        this.buttonGoTo.addActionListener(e -> presetCalledAction.run());
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

    public void setContentPanelColor(Color color)
    {
        this.panelContent.setBackground(color);
        this.buttonGoTo.setBackground(color);
        this.buttonSet.setBackground(color);
        this.textFieldName.setBackground(color);
    }

    public void setPresetIdx(int presetIdx)
    {
        this.presetIdx = presetIdx;
    }

    public int getPresetIdx()
    {
        return presetIdx;
    }

    public PtzCameraUiItemState getState()
    {
        return state;
    }

    public void setState(PtzCameraUiItemState state)
    {
        this.state = state;
    }

    /**
     * JFormDesigner Auto-Generated Code.
     */
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        panelContent = new JPanel();
        buttonGoTo = new JButton();
        textFieldName = new JTextField();
        buttonSet = new JButton();

        //======== this ========
        setBorder(new EmptyBorder(6, 0, 6, 0));
        setPreferredSize(new Dimension(269, 50));
        setMinimumSize(new Dimension(249, 50));
        setOpaque(false);
        setName("this");
        setLayout(new BorderLayout());

        //======== panelContent ========
        {
            panelContent.setBorder(new CompoundBorder(
                new LineBorder(new Color(204, 0, 204), 2),
                new CompoundBorder(
                    new SoftBevelBorder(SoftBevelBorder.LOWERED),
                    new EmptyBorder(2, 2, 2, 2))));
            panelContent.setBackground(Color.black);
            panelContent.setMinimumSize(new Dimension(249, 50));
            panelContent.setPreferredSize(new Dimension(269, 50));
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
            buttonGoTo.setForeground(new Color(0, 165, 44));
            buttonGoTo.setBackground(Color.black);
            buttonGoTo.setIconTextGap(2);
            buttonGoTo.setOpaque(false);
            buttonGoTo.setName("buttonGoTo");
            panelContent.add(buttonGoTo, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 6), 0, 0));

            //---- textFieldName ----
            textFieldName.setText("Name");
            textFieldName.setHorizontalAlignment(SwingConstants.CENTER);
            textFieldName.setFont(new Font("Segoe UI", Font.BOLD, 20));
            textFieldName.setForeground(new Color(0, 204, 204));
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
            buttonSet.setForeground(new Color(204, 102, 0));
            buttonSet.setPreferredSize(new Dimension(100, 30));
            buttonSet.setMinimumSize(new Dimension(100, 30));
            buttonSet.setMaximumSize(new Dimension(100, 30));
            buttonSet.setBackground(Color.black);
            buttonSet.setIconTextGap(2);
            buttonSet.setOpaque(false);
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
