package com.calvaryventura.broadcast.ptzcamera.ui.preset;

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
import java.util.function.Consumer;

import static com.calvaryventura.broadcast.ptzcamera.ui.preset.PtzCameraPresetEntryUi.PtzCameraPresetUiAction.CALL_BUTTON_PRESSED;
import static com.calvaryventura.broadcast.ptzcamera.ui.preset.PtzCameraPresetEntryUi.PtzCameraPresetUiAction.EDIT_BUTTON_DESELECTED;
import static com.calvaryventura.broadcast.ptzcamera.ui.preset.PtzCameraPresetEntryUi.PtzCameraPresetUiAction.EDIT_BUTTON_SELECTED;
import static com.calvaryventura.broadcast.ptzcamera.ui.preset.PtzCameraPresetEntryUi.PtzCameraPresetUiAction.NAME_CHANGED;

/**
 * Structure containing everything for ONE camera-preset item.
 * Each of these items will appear in the {@link PtzCameraControllerUi}.
 */
public class PtzCameraPresetEntryUi extends JPanel
{
    private static final String DEFAULT_PRESET_NAME = "[...New Preset...]";
    private final int cameraIdx;
    private final String cameraName;
    private final int presetIdx;

    /**
     * Simple enum for sending action callbacks out of this class.
     */
    public enum PtzCameraPresetUiAction
    {
        NAME_CHANGED,
        EDIT_BUTTON_SELECTED,
        EDIT_BUTTON_DESELECTED,
        CALL_BUTTON_PRESSED
    }

    /**
     * @param cameraName display name of the camera associated with this preset
     * @param cameraIdx  index of the PTZ camera where these presets reside
     * @param presetName actual text of this preset, this is what persists to disk, or NULL for default name
     * @param presetIdx  numerical unique value for this preset (always stays constant regardless of name change)
     */
    public PtzCameraPresetEntryUi(String cameraName, int cameraIdx, String presetName, int presetIdx)
    {
        this.initComponents();
        this.textFieldName.setText(presetName == null ? DEFAULT_PRESET_NAME : presetName);
        this.cameraIdx = cameraIdx;
        this.presetIdx = presetIdx;
        this.cameraName = cameraName;
        this.labelCameraName.setText(this.cameraName);
    }

    /**
     * @param userAction fired when the user interacts with this preset UI entry
     */
    public void initializePresetUserAction(Consumer<PtzCameraPresetUiAction> userAction)
    {
        // set and GoTo (call) buttons
        this.buttonGoTo.addActionListener(e -> userAction.accept(CALL_BUTTON_PRESSED));
        this.buttonEdit.addActionListener(e -> userAction.accept(this.buttonEdit.isSelected() ? EDIT_BUTTON_SELECTED : EDIT_BUTTON_DESELECTED));

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
    }

    /**
     * @param connected indication if the associated camera for this preset is actively connected
     */
    public void setPresetEntryCameraConnectionStatus(boolean connected)
    {
        this.labelCameraName.setText(this.cameraName + (!connected ? " [Disconnected]" : ""));
    }

    /**
     * Clicks the EDIT button on this preset's UI.
     * Forces callbacks for the button too. Used when a new preset is added,
     * and we right away put the new preset into EDIT mode.
     */
    public void setEditButtonClicked()
    {
        if (!this.buttonEdit.isSelected())
        {
            this.buttonEdit.doClick();
        }
    }

