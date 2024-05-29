package com.calvaryventura.broadcast.ptzcamera.ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import com.calvaryventura.broadcast.ptzcamera.ui.preset.PtzCameraPresetEntryUi;
import com.calvaryventura.broadcast.ptzcamera.ui.preset.PtzCameraPresetEntryUi.PtzCameraPresetUiAction;
import com.calvaryventura.broadcast.uiwidgets.DirectionalTouchUi;
import com.calvaryventura.broadcast.uiwidgets.DragAndDropUtility;
import com.calvaryventura.broadcast.uiwidgets.DragScrollListener;
import com.calvaryventura.broadcast.uiwidgets.HorizontalZoomTouchUi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This is the top-level UI for controlling a SINGLE PTZ camera.
 * See the JFormDesigner layout for all the components involved.
 */
public class PtzCameraControllerUi extends JPanel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final File PRESETS_PERSISTENCE_FILE = new File(System.getProperty("user.home") + "/broadcast_camera_presets.txt");
    private final Map<String, Boolean> ptzCameraNamesConnectionStatuses = new HashMap<>();
    private final IPtzCameraControllerUiCallback callback;
    private final List<PtzCameraPresetEntryUi> presets;
    private final DragAndDropUtility dh = new DragAndDropUtility();
    private PtzCameraPresetEntryUi presetSelectedForEditing;

    /**
     * @param ptzCameraNames display name(s) for all desired PTZ cameras in the system
     * @param callback       mechanism to pass UI events OUT of this class
     */
    public PtzCameraControllerUi(List<String> ptzCameraNames, IPtzCameraControllerUiCallback callback)
    {
        this.initComponents();
        this.callback = callback;

        // create the presets from disk and connect callback actions
        this.presets = this.loadPresetNamesSavedToDisk(ptzCameraNames);
        this.presets.forEach(preset -> preset.initializePresetUserAction(action -> this.processPresetUserAction(preset, action)));
        this.buttonSaveEdits.addActionListener(e -> this.processSavePresetButton());

        // button for adding a new preset
        this.buttonAddNewPreset.addActionListener(e -> {
            final String cameraName = ptzCameraNames.get(this.comboBoxCameraNames.getSelectedIndex());
            final int presetIdx = 0; // TODO

            // create the new preset and add it to the list of presets
            final PtzCameraPresetEntryUi newPreset = new PtzCameraPresetEntryUi(cameraName, this.comboBoxCameraNames.getSelectedIndex(),
                    "[...New Preset...]", presetIdx);
            this.presets.add(newPreset);

            // initialize this new preset in the same manner as we would initialize any other preset
            newPreset.initializePresetUserAction(action -> this.processPresetUserAction(newPreset, action));
            newPreset.setPresetEntryEnabled(this.ptzCameraNamesConnectionStatuses.get(cameraName));
            this.redrawAllCameraPresetsIntoScrollableUiPanel();
        });

        // button for deleting a preset
        this.buttonDeletePreset.addActionListener(e -> {
            this.presets.remove(this.presetSelectedForEditing);
            this.processPresetUserAction(this.presetSelectedForEditing, PtzCameraPresetUiAction.EDIT_BUTTON_DESELECTED);
            this.redrawAllCameraPresetsIntoScrollableUiPanel();
        });

        // default camera connection state is false/not connected for all cameras specified
        ptzCameraNames.forEach(ptzCameraName -> this.setCameraConnectionStatus(ptzCameraName, false));

        // initialize callbacks from the UI elements, include the selected camera index in the callback
        this.directionalSwipePanel.addXYOutputConsumer((pan, tilt) -> this.callback.panTilt(this.comboBoxCameraNames.getSelectedIndex(), pan, tilt));
        this.zoomSlider.addValueChangedConsumer(zoom -> this.callback.zoom(this.comboBoxCameraNames.getSelectedIndex(), zoom));

        // initially draw the preset tiles into the UI scroll pane
        new DragScrollListener(this.panelPresetsHolder).hideScrollBars(true);
        this.redrawAllCameraPresetsIntoScrollableUiPanel();
    }

    /**
     * Call this whenever we want to update the number of camera presets shown on the UI.
     * All camera presets are placed into a scrollable panel.
     */
    private void redrawAllCameraPresetsIntoScrollableUiPanel()
    {
// TODO
    /*
        // complicated layout descriptor for adding PTZ camera preset items into their layout panel, but it does make them stack nice and allows vertical space between entries to grow and fill
        final GridBagConstraints gridBagConstraints = new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, this.presets.size(),
                1.0, 1.0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 20, 5), 0, 0);

        // add all UI tiles to the scrollable pane and revalidate
        this.presets.forEach(presetUiTile -> this.panelPresetsHolder.add(presetUiTile, gridBagConstraints));
        this.revalidate();
     */

        final Box box = Box.createVerticalBox();
        box.addMouseListener(this.dh);
        box.addMouseMotionListener(this.dh);
        this.presets.forEach(box::add);
        this.panelPresetsHolder.removeAll();
        this.panelPresetsHolder.add(box, BorderLayout.CENTER);
        this.panelPresetsHolder.revalidate();
    }

    /**
     * Callback when the user interacts with one of the local UI preset tiles.
     *
     * @param preset       preset the user is currently interacting with
     * @param presetAction user action currently being taken on this preset
     */
    private void processPresetUserAction(PtzCameraPresetEntryUi preset, PtzCameraPresetUiAction presetAction)
    {
        switch (presetAction)
        {
            // just changing the name of the preset, update on disk, no other action required
            case NAME_CHANGED:
                this.presetNameChangedSaveToDisk();
                break;

            // EDIT button triggers the right-side UI portion for editing this preset
            case EDIT_BUTTON_SELECTED:
                this.buttonSaveEdits.setEnabled(true);
                this.buttonDeletePreset.setEnabled(true);
                this.directionalSwipePanel.setEnabled(true); // TODO the enable/disable doesn't do anything yet!!
                this.zoomSlider.setEnabled(true);
                this.presets.stream().filter(p -> !p.equals(preset)).forEach(PtzCameraPresetEntryUi::setEditButtonDeselected);
                this.labelPresetEditStatus.setText("<html><u>Currently Editing Preset:</u><br>" + preset.getPresetName() + "</html>");
                this.presetSelectedForEditing = preset;
                break;

            // EDIT button deselected removed selection state
            case EDIT_BUTTON_DESELECTED:
                this.buttonSaveEdits.setEnabled(false);
                this.buttonDeletePreset.setEnabled(false);
                this.directionalSwipePanel.setEnabled(false);
                this.zoomSlider.setEnabled(false);
                this.presets.forEach(PtzCameraPresetEntryUi::setEditButtonDeselected);
                this.labelPresetEditStatus.setText("<html><u>Currently Editing Preset:</u><br>[Nothing selected]</html>");
                this.presetSelectedForEditing = null;
                break;

            // call/GoTo preset action (returns a boolean indicating successful camera movement)
            case CALL_BUTTON_PRESSED:
                this.callback.callPressed(preset.getCameraIdx(), preset.getPresetIdx());
                break;

            case REORDER_BUTTON_PRESSED:
                logger.info("\n\nPRESS\n\n");
                // TODO
                //SwingUtilities.invokeLater(() -> this.dh.startDragAndDropReorder(preset));
                break;

            case REORDER_BUTTON_RELEASED:
                logger.info("\n\nRELEASED !!\n\n");
                // TODO
                //SwingUtilities.invokeLater(() -> this.dh.startDragAndDropReorder(preset));
                break;
        }
    }

    /**
     * When we are editing a preset, and the user clicks the "SAVE EDITS" button,
     * this gets called to update the 'set' callback, and also to update the status label.
     */
    private void processSavePresetButton()
    {
        final boolean setOk = this.callback.setPressed(this.presetSelectedForEditing.getCameraIdx(), this.presetSelectedForEditing.getPresetIdx());
        final String origText = this.labelPresetEditStatus.getText();
        this.labelPresetEditStatus.setText("<html><u>Currently Editing Preset:</u><br>" + this.presetSelectedForEditing.getPresetName() +
                "<font color='white'>&nbsp;&nbsp;[Save: " + (setOk ? "OK" : "FAILED") + "]</font></html>");
        final Timer tOriginal = new Timer(0, action -> this.labelPresetEditStatus.setText(origText));
        tOriginal.setInitialDelay(2000);
        tOriginal.setRepeats(false);
        tOriginal.start();
    }

    /**
     * NOTE: we make this synchronized because the possibility of both cameras connecting around the
     * same time and both updating the UI with their updated connection state.
     *
     * @param cameraName name of the camera for which we update the UI connection status
     * @param connected  indication if we are connected to the camera's VISCA control port
     */
    public synchronized void setCameraConnectionStatus(String cameraName, boolean connected)
    {
        // update camera connection status in the presets associated to this specified camera
        this.presets.stream().filter(p -> p.getCameraName().equalsIgnoreCase(cameraName))
                .forEach(presetUiTile -> presetUiTile.setPresetEntryEnabled(connected));

        // update the local map of camera connection status
        this.ptzCameraNamesConnectionStatuses.put(cameraName, connected);

        // set the camera names in the dropdown box
        final int selIdx = this.comboBoxCameraNames.getSelectedIndex();
        this.comboBoxCameraNames.removeAllItems();
        this.ptzCameraNamesConnectionStatuses.forEach((name, conn) -> this.comboBoxCameraNames.addItem("<html>" + name + "&nbsp;&nbsp;" +
                (conn ? "<font color='green'>[Connected]</font>" : "<font color='red'>[Not connected]</font>") + "</html>"));
        this.comboBoxCameraNames.setSelectedIndex(selIdx);
    }

    /**
     * Sets the display background color or any preset that was selected.
     *
     * @param backgroundColor color to display
     */
    public void setActivePresetBackgroundColor(Color backgroundColor)
    {
    /*
        this.presets.forEach(p -> p.setContentPanelColor(null));
        if (this.lastClickedPresetIdx >= 0 && this.lastClickedPresetIdx < this.presets.size())
        {
            this.presets.get(this.lastClickedPresetIdx).setContentPanelColor(backgroundColor);
        }
        this.lastBackgroundColor = backgroundColor;

     */
    }

    // TODO
    public void setActivePreviewSelection(int cameraIdx, int presetIdx)
    {
    }

    public void setActiveProgramSelection(int cameraIdx, int presetIdx)
    {
    }

    /**
     * Call this to load presets that were previously saved to disk.
     * If we don't have any existing file, just return some default
     * presets based on the specified camera names in the system.
     *
     * @param cameraNames default camera names to use for creating default presets,
     *                    should we be unable to load the file from disk
     * @return preset names which are one name per line in the preset file
     */
    private List<PtzCameraPresetEntryUi> loadPresetNamesSavedToDisk(List<String> cameraNames)
    {
        try
        {
            // the unique name for this PTZ camera is used to save and restore presets
            logger.info("Loading saved presets for camera from file '{}'", PRESETS_PERSISTENCE_FILE);
            return Files.readAllLines(PRESETS_PERSISTENCE_FILE.toPath()).stream().map(String::trim).map(line -> line.split(","))
                    .map(split -> new PtzCameraPresetEntryUi(
                            split[0].trim(),                    // camera name
                            Integer.parseInt(split[1].trim()),  // camera index
                            split[2].trim(),                    // preset name
                            Integer.parseInt(split[3].trim())   // preset index (for this camera)
                    )).collect(Collectors.toList());
        } catch (Exception e)
        {
            // no file found on disk or no presets
            logger.info("Unable to read presets file on disk, creating default camera presets");
            return IntStream.range(0, cameraNames.size()).boxed().flatMap(cameraIdx ->
                            IntStream.range(0, 5).boxed().map(presetIdx -> new PtzCameraPresetEntryUi(cameraNames.get(cameraIdx), cameraIdx, String.valueOf(presetIdx), presetIdx)))
                    .collect(Collectors.toList());
        }
    }

    /**
     * Fired when we change the name of a preset, updates the file on disk.
     * Note: the current order of presets found in {@link #presets} matches the order in the file.
     */
    private void presetNameChangedSaveToDisk()
    {
        try (PrintWriter out = new PrintWriter(PRESETS_PERSISTENCE_FILE))
        {
            this.presets.stream()
                    .map(p -> p.getCameraName() + "," + p.getCameraIdx() + "," + p.getPresetName() + "," + p.getPresetIdx())
                    .forEach(out::println);
            out.flush();
        } catch (Exception e)
        {
            logger.error("Unable to save presets to disk at {}", PRESETS_PERSISTENCE_FILE, e);
        }
    }

    /**
     * JFormDesigner Auto-Generated Code.
     */
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        JScrollPane scrollPane1 = new JScrollPane();
        panelPresetsHolder = new JPanel();
        JSeparator separator1 = new JSeparator();
        JPanel panel1 = new JPanel();
        JPanel panel3 = new JPanel();
        labelConnectionStatus2 = new JLabel();
        buttonAddNewPreset = new JButton();
        comboBoxCameraNames = new JComboBox<>();
        separator2 = new JSeparator();
        labelPresetEditStatus = new JLabel();
        JPanel panel2 = new JPanel();
        buttonSaveEdits = new JButton();
        buttonDeletePreset = new JButton();
        directionalSwipePanel = new DirectionalTouchUi();
        zoomSlider = new HorizontalZoomTouchUi();

        //======== this ========
        setBackground(Color.black);
        setBorder(null);
        setPreferredSize(new Dimension(324, 100));
        setMinimumSize(new Dimension(324, 100));
        setName("this");
        setLayout(new GridBagLayout());
        ((GridBagLayout)getLayout()).columnWidths = new int[] {0, 0, 140, 0};
        ((GridBagLayout)getLayout()).rowHeights = new int[] {0, 0};
        ((GridBagLayout)getLayout()).columnWeights = new double[] {1.0, 0.0, 0.0, 1.0E-4};
        ((GridBagLayout)getLayout()).rowWeights = new double[] {1.0, 1.0E-4};

        //======== scrollPane1 ========
        {
            scrollPane1.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            scrollPane1.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            scrollPane1.setOpaque(false);
            scrollPane1.setBackground(Color.black);
            scrollPane1.setBorder(null);
            scrollPane1.setName("scrollPane1");

            //======== panelPresetsHolder ========
            {
                panelPresetsHolder.setBorder(null);
                panelPresetsHolder.setBackground(Color.black);
                panelPresetsHolder.setName("panelPresetsHolder");
                panelPresetsHolder.setLayout(new GridLayout());
            }
            scrollPane1.setViewportView(panelPresetsHolder);
        }
        add(scrollPane1, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 10), 0, 0));

        //---- separator1 ----
        separator1.setOrientation(SwingConstants.VERTICAL);
        separator1.setMinimumSize(new Dimension(2, 1));
        separator1.setOpaque(true);
        separator1.setRequestFocusEnabled(false);
        separator1.setVerifyInputWhenFocusTarget(false);
        separator1.setForeground(Color.magenta);
        separator1.setPreferredSize(new Dimension(2, 0));
        separator1.setName("separator1");
        add(separator1, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.VERTICAL,
            new Insets(0, 0, 0, 10), 0, 0));

        //======== panel1 ========
        {
            panel1.setOpaque(false);
            panel1.setBorder(new EmptyBorder(0, 5, 0, 0));
            panel1.setName("panel1");
            panel1.setLayout(new GridBagLayout());
            ((GridBagLayout)panel1.getLayout()).columnWidths = new int[] {0, 0};
            ((GridBagLayout)panel1.getLayout()).rowHeights = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0};
            ((GridBagLayout)panel1.getLayout()).columnWeights = new double[] {1.0, 1.0E-4};
            ((GridBagLayout)panel1.getLayout()).rowWeights = new double[] {0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};

            //======== panel3 ========
            {
                panel3.setOpaque(false);
                panel3.setName("panel3");
                panel3.setLayout(new GridBagLayout());
                ((GridBagLayout)panel3.getLayout()).columnWidths = new int[] {0, 0, 0};
                ((GridBagLayout)panel3.getLayout()).rowHeights = new int[] {0, 0};
                ((GridBagLayout)panel3.getLayout()).columnWeights = new double[] {1.0, 0.0, 1.0E-4};
                ((GridBagLayout)panel3.getLayout()).rowWeights = new double[] {1.0, 1.0E-4};

                //---- labelConnectionStatus2 ----
                labelConnectionStatus2.setText("<html><u>Select Camera for which<br>to add new preset:</u></html>");
                labelConnectionStatus2.setForeground(Color.green);
                labelConnectionStatus2.setBackground(Color.black);
                labelConnectionStatus2.setFont(new Font("Segoe UI", Font.BOLD, 16));
                labelConnectionStatus2.setHorizontalAlignment(SwingConstants.LEFT);
                labelConnectionStatus2.setHorizontalTextPosition(SwingConstants.LEFT);
                labelConnectionStatus2.setVerticalAlignment(SwingConstants.BOTTOM);
                labelConnectionStatus2.setName("labelConnectionStatus2");
                panel3.add(labelConnectionStatus2, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 7), 0, 0));

                //---- buttonAddNewPreset ----
                buttonAddNewPreset.setIcon(new ImageIcon(getClass().getResource("/icons/plus_green_32h.png")));
                buttonAddNewPreset.setFont(new Font("Segoe UI", Font.BOLD, 16));
                buttonAddNewPreset.setHorizontalTextPosition(SwingConstants.LEADING);
                buttonAddNewPreset.setForeground(Color.green);
                buttonAddNewPreset.setPreferredSize(new Dimension(40, 40));
                buttonAddNewPreset.setMinimumSize(new Dimension(40, 40));
                buttonAddNewPreset.setMaximumSize(new Dimension(100, 30));
                buttonAddNewPreset.setBackground(Color.darkGray);
                buttonAddNewPreset.setIconTextGap(6);
                buttonAddNewPreset.setName("buttonAddNewPreset");
                panel3.add(buttonAddNewPreset, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 0), 0, 0));
            }
            panel1.add(panel3, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 10, 0), 0, 0));

            //---- comboBoxCameraNames ----
            comboBoxCameraNames.setModel(new DefaultComboBoxModel<>(new String[] {
                " "
            }));
            comboBoxCameraNames.setForeground(Color.white);
            comboBoxCameraNames.setBackground(Color.black);
            comboBoxCameraNames.setFont(new Font("Ubuntu", Font.BOLD, 18));
            comboBoxCameraNames.setName("comboBoxCameraNames");
            panel1.add(comboBoxCameraNames, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 10, 0), 0, 0));

            //---- separator2 ----
            separator2.setForeground(Color.magenta);
            separator2.setName("separator2");
            panel1.add(separator2, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 10, 0), 0, 0));

            //---- labelPresetEditStatus ----
            labelPresetEditStatus.setText("<html><u>Currently Editing Preset:</u><br>[Nothing selected]</html>");
            labelPresetEditStatus.setForeground(Color.green);
            labelPresetEditStatus.setBackground(Color.black);
            labelPresetEditStatus.setFont(new Font("Segoe UI", Font.BOLD, 16));
            labelPresetEditStatus.setHorizontalAlignment(SwingConstants.LEFT);
            labelPresetEditStatus.setHorizontalTextPosition(SwingConstants.LEFT);
            labelPresetEditStatus.setVerticalAlignment(SwingConstants.BOTTOM);
            labelPresetEditStatus.setName("labelPresetEditStatus");
            panel1.add(labelPresetEditStatus, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 10, 0), 0, 0));

            //======== panel2 ========
            {
                panel2.setOpaque(false);
                panel2.setPreferredSize(new Dimension(100, 40));
                panel2.setMinimumSize(new Dimension(100, 40));
                panel2.setName("panel2");
                panel2.setLayout(new GridLayout(1, 0, 10, 0));

                //---- buttonSaveEdits ----
                buttonSaveEdits.setIcon(new ImageIcon(getClass().getResource("/icons/orange_location_flag_24x24.png")));
                buttonSaveEdits.setText("<html>SAVE<br>EDITS</html>");
                buttonSaveEdits.setFont(new Font("Segoe UI", Font.BOLD, 16));
                buttonSaveEdits.setHorizontalTextPosition(SwingConstants.LEADING);
                buttonSaveEdits.setForeground(new Color(0xcc6600));
                buttonSaveEdits.setPreferredSize(new Dimension(100, 34));
                buttonSaveEdits.setMinimumSize(new Dimension(100, 34));
                buttonSaveEdits.setMaximumSize(new Dimension(100, 30));
                buttonSaveEdits.setBackground(Color.darkGray);
                buttonSaveEdits.setIconTextGap(2);
                buttonSaveEdits.setEnabled(false);
                buttonSaveEdits.setName("buttonSaveEdits");
                panel2.add(buttonSaveEdits);

                //---- buttonDeletePreset ----
                buttonDeletePreset.setIcon(new ImageIcon(getClass().getResource("/icons/red_circle_32x32.png")));
                buttonDeletePreset.setText("<html>DELETE<br>PRESET</html>");
                buttonDeletePreset.setFont(new Font("Segoe UI", Font.BOLD, 16));
                buttonDeletePreset.setHorizontalTextPosition(SwingConstants.LEADING);
                buttonDeletePreset.setForeground(Color.red);
                buttonDeletePreset.setPreferredSize(new Dimension(200, 40));
                buttonDeletePreset.setMinimumSize(new Dimension(150, 40));
                buttonDeletePreset.setMaximumSize(new Dimension(100, 30));
                buttonDeletePreset.setBackground(Color.darkGray);
                buttonDeletePreset.setIconTextGap(2);
                buttonDeletePreset.setEnabled(false);
                buttonDeletePreset.setName("buttonDeletePreset");
                panel2.add(buttonDeletePreset);
            }
            panel1.add(panel2, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 10, 0), 0, 0));

            //---- directionalSwipePanel ----
            directionalSwipePanel.setMinimumSize(new Dimension(300, 150));
            directionalSwipePanel.setPreferredSize(new Dimension(300, 150));
            directionalSwipePanel.setDisplayMessage("PAN & TILT");
            directionalSwipePanel.setName("directionalSwipePanel");
            panel1.add(directionalSwipePanel, new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 10, 0), 0, 0));

            //---- zoomSlider ----
            zoomSlider.setMinimumSize(new Dimension(300, 45));
            zoomSlider.setPreferredSize(new Dimension(300, 45));
            zoomSlider.setName("zoomSlider");
            panel1.add(zoomSlider, new GridBagConstraints(0, 7, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));
        }
        add(panel1, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel panelPresetsHolder;
    private JLabel labelConnectionStatus2;
    private JButton buttonAddNewPreset;
    private JComboBox<String> comboBoxCameraNames;
    private JSeparator separator2;
    private JLabel labelPresetEditStatus;
    private JButton buttonSaveEdits;
    private JButton buttonDeletePreset;
    private DirectionalTouchUi directionalSwipePanel;
    private HorizontalZoomTouchUi zoomSlider;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
