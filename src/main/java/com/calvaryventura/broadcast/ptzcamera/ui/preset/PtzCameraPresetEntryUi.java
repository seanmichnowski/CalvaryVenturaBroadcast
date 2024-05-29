package com.calvaryventura.broadcast.ptzcamera.ui.preset;

import java.awt.GridLayout;
import javax.swing.ButtonModel;
import javax.swing.JLabel;
import javax.swing.JToggleButton;
import com.calvaryventura.broadcast.ptzcamera.ui.PtzCameraControllerUi;
import com.calvaryventura.broadcast.settings.BroadcastSettings;
import com.calvaryventura.broadcast.uiwidgets.PopupComboboxUi;
import com.calvaryventura.broadcast.uiwidgets.TextFieldUtils;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.SoftBevelBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static com.calvaryventura.broadcast.ptzcamera.ui.preset.PtzCameraPresetEntryUi.PtzCameraPresetUiAction.CALL_BUTTON_PRESSED;
import static com.calvaryventura.broadcast.ptzcamera.ui.preset.PtzCameraPresetEntryUi.PtzCameraPresetUiAction.EDIT_BUTTON_DESELECTED;
import static com.calvaryventura.broadcast.ptzcamera.ui.preset.PtzCameraPresetEntryUi.PtzCameraPresetUiAction.EDIT_BUTTON_SELECTED;
import static com.calvaryventura.broadcast.ptzcamera.ui.preset.PtzCameraPresetEntryUi.PtzCameraPresetUiAction.NAME_CHANGED;
import static com.calvaryventura.broadcast.ptzcamera.ui.preset.PtzCameraPresetEntryUi.PtzCameraPresetUiAction.REORDER_BUTTON_PRESSED;
import static com.calvaryventura.broadcast.ptzcamera.ui.preset.PtzCameraPresetEntryUi.PtzCameraPresetUiAction.REORDER_BUTTON_RELEASED;

/**
 * Structure containing everything for ONE camera-preset item.
 * Each of these items will appear in the {@link PtzCameraControllerUi}.
 */
public class PtzCameraPresetEntryUi extends JPanel
{
    private final int cameraIdx;
    private final String cameraName;
    private final int presetIdx;
    private int presetOrderIdx;

    /**
     * Simple enum for sending action callbacks out of this class.
     */
    public enum PtzCameraPresetUiAction
    {
        NAME_CHANGED,
        REORDER_BUTTON_PRESSED,
        REORDER_BUTTON_RELEASED,
        EDIT_BUTTON_SELECTED,
        EDIT_BUTTON_DESELECTED,
        CALL_BUTTON_PRESSED
    }

    /**
     * @param cameraName display name of the camera associated with this preset
     * @param cameraIdx  index of the PTZ camera where these presets reside
     * @param presetName actual text of this preset, this is what persists to disk
     * @param presetIdx  numerical unique value for this preset (always stays constant regardless of name change)
     */
    public PtzCameraPresetEntryUi(String cameraName, int cameraIdx, String presetName, int presetIdx)
    {
        this.initComponents();
        this.textFieldName.setText(presetName);
        this.cameraIdx = cameraIdx;
        this.presetIdx = presetIdx;
        this.cameraName = cameraName;
    }

