package hal.amorce_projet_gd;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class PortefeuilleApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        UserManager.loadUsers();

        Parent root = FXMLLoader.load(getClass().getResource("btc-view.fxml"));
        Scene scene = new Scene(root);
        stage.setTitle("Crypto Portfolio");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        UserManager.saveUsers();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

