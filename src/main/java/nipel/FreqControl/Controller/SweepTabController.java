package nipel.FreqControl.Controller;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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

public class SweepTabController extends  InjectableController implements Initializable {

    @FXML VBox topSettingBox;
    private ListView<String> radioSettingsList;

    private ArrayList settingsPanes;
    private final ObservableList<Double> freqFactorList = FXCollections.observableArrayList(Arrays.asList(1.0, 1000.0, 1000000.0));
    private final ObservableList<Double> timeFactorList = FXCollections.observableArrayList(Arrays.asList(1.0, 1.0/1000, 1.0/1000000));

    private final StringConverter freqFactorConverter = new StringConverter<Double>() {
        @Override
        public String toString(Double d) {
            String rs = "";
            if (d == 1.0)
                rs = "Hz";
            else if (d == 1000.0)
                rs = "kHz";
            else if (d == 1000000.0)
                rs = "MHz";
            return rs;
        }

        @Override
        public Double fromString(String s) {
            Double rs = 0.0;
            switch (s) {
                case "Hz":
                    rs = 1.0;
                    break;
                case "kHz":
                    rs = 1000.0;
                    break;
                case "MHz":
                    rs = 1000000.0;
                    break;
            }
            return rs;
        }
    };

    private final StringConverter timeFactorConverter = new StringConverter<Double>() {
        @Override
        public String toString(Double d) {
            String rs = "";
            if (d == 1.0)
                rs = "s";
            else if (d == 1.0/1000)
                rs = "ms";
            else if (d == 1.0/1000000)
                rs = "us";
            return rs;
        }

        @Override
        public Double fromString(String s) {
            Double rs = 0.0;
            switch (s) {
                case "s":
                    rs = 1.0;
                    break;
                case "ms":
                    rs = 1.0/1000;
                    break;
                case "us":
                    rs = 1.0/1000000;
                    break;
            }
            return rs;
        }
    };

