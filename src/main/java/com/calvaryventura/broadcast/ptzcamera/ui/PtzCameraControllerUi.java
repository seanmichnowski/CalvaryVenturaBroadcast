package com.calvaryventura.broadcast.ptzcamera.ui;

import com.calvaryventura.broadcast.ptzcamera.ui.preset.PtzCameraPresetEntryUi;
import com.calvaryventura.broadcast.ptzcamera.ui.preset.PtzCameraPresetEntryUi.PtzCameraPresetUiAction;
import com.calvaryventura.broadcast.uiwidgets.DirectionalTouchUi;
import com.calvaryventura.broadcast.uiwidgets.DragAndDropUtility;
import com.calvaryventura.broadcast.uiwidgets.DragScrollListener;
import com.calvaryventura.broadcast.uiwidgets.HorizontalZoomTouchUi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.util.Comparator;
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
    private static final int PTZ_CAMERA_MAX_PRESET_IDX = 100;
    private final Map<String, Boolean> ptzCameraNamesConnectionStatuses = new HashMap<>();
    private final Map<Integer, PtzCameraPresetEntryUi> lastPresetsSelectedPerCameraIndex = new HashMap<>();
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
        this.buttonSaveEdits.addActionListener(e -> this.processSavePresetToPtzCameraButton());

        // initialize the mouse listeners for the drag and drop preset UI functionality
        this.panelPresetsHolder.addMouseListener(this.dh);
        this.panelPresetsHolder.addMouseMotionListener(this.dh);

        // button for adding a new preset
        this.buttonAddNewPreset.addActionListener(e -> {
            final String cameraName = ptzCameraNames.get(this.comboBoxCameraNames.getSelectedIndex());
            final int presetIdx = this.locateNotUtilizedPtzCameraPresetIdx(cameraName);

            // initialize this new preset in the same manner as we would initialize any other preset
            final PtzCameraPresetEntryUi newPreset = new PtzCameraPresetEntryUi(cameraName, this.comboBoxCameraNames.getSelectedIndex(), null, presetIdx);
            this.presets.add(newPreset);
            this.savePresetsToDisk();
            newPreset.initializePresetUserAction(action -> this.processPresetUserAction(newPreset, action));
            newPreset.setPresetEntryEnabled(this.ptzCameraNamesConnectionStatuses.get(cameraName));
            this.redrawAllCameraPresetsIntoScrollableUiPanel();
            newPreset.setEditButtonClicked();

            // scroll the scroll pane all the way to the bottom to view the newest/latest preset
            SwingUtilities.invokeLater(() -> {
                final JScrollBar vertical = this.scrollPanePresets.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            });
        });

        // button for deleting a preset
        this.buttonDeletePreset.addActionListener(e -> {
            this.presets.remove(this.presetSelectedForEditing);
            this.processPresetUserAction(this.presetSelectedForEditing, PtzCameraPresetUiAction.EDIT_BUTTON_DESELECTED);
            this.redrawAllCameraPresetsIntoScrollableUiPanel();
            this.savePresetsToDisk();
            this.lastPresetsSelectedPerCameraIndex.values().remove(this.presetSelectedForEditing);
        });

        // default camera connection state is false/not connected for all cameras specified
        ptzCameraNames.forEach(ptzCameraName -> this.setCameraConnectionStatus(ptzCameraName, false));

        // initialize callbacks from the UI elements, include the selected camera index in the callback
        this.directionalSwipePanel.addXYOutputConsumer((pan, tilt) -> this.callback.panTilt(this.presetSelectedForEditing.getCameraIdx(), pan, tilt));
        this.zoomSlider.addValueChangedConsumer(zoom -> this.callback.zoom(this.presetSelectedForEditing.getCameraIdx(), zoom));

        // initially draw the preset tiles into the UI scroll pane
        new DragScrollListener(this.panelPresetsHolder).hideScrollBars(true);
        this.redrawAllCameraPresetsIntoScrollableUiPanel();

        // TODO this is a start but STILL not working..... and the preset changing order isn't really working either...
        dh.addDragAndDropReorderCallback(dragAndDropActive -> {
            if (dragAndDropActive)
            {
                // for reordering, prevent scroll pane movement and enable the drag and drop
                this.allowPresetPanelScrollPaneMovement(false);
            } else
            {
                // on drag and drop button released, sort the presets based on incrementing Y pixel location and save new order to disk
                this.allowPresetPanelScrollPaneMovement(true);
                this.presets.sort(Comparator.comparingInt(JComponent::getY));
                this.savePresetsToDisk();
            }
        });
    }

    /**
     * Call this whenever we want to update the number of camera presets shown on the UI.
     * All camera presets {@link #presets} are placed into a scrollable panel.
     */
    private void redrawAllCameraPresetsIntoScrollableUiPanel()
    {
        this.panelPresetsHolder.removeAll();
        this.presets.forEach(this.panelPresetsHolder::add);
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
                this.savePresetsToDisk();
                break;

            // EDIT button triggers the right-side UI portion for editing this preset
            case EDIT_BUTTON_SELECTED:
                this.buttonSaveEdits.setEnabled(true);
                this.buttonDeletePreset.setEnabled(true);
                this.directionalSwipePanel.setEnabled(true);
                this.zoomSlider.setEnabled(true);
                this.presets.stream().filter(p -> !p.equals(preset)).forEach(PtzCameraPresetEntryUi::setEditButtonDeselected);
                this.labelPresetEditStatus.setText("<html><u>Currently Editing Preset:</u><br>" + preset.getCameraName() + "/" + preset.getPresetName() + "</html>");
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
                this.lastPresetsSelectedPerCameraIndex.put(preset.getCameraIdx(), preset);
                this.callback.callPressed(preset.getCameraIdx(), preset.getPresetIdx());
                break;
        }
    }

    /**
     * When we are editing a preset, and the user clicks the "SAVE EDITS" button,
     * this gets called to update the 'set' callback, and also to update the status label.
     */
    private void processSavePresetToPtzCameraButton()
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
     * @param cameraIdxPreview index of the active preview camera (or -1 for no preview)
     * @param cameraIdxProgram index of the active program camera (or -1 for no program)
     */
    public void setActivePresetsColored(int cameraIdxPreview, int cameraIdxProgram)
    {
        this.presets.forEach(p -> p.setPresetColor(null)); // reset all colors in all presets initially
        final PtzCameraPresetEntryUi previewPreset = this.lastPresetsSelectedPerCameraIndex.get(cameraIdxPreview);
        if (previewPreset != null)
        {
            previewPreset.setPresetColor(Color.GREEN);
        }
        final PtzCameraPresetEntryUi programPreset = this.lastPresetsSelectedPerCameraIndex.get(cameraIdxProgram);
        if (programPreset != null)
        {
            programPreset.setPresetColor(Color.RED);
        }
    }

    /**
     * @param cameraName specified camera we want a new preset index for
     * @return preset index (up to the max allowed) that is NOT currently utilized for this camera
     */
    private int locateNotUtilizedPtzCameraPresetIdx(String cameraName)
    {
        final List<Integer> currentlyUtilizedPresetIndices = this.presets.stream()
                .filter(p -> p.getCameraName().trim().equalsIgnoreCase(cameraName.trim()))
                .map(PtzCameraPresetEntryUi::getPresetIdx).collect(Collectors.toList());
        return IntStream.range(0, PTZ_CAMERA_MAX_PRESET_IDX).boxed()
                .filter(i -> !currentlyUtilizedPresetIndices.contains(i)).findFirst()
                .orElseThrow(() -> new RuntimeException("Unable to find a free/not utilized preset index for " + cameraName));
    }

    /**
     * @param allowScrolling allows the scroll pane to move in the vertical axis when enabled
     */
    private void allowPresetPanelScrollPaneMovement(boolean allowScrolling)
    {
        this.scrollPanePresets.getVerticalScrollBar().setUnitIncrement(allowScrolling ? 1 : 0);
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
    private synchronized List<PtzCameraPresetEntryUi> loadPresetNamesSavedToDisk(List<String> cameraNames)
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
    private synchronized void savePresetsToDisk()
    {
        try (PrintWriter out = new PrintWriter(new FileWriter(PRESETS_PERSISTENCE_FILE))) //TODO check truncation
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
        scrollPanePresets = new JScrollPane();
        panelPresetsHolder = new JPanel();

        //======== this ========
        setBackground(Color.black);
        setBorder(null);
        setPreferredSize(new Dimension(324, 100));
        setMinimumSize(new Dimension(324, 100));
        setName("this");
        setLayout(new GridBagLayout());
        ((GridBagLayout) getLayout()).columnWidths = new int[]{0, 0, 140, 0};
        ((GridBagLayout) getLayout()).rowHeights = new int[]{0, 0};
        ((GridBagLayout) getLayout()).columnWeights = new double[]{1.0, 0.0, 0.0, 1.0E-4};
        ((GridBagLayout) getLayout()).rowWeights = new double[]{1.0, 1.0E-4};

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
            ((GridBagLayout) panel1.getLayout()).columnWidths = new int[]{0, 0};
            ((GridBagLayout) panel1.getLayout()).rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0};
            ((GridBagLayout) panel1.getLayout()).columnWeights = new double[]{1.0, 1.0E-4};
            ((GridBagLayout) panel1.getLayout()).rowWeights = new double[]{0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};

            //======== panel3 ========
            {
                panel3.setOpaque(false);
                panel3.setName("panel3");
                panel3.setLayout(new GridBagLayout());
                ((GridBagLayout) panel3.getLayout()).columnWidths = new int[]{0, 0, 0};
                ((GridBagLayout) panel3.getLayout()).rowHeights = new int[]{0, 0};
                ((GridBagLayout) panel3.getLayout()).columnWeights = new double[]{1.0, 0.0, 1.0E-4};
                ((GridBagLayout) panel3.getLayout()).rowWeights = new double[]{1.0, 1.0E-4};

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
            comboBoxCameraNames.setModel(new DefaultComboBoxModel<>(new String[]{
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
            directionalSwipePanel.setEnabled(false);
            directionalSwipePanel.setName("directionalSwipePanel");
            panel1.add(directionalSwipePanel, new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 10, 0), 0, 0));

            //---- zoomSlider ----
            zoomSlider.setMinimumSize(new Dimension(300, 45));
            zoomSlider.setPreferredSize(new Dimension(300, 45));
            zoomSlider.setEnabled(false);
            zoomSlider.setName("zoomSlider");
            panel1.add(zoomSlider, new GridBagConstraints(0, 7, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 0), 0, 0));
        }
        add(panel1, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));

        //======== scrollPanePresets ========
        {
            scrollPanePresets.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            scrollPanePresets.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            scrollPanePresets.setOpaque(false);
            scrollPanePresets.setBackground(Color.black);
            scrollPanePresets.setBorder(null);
            scrollPanePresets.setName("scrollPanePresets");

            //======== panelPresetsHolder ========
            {
                panelPresetsHolder.setBorder(null);
                panelPresetsHolder.setBackground(Color.black);
                panelPresetsHolder.setName("panelPresetsHolder");
                panelPresetsHolder.setLayout(new BoxLayout(panelPresetsHolder, BoxLayout.Y_AXIS));
            }
            scrollPanePresets.setViewportView(panelPresetsHolder);
        }
        add(scrollPanePresets, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 10), 0, 0));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JLabel labelConnectionStatus2;
    private JButton buttonAddNewPreset;
    private JComboBox<String> comboBoxCameraNames;
    private JSeparator separator2;
    private JLabel labelPresetEditStatus;
    private JButton buttonSaveEdits;
    private JButton buttonDeletePreset;
    private DirectionalTouchUi directionalSwipePanel;
    private HorizontalZoomTouchUi zoomSlider;
    private JScrollPane scrollPanePresets;
    private JPanel panelPresetsHolder;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
