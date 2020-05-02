package nipel.FreqControl.Controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Worker;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import nipel.FreqControl.Util.Commands;
import nipel.FreqControl.Util.ConnectionService;
import static nipel.FreqControl.Util.Commands.log;

import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    private double xOffset, yOffset;
    @FXML private AnchorPane main_pane;
    @FXML private GridPane info_table;
    @FXML private Label connected_info_label;
    @FXML private Label mode_info_label;
    @FXML private ProgressBar progress_bar_info;
    @FXML private ChoiceBox device_list;
    @FXML private Button refresh_btn;

    @FXML private SingleFreqTabController singleFreqTabController;
    @FXML private SweepTabController sweepTabController;

    @FXML TabPane tabPane;

    private ConnectionService connection;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        xOffset = yOffset = 0;

        singleFreqTabController.injectMainController(this);

        connection = new ConnectionService();
        connected_info_label.textProperty().bind(connection.messageProperty());
        connection.setOnFailed(workerStateEvent -> log.warning("task failed"));
        connection.setOnCancelled(workerStateEvent -> log.warning("task cancelled"));

        refresh_btn_action(new ActionEvent());
    }

    //buttons
    public void refresh_btn_action(ActionEvent e) {
        Object o = device_list.getSelectionModel().getSelectedItem();
        device_list.getItems().setAll(FXCollections.observableArrayList(connection.getPorts()));
        if (o != null)
            device_list.getSelectionModel().select(o);
        else
            device_list.getSelectionModel().select(0);
    }

    public void connect_btn_action(ActionEvent e) {
        String portDescriptor = (String)device_list.getValue();
        connection.setPortDescriptor(portDescriptor);
        if (connection.getState() == Worker.State.READY || connection.getState() == Worker.State.SUCCEEDED) {
            connection.setControllerAction(Commands.controllerActions.BEGIN_COMM);
            connection.reset();
            connection.start();
            connection.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
                @Override
                public void handle(WorkerStateEvent workerStateEvent) {
                    if (connection.deviceState != connection.deviceState.NC) {
                        connection.setControllerAction(Commands.controllerActions.PING);
                        connection.setOnSucceeded(null);
                        connection.restart();
                    }
                }
            });
        }
    }

    //window control
    public void minimize_btn_action(ActionEvent e) {
        ((Stage)((Button)e.getSource()).getScene().getWindow()).setIconified(true);
    }

    public void close_btn_action(ActionEvent e) {
        connection.down();
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

    public void sendBtnAction(ActionEvent actionEvent) {
        if (connection.deviceState == Commands.deviceStates.SB || connection.deviceState == Commands.deviceStates.SF) {
            switch (tabPane.getSelectionModel().getSelectedIndex()) {
                case 0: // single frequency
                    connection.setControllerAction(Commands.controllerActions.SEND_SF);
                    connection.setSettings(singleFreqTabController.getSettings());
                    break;
                case 1: // sweep
                    break;
                default:
                    break;
            }
        }
    }
}
