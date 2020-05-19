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

    public void update() {
        if (sf_toggle_button.isSelected()) {
            double freq = Double.parseDouble(freqField.getTextFormatter().getValue().toString());
            int factor = freqFactorBox.getValue();
            mainController.settings.freq = (int) (freq * factor);
        } else {
            mainController.settings.freq = 0;
        }
    }

    @Override
    public void injectMainController(MainController mainController) {
        super.injectMainController(mainController);
        freqField.setText(Double.toString(mainController.settings.freq / freqFactorBox.getValue()));
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        freqFactorBox.getItems().setAll(new Integer[]{1, 1000, 1000000});
        freqFactorBox.setValue(freqFactorBox.getItems().get(0));

        freqField.setTextFormatter(DoubleFieldValidator.getDoubleTextFormatter());

        freqFactorBox.setConverter(new StringConverter<Integer>() {
            @Override
            public String toString(Integer integer) {
                return switch (integer) {
                    case 1 -> "Hz";
                    case 1000 -> "kHz";
                    case 1000000 -> "MHz";
                    default -> "";
                };
            }

            @Override
            public Integer fromString(String s) {
                return Integer.parseInt(s);
            }
        });
    }
}
