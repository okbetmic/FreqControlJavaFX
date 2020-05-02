package nipel.FreqControl.Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleButton;
import javafx.util.StringConverter;
import nipel.FreqControl.Util.Commands;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public class SingleFreqTabController extends InjectableController implements Initializable {

    @FXML private ToggleButton sf_toggle_button;

    @FXML TextField freqField;
    @FXML ChoiceBox<Integer> freqFactorBox;

    public void sf_toggle_button_action(ActionEvent actionEvent) {
        getSettings();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        freqFactorBox.getItems().setAll(new Integer[]{1, 1000, 1000000});
        freqFactorBox.setValue(freqFactorBox.getItems().get(0));

        Pattern validEditingState = Pattern.compile("-?(([1-9][0-9]*)|0)?(\\.[0-9]*)?");

        UnaryOperator<TextFormatter.Change> filter = c -> {
            String text = c.getControlNewText();
            if (validEditingState.matcher(text).matches()) {
                return c ;
            } else {
                return null ;
            }
        };

        StringConverter<Double> converter = new StringConverter<Double>() {

            @Override
            public Double fromString(String s) {
                if (s.isEmpty() || "-".equals(s) || ".".equals(s) || "-.".equals(s)) {
                    return 0.0 ;
                } else {
                    return Double.valueOf(s);
                }
            }


            @Override
            public String toString(Double d) {
                return d.toString();
            }
        };

        TextFormatter<Double> textFormatter = new TextFormatter<>(converter, 0.0, filter);
        freqField.setTextFormatter(textFormatter);

        freqFactorBox.setConverter(new StringConverter<Integer>() {

            @Override
            public String toString(Integer integer) {
                int f = integer;
                switch (f) {
                    case 1:
                        return "Hz";
                    case 1000:
                        return "kHz";
                    case 1000000:
                        return "MHz";
                    default:
                        return "";
                }
            }

            @Override
            public Integer fromString(String s) {
                return Integer.parseInt(s);
            }
        });
    }

    @Override
    public Commands.deviceSettings getSettings() {
        double freq = Double.parseDouble(freqField.getTextFormatter().getValue().toString());
        int factor = freqFactorBox.getValue();
        deviceSettings.freq = (int) (freq * factor);
        return this.deviceSettings;
    }
}
