package nipel.FreqControl.Controller;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import nipel.FreqControl.Util.ConnectionService;
import static nipel.FreqControl.Util.Commands.log;
import static nipel.FreqControl.Util.Commands.deviceStates;
import static nipel.FreqControl.Util.Commands.controllerActions;

import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    private double xOffset, yOffset;
    @FXML private AnchorPane main_pane;
    @FXML private GridPane info_table;
    @FXML private ChoiceBox<String> device_list;

    @FXML private SingleFreqTabController singleFreqTabController;
    @FXML private SweepTabController sweepTabController;

    @FXML private Label deviceModeLabel;
    @FXML private Label errorLabel;

    //@FXML private Label mode_info_label;
    @FXML private ProgressBar sweepProgressBar;
    @FXML private Button sendBtn;
    @FXML private Button connectBtn;
    @FXML private Button disconnectBtn;


    @FXML TabPane tabPane;

    private ConnectionService connection;

    public MainController() {

    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        xOffset = yOffset = 0;
        singleFreqTabController.injectMainController(this);
        sweepTabController.injectMainController(this);

        connection = new ConnectionService();

        deviceModeLabel.textProperty().bind(connection.getDeviceStateProperty().asString());
        sendBtn.disableProperty().bind(Bindings.and(connection.getDeviceStateProperty().isNotEqualTo(deviceStates.StandBy), connection.getDeviceStateProperty().isNotEqualTo(deviceStates.SingleFreq)));
        sweepProgressBar.disableProperty().bind(connection.getDeviceStateProperty().isNotEqualTo(deviceStates.Sweep));
        sweepProgressBar.progressProperty().bind(connection.progressProperty());
        connectBtn.disableProperty().bind(connection.getDeviceStateProperty().isNotEqualTo(deviceStates.NotConnected));
        disconnectBtn.disableProperty().bind(connectBtn.disabledProperty().not());

        refreshBtnAction(new ActionEvent()); // update ports on startup

        connection.setControllerAction(controllerActions.BEGIN_COMM);
        connection.setOnSucceeded(ee -> { // main loop
            connection.restart();
        });
        connection.setOnFailed(ee -> {
            log.warning("Connection service error: " + connection.exceptionProperty().get().getLocalizedMessage());
            errorLabel.setText(connection.getException().getLocalizedMessage());
            connection.reset();
        });
        connection.setOnScheduled(ee -> errorLabel.setText(""));
    }

    // device control buttons
    public void refreshBtnAction(ActionEvent e) {
        String o = device_list.getSelectionModel().getSelectedItem();
        device_list.getItems().setAll(FXCollections.observableArrayList(connection.getPorts()));
        if (o != null && device_list.getItems().contains(o))
            device_list.getSelectionModel().select(o); // save selected element
        else
            device_list.getSelectionModel().selectLast();
    }

    public void connectBtnAction(ActionEvent e) {
        String portDescriptor = device_list.getValue();
        connection.setPortDescriptor(portDescriptor);
        connection.setControllerAction(controllerActions.BEGIN_COMM);
        if (connection.getState() == Worker.State.READY)
            connection.start();
    }

    public void disconnectBtnAction(ActionEvent e) {
        if(connection.cancel())
            connection.reset();
    }

    public void sendBtnAction(ActionEvent actionEvent) {
        switch (tabPane.getSelectionModel().getSelectedIndex()) {
            case 0: // single frequency
                connection.setControllerAction(controllerActions.SEND_SF);
                connection.setSettings(singleFreqTabController.getSettings());
                break;
            case 1: // sweep
                connection.setControllerAction(controllerActions.SEND_SW);
                connection.setSettings(sweepTabController.getSettings());
                break;
            default:
                break;
        }
    }

    //window control
    public void minimize_btn_action(ActionEvent e) {
        ((Stage)((Button)e.getSource()).getScene().getWindow()).setIconified(true);
    }

    public void close_btn_action(ActionEvent e) {
        disconnectBtnAction(e);
        Platform.exit();
        System.exit(0);
    }

    public void top_on_mouse_pressed(MouseEvent e) {
        Stage sc = (Stage)((AnchorPane)e.getSource()).getScene().getWindow();
        xOffset = sc.getX() - e.getScreenX();
        yOffset = sc.getY() - e.getScreenY();
    }

    public void top_on_mouse_dragged(MouseEvent e) {
        Stage sc = (Stage)((AnchorPane)e.getSource()).getScene().getWindow();
        sc.setX(e.getScreenX() + xOffset);
        sc.setY(e.getScreenY() + yOffset);
    }
}
