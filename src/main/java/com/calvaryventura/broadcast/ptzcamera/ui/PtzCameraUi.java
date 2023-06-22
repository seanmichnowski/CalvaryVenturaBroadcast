package com.calvaryventura.broadcast.ptzcamera.ui;

import com.calvaryventura.broadcast.uiwidgets.DirectionalTouchUi;
import com.calvaryventura.broadcast.uiwidgets.HorizontalZoomTouchUi;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This is the top-level UI for controlling a SINGLE PTZ camera.
 * See the JFormDesigner layout for all the components involved.
 */
public class PtzCameraUi extends JPanel
{
    private static final int NUM_PRESETS = 6;
    private final List<PtzCameraUiItem> presets = new ArrayList<>();
    private int lastClickedPresetIdx = -1;
    private IPtzCameraUiCallbacks callback;
    private File saveFile;

    // complicated layout descriptor for adding  PTZ camera preset items into their layout panel, but it does make them stack nice and allows vertical space between entries to grow and fill
    private static final GridBagConstraints GRID_BAG_CONSTRAINTS = new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, NUM_PRESETS,
            1.0, 1.0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);

    /**
     * MAIN - wait on connect before doing anything else
     */
    public PtzCameraUi()
    {
        this.initComponents();

        // add the desired number of preset boxes to the panel
        IntStream.range(0, NUM_PRESETS).boxed().forEach(i ->
        {
            // create the new UI item for this preset
            final PtzCameraUiItem item = new PtzCameraUiItem(i, this::presetNameChangedSaveToDisk);

            // set preset action updates the status label text for a little while
            item.onPresetSetClicked(() -> {
                final boolean success = this.callback.setPressed(item.getPresetIdx());
                this.lastClickedPresetIdx = success ? item.getPresetIdx() : -1;
                final String originalLabelStatus = this.labelConnectionStatus.getText();
                this.labelConnectionStatus.setText("<html><font color='white'>Set preset: <u>" + item.getPresetName()
                        + "</u> " + (success ? "OK" : "FAILED") + "</font></html>");
                final Timer tOriginal = new Timer(0, action -> this.labelConnectionStatus.setText(originalLabelStatus));
                tOriginal.setInitialDelay(2000);
                tOriginal.setRepeats(false);
                tOriginal.start();
            });

            // call/GoTo preset action (returns a boolean indicating successful camera movement)
            item.onPresetCalledClicked(() -> {
                final boolean success = this.callback.callPressed(item.getPresetIdx());
                this.lastClickedPresetIdx = success ? item.getPresetIdx() : -1;
                return success;
            });

            // add to UI parent panel
            this.presets.add(item);
            this.panelPresetsHolder.add(item, GRID_BAG_CONSTRAINTS);
        });

        // initialize callbacks from the UI elements
        this.directionalSwipePanel.addXYOutputConsumer((pan, tilt) -> this.callback.panTilt(pan, tilt));
        this.zoomSlider.addValueChangedConsumer(zoom -> this.callback.zoom(zoom));
    }

    /**
     * @param callback mechanism to pass UI events OUT of this class
     */
    public void setCallback(IPtzCameraUiCallbacks callback)
    {
        this.callback = callback;
    }

    /**
     * @param connected indication if we are connected to the camera's VISCA control port
     */
    public void setCameraConnectionStatus(boolean connected)
    {
        this.labelConnectionStatus.setText(connected ? "Camera connected :)" : "Camera not connected :(");
        this.labelConnectionStatus.setForeground(connected ? Color.GREEN : Color.RED);
    }

    /**
     * Sets the display background color or any preset that was selected.
     * @param backgroundColor color to display
     */
    public void setActivePresetBackgroundColor(Color backgroundColor)
    {
        this.presets.forEach(p -> p.setContentPanelColor(null));
        if (this.lastClickedPresetIdx >= 0 && this.lastClickedPresetIdx < this.presets.size())
        {
            this.presets.get(this.lastClickedPresetIdx).setContentPanelColor(backgroundColor);
        }
    }

    /**
     * Call this to load presets that were previously saved to disk.
     */
    public void loadPresetsSavedToDisk()
    {
        final String name = this.getName(); // unique name for associating this bank of camera presets
        this.saveFile = new File(System.getProperty("user.home") + "/camera_presets_" + name + ".txt");
        if (!this.saveFile.isFile())
        {
            try
            {
                this.saveFile.createNewFile();
            } catch (IOException e)
            {
                e.printStackTrace();
                this.saveFile = null;
            }
        }

        // load exiting contents of the file into the presets
        if (this.saveFile != null)
        {
            try
            {
                final List<String> collect = Files.readAllLines(this.saveFile.toPath());
                if (!collect.isEmpty())
                {
                    final String[] presetNames = collect.get(0).split(",");
                    if (presetNames.length == NUM_PRESETS)
                    {
                        SwingUtilities.invokeLater(() -> IntStream.range(0, presetNames.length).boxed()
                                .forEach(i -> this.presets.get(i).setPresetName(presetNames[i])));
                    }
                }
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    /**
     * Fired when we change the name of a preset, updates the file on disk.
     */
    private void presetNameChangedSaveToDisk()
    {
        if (this.saveFile != null)
        {
            final String lineToWrite = this.presets.stream().map(PtzCameraUiItem::getPresetName).collect(Collectors.joining(","));
            try (PrintWriter out = new PrintWriter(this.saveFile)) {
                out.println(lineToWrite);
                out.flush();
            } catch (FileNotFoundException e)
            {
                e.printStackTrace();
            }
        }
    }

    /**
     * JFormDesigner Auto-Generated Code.
     */
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        labelConnectionStatus = new JLabel();
        panelPresetsHolder = new JPanel();
        directionalSwipePanel = new DirectionalTouchUi();
        zoomSlider = new HorizontalZoomTouchUi();

        //======== this ========
        setBackground(Color.black);
        setName("this");
        setLayout(new GridBagLayout());
        ((GridBagLayout)getLayout()).columnWidths = new int[] {0, 0};
        ((GridBagLayout)getLayout()).rowHeights = new int[] {0, 0, 0, 0, 0};
        ((GridBagLayout)getLayout()).columnWeights = new double[] {1.0, 1.0E-4};
        ((GridBagLayout)getLayout()).rowWeights = new double[] {0.0, 1.0, 0.1, 0.0, 1.0E-4};

        //---- labelConnectionStatus ----
        labelConnectionStatus.setText("Camera not connected :(");
        labelConnectionStatus.setForeground(Color.red);
        labelConnectionStatus.setBackground(Color.black);
        labelConnectionStatus.setFont(new Font("Segoe UI", Font.BOLD, 14));
        labelConnectionStatus.setHorizontalAlignment(SwingConstants.LEFT);
        labelConnectionStatus.setHorizontalTextPosition(SwingConstants.LEFT);
        labelConnectionStatus.setName("labelConnectionStatus");
        add(labelConnectionStatus, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 10, 0), 0, 0));

        //======== panelPresetsHolder ========
        {
            panelPresetsHolder.setOpaque(false);
            panelPresetsHolder.setBorder(null);
            panelPresetsHolder.setName("panelPresetsHolder");
            panelPresetsHolder.setLayout(new GridBagLayout());
            ((GridBagLayout)panelPresetsHolder.getLayout()).columnWidths = new int[] {0, 0};
            ((GridBagLayout)panelPresetsHolder.getLayout()).rowHeights = new int[] {0, 0};
            ((GridBagLayout)panelPresetsHolder.getLayout()).columnWeights = new double[] {1.0, 1.0E-4};
            ((GridBagLayout)panelPresetsHolder.getLayout()).rowWeights = new double[] {1.0, 1.0E-4};
        }
        add(panelPresetsHolder, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 10, 0), 0, 0));

        //---- directionalSwipePanel ----
        directionalSwipePanel.setMinimumSize(new Dimension(300, 200));
        directionalSwipePanel.setPreferredSize(new Dimension(300, 200));
        directionalSwipePanel.setDisplayMessage("PAN & TILT");
        directionalSwipePanel.setName("directionalSwipePanel");
        add(directionalSwipePanel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 10, 0), 0, 0));

        //---- zoomSlider ----
        zoomSlider.setMinimumSize(new Dimension(300, 50));
        zoomSlider.setPreferredSize(new Dimension(300, 50));
        zoomSlider.setName("zoomSlider");
        add(zoomSlider, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JLabel labelConnectionStatus;
    private JPanel panelPresetsHolder;
    private DirectionalTouchUi directionalSwipePanel;
    private HorizontalZoomTouchUi zoomSlider;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