    private ToggleGroup toggleGroup = new ToggleGroup();
    private ObservableList settingsModeStringList = FXCollections.observableArrayList(Arrays.asList("Total time + Time step",
            "Total time + Frequency step",
            "Time step + Frequency step"));
    private enum settingsModes {
        TsFs, TtFs, TtTs
    }; settingsModes settingsMode;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        settingsPanes = new ArrayList<SettingPane>();
        settingsPanes.addAll(Arrays.asList(
                new SettingPane("Min frequency", freqFactorList, freqFactorConverter),
                new SettingPane("Max frequency", freqFactorList, freqFactorConverter),
                new SettingPane("Total time", timeFactorList, timeFactorConverter),
                new SettingPane("Time step", timeFactorList, timeFactorConverter),
                new SettingPane("Frequency step", freqFactorList, freqFactorConverter),
                new SettingPane("Step count")
                ));
        radioSettingsList = new ListView<String>(settingsModeStringList);
        radioSettingsList.setPrefSize(Region.USE_COMPUTED_SIZE, 75);
        radioSettingsList.setFocusTraversable(false);
        radioSettingsList.setPadding(new Insets(0,0,0,0));
        radioSettingsList.setCellFactory(param -> new RadioListCell());
        toggleGroup.selectedToggleProperty().addListener((observableValue, toggle, t1) -> {
            String selected = ((RadioButton)t1).getText();
            if (selected == settingsModeStringList.get(0))
                settingsMode = settingsModes.TtTs;
            else if (selected == settingsModeStringList.get(1))
                settingsMode = settingsModes.TtFs;
            else if (selected == settingsModeStringList.get(2))
                settingsMode = settingsModes.TsFs;
            updateDeviceSettings();
        });
        settingsMode = settingsModes.TsFs;
        topSettingBox.setSpacing(5.0);
        topSettingBox.getChildren().addAll(settingsPanes);
        topSettingBox.getChildren().add(radioSettingsList);
        topSettingBox.setPadding(new Insets(10, 10, 10, 10));
    }

    private class SettingPane extends AnchorPane {
        public Label settingLabel;
        public TextField settingField;
        public ChoiceBox<Double> settingFactorBox;

        public void activate(boolean en) {
            settingField.setDisable(!en);
            settingFactorBox.setDisable(!en);
        }

        public Double getDoubleValueSafe() {
            Double rs;
            rs = Double.parseDouble(settingField.textProperty().get()) * settingFactorBox.getValue();
            return rs;
        }
        public void setDoubleValueSafe(Double d) {
            settingField.textProperty().removeListener(listener);
            if (d != Double.POSITIVE_INFINITY && d != Double.NEGATIVE_INFINITY) {
                if (!settingFactorBox.getItems().isEmpty())
                    settingField.setText(((Double) (d / settingFactorBox.getValue())).toString());
                else
                    settingField.setText(d.toString());
            }
            settingField.textProperty().addListener(listener);
        }

        ChangeListener listener = new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
                updateDeviceSettings();
            }
        };


        public SettingPane(String text) {
            super();
            settingLabel = new Label(text);
            settingField = new TextField();

            if (text == "Step count")
                settingField.setDisable(true);

            settingField.setTextFormatter(DoubleFieldValidator.getDoubleTextFormatter());
            settingField.textProperty().addListener(listener);

            settingLabel.setMaxHeight(Double.MAX_VALUE);
            setTopAnchor(settingLabel, 0.0);
            setBottomAnchor(settingLabel, 0.0);
            setLeftAnchor(settingLabel, 0.0);
            settingLabel.setAlignment(Pos.CENTER);

            settingField.setMaxHeight(Double.MAX_VALUE);
            setTopAnchor(settingField, 0.0);
            setBottomAnchor(settingField, 0.0);
            setRightAnchor(settingField, 65.0);
            settingLabel.setAlignment(Pos.CENTER);

            settingFactorBox = new ChoiceBox();
            settingFactorBox.setDisable(true);

            getChildren().addAll(settingLabel, settingField);
            setPadding(new Insets(3, 3, 5, 3));
            setPrefSize(310, USE_COMPUTED_SIZE);
        }

        public SettingPane(String text, ObservableList els, StringConverter converter) {
            this(text);
            //elements
            settingFactorBox = new ChoiceBox();
            settingFactorBox.getItems().addAll(els);
            settingFactorBox.setConverter(converter);
            settingFactorBox.setValue(1.0);
            settingFactorBox.setDisable(false);
            //settingFactorBox.getSelectionModel().selectedIndexProperty().addListener((e) -> updateDeviceSettings());
            settingFactorBox.setOnAction((e) -> updateDeviceSettings());
            //ui
            settingFactorBox.setPrefSize(50, 25);
            settingFactorBox.setMaxHeight(Double.MAX_VALUE);
            setTopAnchor(settingFactorBox, 0.0);
            setBottomAnchor(settingFactorBox, 0.0);
            setRightAnchor(settingFactorBox, 0.0);
            getChildren().add(settingFactorBox);
        }
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

    private void updateDeviceSettings() {
        if (settingsPanes.size() < 6)
            return;
        //extract ass possible
        Double minF, maxF, totalTime, timeStep, freqStep, stepCnt = 0.0;
        minF = ((SettingPane)settingsPanes.get(0)).getDoubleValueSafe(); // Hz
        maxF = ((SettingPane)settingsPanes.get(1)).getDoubleValueSafe(); // Hz
        totalTime = ((SettingPane)settingsPanes.get(2)).getDoubleValueSafe(); // s
        timeStep = ((SettingPane)settingsPanes.get(3)).getDoubleValueSafe(); // !!! s -> ms !!!
        freqStep = ((SettingPane)settingsPanes.get(4)).getDoubleValueSafe(); // Hz

        switch (settingsMode) {
            case TtTs: { // total time + time step
                ((SettingPane) settingsPanes.get(2)).activate(true);
                ((SettingPane) settingsPanes.get(3)).activate(true);
                ((SettingPane) settingsPanes.get(4)).activate(false);

                if (timeStep <= 0 || totalTime <= 0)
                    return;
                stepCnt = totalTime / timeStep;
                freqStep = (maxF - minF) / stepCnt;
                ((SettingPane) settingsPanes.get(4)).setDoubleValueSafe(freqStep);
                break;
            }
            case TtFs: { // total time + freq step
                ((SettingPane) settingsPanes.get(2)).activate(true);
                ((SettingPane) settingsPanes.get(4)).activate(true);
                ((SettingPane) settingsPanes.get(3)).activate(false);

                if (freqStep <= 0 || totalTime <= 0)
                    return;
                stepCnt = (maxF - minF) / freqStep;
                timeStep = totalTime / stepCnt;
                ((SettingPane) settingsPanes.get(3)).setDoubleValueSafe(timeStep);

                break;
            }
            case TsFs: { // time step + frequency step
                ((SettingPane) settingsPanes.get(3)).activate(true);
                ((SettingPane) settingsPanes.get(4)).activate(true);
                ((SettingPane) settingsPanes.get(2)).activate(false);

                if (timeStep <= 0 || freqStep <= 0)
                    return;
                stepCnt = (maxF - minF) / freqStep;
                totalTime = timeStep * stepCnt;
                ((SettingPane) settingsPanes.get(2)).setDoubleValueSafe(totalTime);
                break;
            }
        }
        ((SettingPane) settingsPanes.get(5)).setDoubleValueSafe(stepCnt);
        //simple validate
        /*if ((totalTime < 1) || (freqStep < 1) || timeStep < 1) {
            System.out.println("invalid");
            return;
        }*/

        deviceSettings.minF = (int) Math.round(minF);
        deviceSettings.maxF = (int) Math.round(maxF);
        deviceSettings.timeStep = (int) Math.round(timeStep * 1000);
        deviceSettings.freqStep = (int) Math.round(freqStep);
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
