package nipel.FreqControl;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Main extends Application {

    static final int Width = 800;
    static final int Height = 600;

    @Override
    public void start(Stage stage) throws Exception{

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Main.fxml"));
        Parent root = loader.load();

        Scene sc = new Scene(root);
        sc.setFill(Color.TRANSPARENT);
        sc.getStylesheets().add(getClass().getResource("/style/main.css").toExternalForm());

        stage.setTitle("AD9850 Frequency Control");
        stage.setScene(sc);
        stage.setResizable(false);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