    /**
     * @param userAction fired when the user interacts with this preset UI entry
     */
    public void initializePresetUserAction(Consumer<PtzCameraPresetUiAction> userAction)
    {
        // set and GoTo (call) buttons
        this.buttonGoTo.addActionListener(e -> userAction.accept(CALL_BUTTON_PRESSED));
        this.buttonEdit.addActionListener(e -> userAction.accept(this.buttonEdit.isSelected() ? EDIT_BUTTON_SELECTED : EDIT_BUTTON_DESELECTED));

        // the reorder button produces two callbacks: one for pressed and one for released (could also be done with mouse listener)
        final AtomicBoolean lastChangeState = new AtomicBoolean();
        this.buttonReorder.getModel().addChangeListener(e -> {
            boolean pressed = ((ButtonModel) e.getSource()).isPressed();
            if (pressed != lastChangeState.get())
            {
                userAction.accept(pressed ? REORDER_BUTTON_PRESSED : REORDER_BUTTON_RELEASED);
                lastChangeState.set(pressed);
            }
        });

        // set up listener for the preset name changed action
        TextFieldUtils.attachTextListener(this.textFieldName, updatedText -> userAction.accept(NAME_CHANGED));

        // set up a popup menu that appears and provides default text options,
        // the popup appears when double-clicking the text field
        // the callback 'presetNameChangedAction' is automatically fired
        TextFieldUtils.attachMouseDoubleClickAction(this.textFieldName, () ->
                PopupComboboxUi.showPopupSelectionOptions(BroadcastSettings.getInst().getDefaultPresetNames(),
                        this.textFieldName.getText(), newText -> this.textFieldName.setText(newText)));
    }

