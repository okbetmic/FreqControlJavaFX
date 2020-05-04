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
import nipel.FreqControl.Util.DoubleFieldValidator;

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

        freqField.setTextFormatter(DoubleFieldValidator.getDoubleTextFormatter());

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
        if (sf_toggle_button.isSelected()) {
            double freq = Double.parseDouble(freqField.getTextFormatter().getValue().toString());
            int factor = freqFactorBox.getValue();
            deviceSettings.freq = (int) (freq * factor);
        } else {
            deviceSettings.freq = 0;
        }
        return this.deviceSettings;
    }
}
