package nipel.FreqControl.Controller;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
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
import nipel.FreqControl.Util.Settings;

import static nipel.FreqControl.Util.Commands.log;
import static nipel.FreqControl.Util.Commands.deviceStates;
import static nipel.FreqControl.Util.Commands.controllerActions;

import java.io.*;
import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    private double xOffset, yOffset;
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

    Settings settings;

    public MainController() {
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream("settings.data"));
            settings = (Settings) objectInputStream.readObject();
            objectInputStream.close();
            log.info(String.format("Loaded settings:\n\t%s= %f\n\t%s= %f\n\t%s= %f\n\t%s= %f\n\t%s= %f\n\t%s= %f",
                    "freq", settings.freq,
                    "minF", settings.minF,
                    "maxF", settings.maxF,
                    "totalTime", settings.totalTime,
                    "timeStep",settings.timeStep,
                    "freqStep", settings.freqStep));

        } catch (Exception ex) {
            settings = new Settings();
        }
        connection = new ConnectionService(settings);

        xOffset = yOffset = 0;
        singleFreqTabController.injectMainController(this);
        sweepTabController.injectMainController(this);

        deviceModeLabel.textProperty().bind(connection.getDeviceStateProperty().asString());
        sendBtn.disableProperty().bind(Bindings.and(connection.getDeviceStateProperty().isNotEqualTo(deviceStates.StandBy), connection.getDeviceStateProperty().isNotEqualTo(deviceStates.SingleFreq)));
        sweepProgressBar.disableProperty().bind(connection.getDeviceStateProperty().isNotEqualTo(deviceStates.Sweep));
        sweepProgressBar.progressProperty().bind(connection.progressProperty());
        connectBtn.disableProperty().bind(connection.getDeviceStateProperty().isNotEqualTo(deviceStates.NotConnected));
        disconnectBtn.disableProperty().bind(connectBtn.disabledProperty().not());

        refreshBtnAction(new ActionEvent()); // update ports on startup

        settings.activeAction = controllerActions.BEGIN_COMM;
        settings.userAction = controllerActions.SEND_SF;
        connection.setOnSucceeded(ee -> { // main loop
            connection.restart();
        });
        connection.setOnFailed(ee -> {
            log.warning("Connection service error: " + connection.exceptionProperty().get().getLocalizedMessage());
            errorLabel.setText(connection.getException().getLocalizedMessage());
            connection.reset();
        });
        connection.setOnScheduled(ee -> errorLabel.setText(""));

        tabPane.getSelectionModel().selectedIndexProperty().addListener(e -> updateTabData());
    }

    private void updateTabData() {
        switch (tabPane.getSelectionModel().getSelectedIndex()) {
            case 0 -> {
                settings.userAction = controllerActions.SEND_SF;
                singleFreqTabController.update();
            }
            case 1 -> {
                settings.userAction = controllerActions.SEND_SW;
                sweepTabController.update();
            }
        }
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
        settings.activeAction = controllerActions.BEGIN_COMM;
        if (connection.getState() == Worker.State.READY)
            connection.start();
    }

    public void disconnectBtnAction(ActionEvent e) {
        if(connection.cancel())
            connection.reset();
    }

    public void sendBtnAction(ActionEvent actionEvent) {
        updateTabData();
        settings.activeAction = settings.userAction;
    }

    //window control
    public void minimize_btn_action(ActionEvent e) {
        ((Stage)((Button)e.getSource()).getScene().getWindow()).setIconified(true);
    }

    public void close_btn_action(ActionEvent e) {
        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream("settings.data"));
            objectOutputStream.writeObject(settings);
            objectOutputStream.close();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

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
