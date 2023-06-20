package com.calvaryventura.broadcast.ptzcamera.ui;

import com.calvaryventura.broadcast.uiwidgets.DirectionalTouchUi;
import com.calvaryventura.broadcast.uiwidgets.HorizontalTouchSliderUi;

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
    private static final int NUM_PRESETS = 8;
    private final List<PtzCameraUiItem> presets = new ArrayList<>();
    private IPtzCameraUiCallbacks callback;
    private int cameraPresetItemIdx = 0;
    private File saveFile;

    /**
     * MAIN - wait on connect before doing anything else
     */
    public PtzCameraUi()
    {
        this.initComponents();

        // add the desired number of preset boxes to the panel
        IntStream.range(0, NUM_PRESETS).boxed().forEach(i ->
                this.addCameraPresetItem(new PtzCameraUiItem(String.valueOf(i), this::presetNameChangedSaveToDisk)));

        this.zoomSlider.setDisplayMessage("ZOOM", "OUT", "IN", Color.GREEN);
        this.focusSlider.setDisplayMessage("FOCUS", "FAR", "NEAR", Color.GREEN);

        this.initializeCallbacks();
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
     * TODO
     */
    public void setPreviewShowingOnLastItemIfCameraNotMoved()
    {
    }

    /**
     * TODO
     */
    public void setProgramShowingOnLastItemIfCameraNotMoved()
    {
    }

    /**
     * TODO
     */
    public boolean clearPreviewAndProgramHighlights()
    {
        this.disablePresetShowing();
        return true;
    }

    /**
     * Must call this during initialization.
     */
    private void initializeCallbacks()
    {
        this.directionalSwipePanel.addXYOutputConsumer((pan, tilt) -> this.callback.panTilt(pan, tilt));
        this.zoomSlider.addValueChangedConsumer(zoom -> this.callback.zoom(zoom));
        this.focusSlider.addValueChangedConsumer(focus -> this.callback.focus(focus));

        // when either the directional or the zoom is moved, we NO LONGER
        // have any preset, so clear the presets that might be showing in RED
        this.directionalSwipePanel.addXYOutputConsumer((x, y) -> {
            if (x != 0 || y != 0)
            {
                this.disablePresetShowing();
            }
        });
        this.zoomSlider.addValueChangedConsumer(z -> {
            if (z != 0)
            {
                this.disablePresetShowing();
            }
        });
    }

    /**
     * NOTE: deleting of items is done through this panel,
     * i.e. there's no 'deleteItem' in this class
     *
     * @param item the new preset to add
     */
    private void addCameraPresetItem(PtzCameraUiItem item)
    {
        item.setPresetIdx(cameraPresetItemIdx++);
        item.onPresetSetClicked(() -> {
            final boolean success = this.callback.setPressed(item.getPresetIdx());
            if (success)
            {
                item.setContentPanelColor(Color.BLACK);
                final Timer tGreen = new Timer(0, action -> item.setContentPanelColor(Color.GREEN.darker()));
                final Timer tOriginal = new Timer(0, action -> {
                    this.disablePresetShowing();
                    item.setContentPanelColor(Color.RED);
                });
                tGreen.setInitialDelay(500);
                tGreen.setRepeats(false);
                tGreen.start();
                tOriginal.setInitialDelay(1500);
                tOriginal.setRepeats(false);
                tOriginal.start();
            } else
            {
                item.setContentPanelColor(Color.BLACK);
            }
        });

        item.onPresetCalledClicked(() -> {
            final PtzCameraUiItemState originalState = item.getState();
            this.disablePresetShowing(); // reset all items including their states
            if (originalState == PtzCameraUiItemState.NOT_SELECTED)
            {
                final boolean success = this.callback.callPreviewPressed(item.getPresetIdx());
                item.setContentPanelColor(success ? Color.GREEN.darker() : Color.BLACK);
                item.setState(PtzCameraUiItemState.PREVIEW_SHOWING);
            } else
            {
                final boolean success = this.callback.callProgramPressed(item.getPresetIdx());
                item.setContentPanelColor(success ? Color.RED.darker() : Color.BLACK);
                item.setState(PtzCameraUiItemState.PROGRAM_SHOWING);
            }
        });

        this.presets.add(item);
        this.panelPresetsHolder.add(item);
        this.revalidate();
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
                System.out.printf("Cannot create preset save file '%s'\n", this.saveFile.getPath());
                this.saveFile = null;
            }
        }

        // load exiting contents of the file into the presets
        if (this.saveFile != null)
        {
            try
            {
                final List<String> collect = Files.lines(this.saveFile.toPath()).collect(Collectors.toList());
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
                System.out.printf("Cannot read save file\n");
            }
        }
    }

    /**
     * TODO
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
     * If there is any preset which is RED/active/showing, turn off the red color,
     * which is then indicating the camera was moved and the preset is no longer active.
     */
    private void disablePresetShowing()
    {
        this.presets.forEach(p -> {
            p.setContentPanelColor(Color.BLACK);
            p.setState(PtzCameraUiItemState.NOT_SELECTED);
        });
    }

    /**
     * JFormDesigner Auto-Generated Code.
     */
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        panelPresetsHolder = new JPanel();
        labelConnectionStatus = new JLabel();
        directionalSwipePanel = new DirectionalTouchUi();
        zoomSlider = new HorizontalTouchSliderUi();
        focusSlider = new HorizontalTouchSliderUi();

        //======== this ========
        setBackground(Color.black);
        setName("this");
        setLayout(new GridBagLayout());
        ((GridBagLayout)getLayout()).columnWidths = new int[] {0, 0};
        ((GridBagLayout)getLayout()).rowHeights = new int[] {0, 0, 0, 0};
        ((GridBagLayout)getLayout()).columnWeights = new double[] {1.0, 1.0E-4};
        ((GridBagLayout)getLayout()).rowWeights = new double[] {1.0, 0.1, 0.0, 1.0E-4};

        //======== panelPresetsHolder ========
        {
            panelPresetsHolder.setOpaque(false);
            panelPresetsHolder.setBorder(null);
            panelPresetsHolder.setName("panelPresetsHolder");
            panelPresetsHolder.setLayout(new BoxLayout(panelPresetsHolder, BoxLayout.Y_AXIS));

            //---- labelConnectionStatus ----
            labelConnectionStatus.setText("Camera not connected :(");
            labelConnectionStatus.setForeground(Color.red);
            labelConnectionStatus.setBackground(Color.black);
            labelConnectionStatus.setFont(new Font("Segoe UI", Font.BOLD, 14));
            labelConnectionStatus.setHorizontalAlignment(SwingConstants.LEFT);
            labelConnectionStatus.setHorizontalTextPosition(SwingConstants.LEFT);
            labelConnectionStatus.setName("labelConnectionStatus");
            panelPresetsHolder.add(labelConnectionStatus);
        }
        add(panelPresetsHolder, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 15, 0), 0, 0));

        //---- directionalSwipePanel ----
        directionalSwipePanel.setMinimumSize(new Dimension(300, 200));
        directionalSwipePanel.setPreferredSize(new Dimension(300, 200));
        directionalSwipePanel.setDisplayMessage("PAN & TILT");
        directionalSwipePanel.setName("directionalSwipePanel");
        add(directionalSwipePanel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 15, 0), 0, 0));

        //---- zoomSlider ----
        zoomSlider.setMinimumSize(new Dimension(300, 50));
        zoomSlider.setPreferredSize(new Dimension(300, 50));
        zoomSlider.setName("zoomSlider");
        add(zoomSlider, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));

        //---- focusSlider ----
        focusSlider.setMinimumSize(new Dimension(300, 50));
        focusSlider.setPreferredSize(new Dimension(300, 50));
        focusSlider.setName("focusSlider");
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel panelPresetsHolder;
    private JLabel labelConnectionStatus;
    private DirectionalTouchUi directionalSwipePanel;
    private HorizontalTouchSliderUi zoomSlider;
    private HorizontalTouchSliderUi focusSlider;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
