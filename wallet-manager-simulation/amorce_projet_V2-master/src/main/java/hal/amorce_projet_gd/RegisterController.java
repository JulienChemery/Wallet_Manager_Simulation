package hal.amorce_projet_gd;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label registrationMessageLabel;

    public void handleRegister(ActionEvent event) throws Exception {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (isPasswordValid(password)) {
            if (UserManager.registerUser(username, password)) {
                registrationMessageLabel.setText("Inscription réussie");
                switchToLogin(event);
            } else {
                registrationMessageLabel.setText("L'utilisateur existe déjà !");
            }
        } else {
            registrationMessageLabel.setText("Il faut au moins : 6 caractères, un chiffre et un caractère spécial");
        }
    }

    private boolean isPasswordValid(String password) {
        if (password.length() < 6) {
            return false;
        }

        if (!password.matches(".*[a-zA-Z].*")) {
            return false;
        }

        if (!password.matches(".*[!@#$%^&*()_+=\\-\\[\\]{};:'\"\\\\|,.<>\\/?].*")) {
            return false;
        }
        return true;
    }

    public void switchToLogin(ActionEvent event) throws Exception {
        Stage stage = (Stage) usernameField.getScene().getWindow();
        Parent root = FXMLLoader.load(getClass().getResource("login.fxml"));
        stage.setScene(new Scene(root));
        stage.show();
    }
}
