package nipel.FreqControl.Controller;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.util.StringConverter;
import nipel.FreqControl.Util.DoubleFieldValidator;

public class SettingPane extends AnchorPane {
    private Label settingLabel;
    private TextField settingField;
    private ChoiceBox<Double> settingFactorBox;

    private Runnable updateRunnable;
    ChangeListener<String> settingFieldChangeListener = new ChangeListener<>() {
        @Override
        public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
            updateRunnable.run();
        }
    };

    public void active(boolean en) {
        settingField.setDisable(!en);
        settingFactorBox.setDisable(!en);
    }

    public Double getDoubleValue() {
        return Double.parseDouble(settingField.textProperty().get()) * (settingFactorBox.isDisabled() ? 1 :settingFactorBox.getValue());
    }

    public void setValue(double d) {
        if (!settingFactorBox.isDisabled()) // if this field has a multipliers
        {
            int i;
            for (i = 0; i <  settingFactorBox.getItems().size(); i++)
                if (d / settingFactorBox.getItems().get(i) < 1000)
                    break;
            settingFactorBox.selectionModelProperty().get().select(i);
            settingField.setText(((Double) (d / settingFactorBox.getValue())).toString()); // we need to divide by it
        }
        else
            settingField.setText(String.valueOf(d));
    }

    public void setChangeListener(Runnable r) {
        this.updateRunnable = r;
    }

    private void update() {
        updateRunnable.run();
    }

    public SettingPane(String text) {
        super();
        settingLabel = new Label(text);
        settingField = new TextField();

        if (text.equals("Step count"))
            settingField.setDisable(true);

        settingField.setTextFormatter(DoubleFieldValidator.getDoubleTextFormatter());
        settingField.textProperty().addListener(settingFieldChangeListener);

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

        settingFactorBox = new ChoiceBox<>();
        settingFactorBox.setDisable(true);

        getChildren().addAll(settingLabel, settingField);
        setPadding(new Insets(3, 3, 5, 3));
        setPrefSize(310, USE_COMPUTED_SIZE);
    }

    public SettingPane(String text, ObservableList<Double> els, SettingType type, Runnable r) {
        this(text);
        //elements
        this.updateRunnable = r;
        settingFactorBox = new ChoiceBox<>();
        settingFactorBox.getItems().addAll(els);
        settingFactorBox.setConverter(switch (type) {
            case Time -> timeFactorConverter;
            case Frequency -> freqFactorConverter;
        });
        settingFactorBox.setValue(1.0);
        settingFactorBox.setDisable(false);
        settingFactorBox.setOnAction(e -> {  updateRunnable.run(); });
        //ui
        settingFactorBox.setPrefSize(50, 25);
        settingFactorBox.setMaxHeight(Double.MAX_VALUE);
        setTopAnchor(settingFactorBox, 0.0);
        setBottomAnchor(settingFactorBox, 0.0);
        setRightAnchor(settingFactorBox, 0.0);
        getChildren().add(settingFactorBox);
    }

    private final StringConverter<Double> freqFactorConverter = new StringConverter<>() {
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
            return switch (s) {
                case "Hz" -> 1.0;
                case "kHz" -> 1000.0;
                case "MHz" -> 1000000.0;
                default -> 0.0;
            };
        }
    };

    public enum SettingType {
        Time, Frequency;
    }

    private final StringConverter<Double> timeFactorConverter = new StringConverter<>() {
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
            return switch (s) {
                case "s" -> 1.0;
                case "ms" -> 1.0/1000;
                case "us" -> 1.0/1000000;
                default -> throw new IllegalStateException("Unexpected value: " + s);
            };
        }
    };
}

