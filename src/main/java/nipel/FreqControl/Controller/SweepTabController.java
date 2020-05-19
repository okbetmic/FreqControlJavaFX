package nipel.FreqControl.Controller;

import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import nipel.FreqControl.Util.DoubleFieldValidator;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ResourceBundle;

import static nipel.FreqControl.Controller.SettingPane.SettingType;

public class SweepTabController extends  InjectableController implements Initializable {

    @FXML VBox topSettingBox;

    private final ObservableList<Double> freqFactorList = FXCollections.observableArrayList(Arrays.asList(1.0, 1000.0, 1000000.0));
    private final ObservableList<Double> timeFactorList = FXCollections.observableArrayList(Arrays.asList(1.0/1000, 1.0));

    private final ToggleGroup toggleGroup = new ToggleGroup();
    private final ObservableList<String> settingsModeStringList = FXCollections.observableArrayList(Arrays.asList("Total time + Time step",
            "Total time + Frequency step",
            "Time step + Frequency step"));
    private enum settingsModes {
        TsFs, TtFs, TtTs
    }
    settingsModes settingsMode;

    private Runnable updateValuesRunnable = () -> update();

    ArrayList<SettingPane> settingsPanes = new ArrayList<>(Arrays.asList(
            new SettingPane("Min frequency", freqFactorList, SettingType.Frequency, updateValuesRunnable),
            new SettingPane("Max frequency", freqFactorList, SettingType.Frequency, updateValuesRunnable),
            new SettingPane("Total time", timeFactorList, SettingType.Time, updateValuesRunnable),
            new SettingPane("Time step", timeFactorList, SettingType.Time, updateValuesRunnable),
            new SettingPane("Frequency step", freqFactorList, SettingType.Frequency, updateValuesRunnable),
            new SettingPane("Step count")));

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ListView<String> radioSettingsList = new ListView<>(settingsModeStringList);
        radioSettingsList.setPrefSize(Region.USE_COMPUTED_SIZE, 75);
        radioSettingsList.setFocusTraversable(false);
        radioSettingsList.setPadding(new Insets(0,0,0,0));
        radioSettingsList.setCellFactory(param -> new RadioListCell());
        toggleGroup.selectedToggleProperty().addListener((observableValue, toggle, t1) -> {
            String selected = ((RadioButton)t1).getText();
            if (selected.equals(settingsModeStringList.get(0)))
                settingsMode = settingsModes.TtTs;
            else if (selected.equals(settingsModeStringList.get(1)))
                settingsMode = settingsModes.TtFs;
            else if (selected.equals(settingsModeStringList.get(2)))
                settingsMode = settingsModes.TsFs;
            changeEnabledFields();
        });
        settingsMode = settingsModes.TsFs;
        topSettingBox.setSpacing(5.0);
        topSettingBox.getChildren().addAll(settingsPanes);
        topSettingBox.getChildren().add(radioSettingsList);
        topSettingBox.setPadding(new Insets(10, 10, 10, 10));
    }



    @Override
    public void injectMainController(MainController mainController) {
        super.injectMainController(mainController);
        settingsPanes.get(0).setValue(mainController.settings.minF);
        settingsPanes.get(1).setValue(mainController.settings.maxF);
        settingsPanes.get(3).setValue(mainController.settings.timeStep);
        settingsPanes.get(4).setValue(mainController.settings.freqStep);
    }

    private class RadioListCell extends ListCell<String> {

        public RadioListCell() {
            super();
        }

        @Override
        protected void updateItem(String obj, boolean empty) {
            super.updateItem(obj, empty);
            if (empty) {
                setText(null); setGraphic(null);
                setGraphic(null);
            } else {
                RadioButton radioButton = new RadioButton(obj);
                radioButton.setToggleGroup(toggleGroup);
                radioButton.setSelected(true);
                setGraphic(radioButton);
            }
        }
    }

    private void changeEnabledFields() {
        if (settingsPanes.get(1).getDoubleValue() - settingsPanes.get(0).getDoubleValue() > 0)
            switch (settingsMode) {
                case TtTs -> { // total time + time step
                    settingsPanes.get(2).active(true);
                    settingsPanes.get(3).active(true);
                    settingsPanes.get(4).active(false);
                }
                case TtFs -> { // total time + freq step
                    settingsPanes.get(2).active(true);
                    settingsPanes.get(4).active(true);
                    settingsPanes.get(3).active(false);
                }
                case TsFs -> { // time step + frequency step
                    settingsPanes.get(3).active(true);
                    settingsPanes.get(4).active(true);
                    settingsPanes.get(2).active(false);
                }
            }
        else {
            settingsPanes.get(3).active(false);
            settingsPanes.get(4).active(false);
            settingsPanes.get(2).active(false);
        }

    }

    public void update() {
        if (settingsPanes.size() < 6)
            return;

        // here we get try to get values
        double minF, maxF, totalTime, timeStep, freqStep, stepCnt = 0;
        minF = settingsPanes.get(0).getDoubleValue(); // Hz
        maxF = settingsPanes.get(1).getDoubleValue(); // Hz
        totalTime = settingsPanes.get(2).getDoubleValue(); // s
        timeStep = settingsPanes.get(3).getDoubleValue(); // ms
        freqStep = settingsPanes.get(4).getDoubleValue(); // Hz

        if ((maxF - minF) <= 0)
            return;

        switch (settingsMode) {
            case TtTs -> { // total time + time step
                if (timeStep <= 0 || totalTime <= 0)
                    return;
                stepCnt = totalTime / timeStep;
                freqStep = (maxF - minF) / stepCnt;
                settingsPanes.get(4).setValue(freqStep);
            }
            case TtFs -> { // total time + freq step
                if (freqStep <= 0 || totalTime <= 0)
                    return;
                stepCnt = (maxF - minF) / freqStep;
                timeStep = totalTime / stepCnt;
                settingsPanes.get(3).setValue(timeStep);
            }
            case TsFs -> { // time step + frequency step
                if (timeStep <= 0 || freqStep <= 0)
                    return;
                stepCnt = (maxF - minF) / freqStep;
                totalTime = timeStep * stepCnt;
                settingsPanes.get(2).setValue(totalTime);
            }
        }
        System.out.println("step cnt" + stepCnt);
        settingsPanes.get(5).setValue(stepCnt);

        // now check the calculations
        //if ()

        // finally pass data settings to main controller
        System.out.println(timeStep);
        mainController.settings.minF = minF;
        mainController.settings.maxF = maxF;
        mainController.settings.timeStep = timeStep; // s -> ms
        mainController.settings.freqStep = freqStep;
        mainController.settings.totalTime = totalTime;
    }

    /*@FXML private TextField minFreqField;
    @FXML private TextField maxFreqField;

    @FXML private TextField timeStepField;
    @FXML private TextField freqStepField;

    @FXML private TextField stepsCountField;
    @FXML private TextField totalTimeField;

    @FXML private VBox sweepSettingsContainer;
    @FXML private ListView<String> radioSettingsList;

    private final ToggleGroup toggleGroup = new ToggleGroup();
    private final ObservableList settingsType = FXCollections.observableArrayList(Arrays.asList("Total time + Time step",
            "Total time + Frequency step",
            "Time step + Frequency step"));
    private boolean limitsSet;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        limitsSet = true;

        minFreqField.setTextFormatter(DoubleFieldValidator.getDoubleTextFormatter());
        maxFreqField.setTextFormatter(DoubleFieldValidator.getDoubleTextFormatter());
        totalTimeField.setTextFormatter(DoubleFieldValidator.getDoubleTextFormatter());
        timeStepField.setTextFormatter(DoubleFieldValidator.getDoubleTextFormatter());
        freqStepField.setTextFormatter(DoubleFieldValidator.getDoubleTextFormatter());
        stepsCountField.setTextFormatter(DoubleFieldValidator.getDoubleTextFormatter());

        setSettingsEnabled("0", "0", "0");
        minFreqField.textProperty().addListener(((observableValue, s, t1) -> setSettingsEnabled(t1, maxFreqField.textProperty().get(), (toggleGroup.getSelectedToggle() != null) ? ((RadioButton)toggleGroup.getSelectedToggle()).getText() : "")));
        maxFreqField.textProperty().addListener(((observableValue, s, t1) -> setSettingsEnabled(minFreqField.textProperty().get(), t1, (toggleGroup.getSelectedToggle() != null) ? ((RadioButton)toggleGroup.getSelectedToggle()).getText() : "")));

        stepsCountField.setDisable(true);

        radioSettingsList.setItems(settingsType);
        radioSettingsList.setCellFactory(param -> new RadioListCell());
        toggleGroup.selectedToggleProperty().addListener((observableValue, toggle, t1) -> {
            setSettingsEnabled(minFreqField.getTextFormatter().getValue().toString(), maxFreqField.getTextFormatter().getValue().toString(), ((RadioButton)t1).getText());
        });

        ChangeListener sweepCalculator = new ChangeListener() {
            @Override
            public void changed(ObservableValue observableValue, Object o, Object t1) {
                try {
                    double minF = Double.parseDouble(minFreqField.getTextFormatter().getValue().toString());
                    double maxF = Double.parseDouble(maxFreqField.getTextFormatter().getValue().toString());
                    double totalTime = Double.parseDouble(totalTimeField.getTextFormatter().getValue().toString());
                    double timeStep = Double.parseDouble(timeStepField.getTextFormatter().getValue().toString());
                    double freqStep = Double.parseDouble(freqStepField.getTextFormatter().getValue().toString());

                    double stepCnt = 0;

                    if (freqStepField.isDisabled()) { // total time + time step
                        stepCnt = (totalTime * 1000000) / timeStep;
                        freqStep = (maxF - minF) / stepCnt;
                        freqStepField.setText(Double.toString(freqStep));
                    } else if (timeStepField.isDisabled()) { // total time + frequency step
                        stepCnt = (maxF - minF) / freqStep;
                        timeStep = totalTime / stepCnt;
                        timeStepField.setText(Double.toString(timeStep));
                    } else if (totalTimeField.isDisabled()) { // time step + frequency step
                        stepCnt = (maxF - minF) / freqStep;
                        totalTime = timeStep * stepCnt;
                        totalTimeField.setText(Double.toString(totalTime));
                    }

                    stepsCountField.setText(Double.toString(stepCnt));

                    deviceSettings.beginFreq = (int) minF;
                    deviceSettings.endFreq = (int) maxF;
                    deviceSettings.timeStep = (int) timeStep;
                    deviceSettings.totalTime = (int) totalTime;

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };

        totalTimeField.textProperty().addListener(sweepCalculator);
        timeStepField.textProperty().addListener(sweepCalculator);
        freqStepField.textProperty().addListener(sweepCalculator);
    }

    public void setSettingsEnabled(String mint, String maxt, String selected) {
        double min = Double.parseDouble(mint);
        double max = Double.parseDouble(maxt);
        boolean rs = min >= 0 && max > min;
        if (rs) {
            if (selected == settingsType.get(0).toString()) { // total time + time step
                totalTimeField.setDisable(false);
                timeStepField.setDisable(false);
                freqStepField.setDisable(true);
            }
            else if (selected == settingsType.get(1).toString()) { // total time + frequency step
                totalTimeField.setDisable(false);
                timeStepField.setDisable(true);
                freqStepField.setDisable(false);
            } else if (selected == settingsType.get(2).toString()) { // time step + frequency step
                totalTimeField.setDisable(true);
                timeStepField.setDisable(false);
                freqStepField.setDisable(false);
            }
        } else {
            totalTimeField.setDisable(true);
            timeStepField.setDisable(true);
            freqStepField.setDisable(true);
        }
    }

    */
}