    /**
     * @param color color to set as background for this preset item (or NULL for default/no color)
     */
    public void setPresetColor(Color color)
    {
        // background panel color
        this.panelContent.setBackground(color == null ? new Color(100, 0, 100) : color.darker().darker());

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
        this.panelContent = new JPanel();
        this.labelCameraName = new JLabel();
        this.buttonGoTo = new JButton();
        this.textFieldName = new JTextField();
        this.buttonEdit = new JToggleButton();
        this.labelReorder = new JLabel();

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
            this.panelContent.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xcc00cc), 2),
                new CompoundBorder(
                    new SoftBevelBorder(SoftBevelBorder.LOWERED),
                    new EmptyBorder(2, 2, 2, 2))));
            this.panelContent.setBackground(new Color(0x660066));
            this.panelContent.setMinimumSize(new Dimension(249, 70));
            this.panelContent.setPreferredSize(new Dimension(269, 70));
            this.panelContent.setMaximumSize(new Dimension(2147483647, 70));
            this.panelContent.setName("panelContent");
            this.panelContent.setLayout(new GridBagLayout());
            ((GridBagLayout)this.panelContent.getLayout()).columnWidths = new int[] {0, 0, 0, 0, 0};
            ((GridBagLayout)this.panelContent.getLayout()).rowHeights = new int[] {0, 0, 0};
            ((GridBagLayout)this.panelContent.getLayout()).columnWeights = new double[] {0.0, 1.0, 0.0, 0.0, 1.0E-4};
            ((GridBagLayout)this.panelContent.getLayout()).rowWeights = new double[] {0.0, 1.0, 1.0E-4};

            //---- labelCameraName ----
            this.labelCameraName.setText("Camera Name");
            this.labelCameraName.setForeground(new Color(0x00a52c));
            this.labelCameraName.setName("labelCameraName");
            this.panelContent.add(this.labelCameraName, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 6), 0, 0));

            //---- buttonGoTo ----
            this.buttonGoTo.setToolTipText("GOTO this preset camera angle");
            this.buttonGoTo.setIcon(new ImageIcon(getClass().getResource("/icons/green_arrow_24x24.png")));
            this.buttonGoTo.setHorizontalTextPosition(SwingConstants.LEADING);
            this.buttonGoTo.setFont(new Font("Segoe UI", Font.BOLD, 16));
            this.buttonGoTo.setMaximumSize(new Dimension(50, 46));
            this.buttonGoTo.setMinimumSize(new Dimension(50, 46));
            this.buttonGoTo.setPreferredSize(new Dimension(50, 46));
            this.buttonGoTo.setForeground(new Color(0x00a52c));
            this.buttonGoTo.setBackground(Color.darkGray);
            this.buttonGoTo.setIconTextGap(2);
            this.buttonGoTo.setName("buttonGoTo");
            this.panelContent.add(this.buttonGoTo, new GridBagConstraints(0, 0, 1, 2, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 6), 0, 0));

            //---- textFieldName ----
            this.textFieldName.setText("Name");
            this.textFieldName.setHorizontalAlignment(SwingConstants.CENTER);
            this.textFieldName.setFont(new Font("Segoe UI", Font.BOLD, 20));
            this.textFieldName.setForeground(new Color(0x00cccc));
            this.textFieldName.setBackground(Color.black);
            this.textFieldName.setSelectionColor(Color.yellow);
            this.textFieldName.setOpaque(false);
            this.textFieldName.setName("textFieldName");
            this.panelContent.add(this.textFieldName, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 6), 0, 0));

            //---- buttonEdit ----
            this.buttonEdit.setIcon(new ImageIcon(getClass().getResource("/icons/edit_red_34h.png")));
            this.buttonEdit.setFont(new Font("Segoe UI", Font.BOLD, 16));
            this.buttonEdit.setHorizontalTextPosition(SwingConstants.LEADING);
            this.buttonEdit.setForeground(new Color(0xcc6600));
            this.buttonEdit.setPreferredSize(new Dimension(40, 30));
            this.buttonEdit.setMinimumSize(new Dimension(40, 30));
            this.buttonEdit.setMaximumSize(new Dimension(100, 30));
            this.buttonEdit.setBackground(Color.darkGray);
            this.buttonEdit.setIconTextGap(2);
            this.buttonEdit.setOpaque(false);
            this.buttonEdit.setToolTipText("EDIT this preset");
            this.buttonEdit.setSelectedIcon(new ImageIcon(getClass().getResource("/icons/lamp_32x32.png")));
            this.buttonEdit.setName("buttonEdit");
            this.panelContent.add(this.buttonEdit, new GridBagConstraints(2, 0, 1, 2, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 6), 0, 0));

            //---- labelReorder ----
            this.labelReorder.setForeground(new Color(0x00a52c));
            this.labelReorder.setPreferredSize(new Dimension(40, 0));
            this.labelReorder.setMinimumSize(new Dimension(40, 0));
            this.labelReorder.setIcon(new ImageIcon(getClass().getResource("/icons/blue_move_arrows_32x32.png")));
            this.labelReorder.setHorizontalAlignment(SwingConstants.CENTER);
            this.labelReorder.setBorder(new LineBorder(Color.lightGray));
            this.labelReorder.setName("labelReorder");
            this.panelContent.add(this.labelReorder, new GridBagConstraints(3, 0, 1, 2, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));
        }
        add(this.panelContent, BorderLayout.CENTER);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel panelContent;
    private JLabel labelCameraName;
    private JButton buttonGoTo;
    private JTextField textFieldName;
    private JToggleButton buttonEdit;
    private JLabel labelReorder;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