    /**
     * @param enabled enables or disables all UI elements in this preset entry
     */
    public void setPresetEntryEnabled(boolean enabled)
    {
        this.buttonGoTo.setEnabled(enabled);
        this.buttonEdit.setEnabled(enabled);
        this.labelCameraName.setText(this.cameraName);
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
     * @return numerical 0-indexed index of the camera associated to this preset
     */
    public int getCameraIdx()
    {
        return cameraIdx;
    }

    /**
     * @return 0-indexed index of this preset for the specified camera
     */
    public int getPresetIdx()
    {
        return presetIdx;
    }

    /**
     * @return display name of the camera associated with this preset
     */
    public String getCameraName()
    {
        return cameraName;
    }

    /**
     * @return name of this preset item
     */
    public String getPresetName()
    {
        return this.textFieldName.getText().trim();
    }

    /**
     * @return UI display order of this preset (can be dynamically updated via {@link #setPresetOrderIdx(int)}
     */
    public int getPresetOrderIdx()
    {
        return presetOrderIdx;
    }

    /**
     * @param presetOrderIdx UI display order of this preset (can be queried via {@link #getPresetOrderIdx()}
     */
    public void setPresetOrderIdx(int presetOrderIdx)
    {
        this.presetOrderIdx = presetOrderIdx;
    }

    /**
     * Sets the EDIT button unselected (typically because another preset's EDIT button was selected).
     * Note: this does not produce any action event callback for the programmatic button unselecting.
     */
    public void setEditButtonDeselected()
    {
        this.buttonEdit.setSelected(false);
    }

    /**
     * JFormDesigner Auto-Generated Code.
     */
    @SuppressWarnings("all")
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        panelContent = new JPanel();
        labelCameraName = new JLabel();
        buttonGoTo = new JButton();
        textFieldName = new JTextField();
        JPanel panel1 = new JPanel();
        buttonEdit = new JToggleButton();
        buttonReorder = new JButton();

        //======== this ========
        setBorder(new EmptyBorder(12, 0, 12, 0));
        setPreferredSize(new Dimension(269, 80));
        setMinimumSize(new Dimension(249, 80));
        setOpaque(false);
        setMaximumSize(new Dimension(2147483647, 80));
        setName("this");
        setLayout(new BorderLayout());

        //======== panelContent ========
        {
            panelContent.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xcc00cc), 2),
                new CompoundBorder(
                    new SoftBevelBorder(SoftBevelBorder.LOWERED),
                    new EmptyBorder(2, 2, 2, 2))));
            panelContent.setBackground(new Color(0x660066));
            panelContent.setMinimumSize(new Dimension(249, 70));
            panelContent.setPreferredSize(new Dimension(269, 70));
            panelContent.setMaximumSize(new Dimension(2147483647, 70));
            panelContent.setName("panelContent");
            panelContent.setLayout(new GridBagLayout());
            ((GridBagLayout)panelContent.getLayout()).columnWidths = new int[] {0, 0, 0, 0};
            ((GridBagLayout)panelContent.getLayout()).rowHeights = new int[] {0, 0, 0};
            ((GridBagLayout)panelContent.getLayout()).columnWeights = new double[] {0.0, 1.0, 0.0, 1.0E-4};
            ((GridBagLayout)panelContent.getLayout()).rowWeights = new double[] {0.0, 1.0, 1.0E-4};

            //---- labelCameraName ----
            labelCameraName.setText("Camera Name");
            labelCameraName.setForeground(new Color(0x00a52c));
            labelCameraName.setName("labelCameraName");
            panelContent.add(labelCameraName, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 6), 0, 0));

            //---- buttonGoTo ----
            buttonGoTo.setToolTipText("GOTO this preset camera angle");
            buttonGoTo.setIcon(new ImageIcon(getClass().getResource("/icons/green_arrow_24x24.png")));
            buttonGoTo.setHorizontalTextPosition(SwingConstants.LEADING);
            buttonGoTo.setFont(new Font("Segoe UI", Font.BOLD, 16));
            buttonGoTo.setMaximumSize(new Dimension(50, 46));
            buttonGoTo.setMinimumSize(new Dimension(130, 46));
            buttonGoTo.setPreferredSize(new Dimension(130, 46));
            buttonGoTo.setForeground(new Color(0x00a52c));
            buttonGoTo.setBackground(Color.darkGray);
            buttonGoTo.setIconTextGap(2);
            buttonGoTo.setText("GOTO");
            buttonGoTo.setName("buttonGoTo");
            panelContent.add(buttonGoTo, new GridBagConstraints(0, 0, 1, 2, 0.0, 0.0,
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
            panelContent.add(textFieldName, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 6), 0, 0));

            //======== panel1 ========
            {
                panel1.setOpaque(false);
                panel1.setName("panel1");
                panel1.setLayout(new GridLayout(1, 2, 5, 5));

                //---- buttonEdit ----
                buttonEdit.setIcon(new ImageIcon(getClass().getResource("/icons/edit_red_34h.png")));
                buttonEdit.setFont(new Font("Segoe UI", Font.BOLD, 16));
                buttonEdit.setHorizontalTextPosition(SwingConstants.LEADING);
                buttonEdit.setForeground(new Color(0xcc6600));
                buttonEdit.setPreferredSize(new Dimension(40, 30));
                buttonEdit.setMinimumSize(new Dimension(40, 30));
                buttonEdit.setMaximumSize(new Dimension(100, 30));
                buttonEdit.setBackground(Color.darkGray);
                buttonEdit.setIconTextGap(2);
                buttonEdit.setOpaque(false);
                buttonEdit.setToolTipText("EDIT this preset");
                buttonEdit.setName("buttonEdit");
                panel1.add(buttonEdit);

                //---- buttonReorder ----
                buttonReorder.setIcon(new ImageIcon(getClass().getResource("/icons/blue_move_arrows_32x32.png")));
                buttonReorder.setFont(new Font("Segoe UI", Font.BOLD, 12));
                buttonReorder.setHorizontalTextPosition(SwingConstants.LEADING);
                buttonReorder.setForeground(new Color(0xcc6600));
                buttonReorder.setPreferredSize(new Dimension(40, 30));
                buttonReorder.setMinimumSize(new Dimension(40, 30));
                buttonReorder.setMaximumSize(new Dimension(30, 30));
                buttonReorder.setBackground(Color.darkGray);
                buttonReorder.setIconTextGap(2);
                buttonReorder.setOpaque(false);
                buttonReorder.setToolTipText("REORDER this preset in the list");
                buttonReorder.setName("buttonReorder");
                panel1.add(buttonReorder);
            }
            panelContent.add(panel1, new GridBagConstraints(2, 0, 1, 2, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));
        }
        add(panelContent, BorderLayout.CENTER);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel panelContent;
    private JLabel labelCameraName;
    private JButton buttonGoTo;
    private JTextField textFieldName;
    private JToggleButton buttonEdit;
    private JButton buttonReorder;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
