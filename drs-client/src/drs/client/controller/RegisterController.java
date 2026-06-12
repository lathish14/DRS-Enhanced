package drs.client.controller;

import drs.client.MainApp;
import drs.client.net.ClientService;
import drs.common.Response;
import drs.common.User;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * JavaFX controller for {@code register.fxml}. Lets any visitor create a new
 * DRS account by supplying their full name, a unique username, a password
 * (confirmed), their role, and optionally their department or organisation.
 *
 * <p>On success the user is sent back to the login screen with a confirmation
 * message so they can sign in immediately.</p>
 *
 * @author DRS Team (Client tier)
 * @version 2.0 (DRS-Enhanced)
 */
public class RegisterController {

    // --- FXML fields ---

    /** Full name input. */
    @FXML private TextField fullNameField;

    /** Username input. */
    @FXML private TextField usernameField;

    /** Password input. */
    @FXML private PasswordField passwordField;

    /** Password confirmation input. */
    @FXML private PasswordField confirmPasswordField;

    /** Role selector. */
    @FXML private ComboBox<String> roleCombo;

    /** Department / organisation input (optional). */
    @FXML private TextField departmentField;

    /** Error message container. */
    @FXML private VBox errorBox;

    /** Error message label. */
    @FXML private Label errorLabel;

    /** Success message container. */
    @FXML private VBox successBox;

    /** Success message label. */
    @FXML private Label successLabel;

    /** Shared client service. */
    private final ClientService service = MainApp.getService();

    /**
     * Populates the role selector. Called by FXMLLoader after field injection.
     */
    @FXML
    public void initialize() {
        this.roleCombo.setItems(FXCollections.observableArrayList(
                User.ROLE_PUBLIC,
                User.ROLE_DEPARTMENT_STAFF,
                User.ROLE_COORDINATOR,
                User.ROLE_ADMIN));
        this.roleCombo.setValue(User.ROLE_PUBLIC);
    } // end method initialize

    /**
     * Handles the Register button click. Validates all fields client-side
     * first, then submits to the server.
     *
     * @param event the triggering action event
     */
    @FXML
    public void handleRegister(ActionEvent event) {
        this.hideMessages();

        String fullName   = this.fullNameField.getText().trim();
        String username   = this.usernameField.getText().trim();
        String password   = this.passwordField.getText();
        String confirm    = this.confirmPasswordField.getText();
        String role       = this.roleCombo.getValue();
        String department = this.departmentField.getText().trim();

        // Client-side validation
        if (fullName.isEmpty()) {
            this.showError("Please enter your full name.");
            return;
        }
        if (username.isEmpty()) {
            this.showError("Please choose a username.");
            return;
        }
        if (username.length() < 3) {
            this.showError("Username must be at least 3 characters.");
            return;
        }
        if (password.isEmpty()) {
            this.showError("Please choose a password.");
            return;
        }
        if (password.length() < 6) {
            this.showError("Password must be at least 6 characters.");
            return;
        }
        if (!password.equals(confirm)) {
            this.showError("Passwords do not match.");
            this.confirmPasswordField.clear();
            return;
        }
        if (role == null) {
            this.showError("Please select a role.");
            return;
        }

        try {
            Response response = this.service.register(
                    fullName, username, password, role, department);
            if (response.isSuccess()) {
                this.showSuccess("Account created! Redirecting to login…");
                // Brief pause then go to login screen
                javafx.animation.PauseTransition pause =
                        new javafx.animation.PauseTransition(
                                javafx.util.Duration.seconds(1.5));
                pause.setOnFinished(e -> {
                    try { this.goToLogin(); }
                    catch (IOException ex) {
                        this.showError("Error: " + ex.getMessage());
                    }
                });
                pause.play();
            } else {
                this.showError(response.getMessage());
            }
        } catch (IOException ex) {
            this.showError("Server error: " + ex.getMessage());
        }
    } // end method handleRegister

    /**
     * Navigates back to the login screen when the "Log in" label is clicked.
     *
     * @param event the triggering mouse event
     */
    @FXML
    public void handleBackClick(javafx.scene.input.MouseEvent event) {
        try {
            this.goToLogin();
        } catch (IOException ex) {
            this.showError("Error: " + ex.getMessage());
        }
    } // end method handleBackClick

    /**
     * Navigates back to the login screen when Back is clicked.
     *
     * @param event the triggering action event
     */
    @FXML
    public void handleBack(ActionEvent event) {
        try {
            this.goToLogin();
        } catch (IOException ex) {
            this.showError("Error: " + ex.getMessage());
        }
    } // end method handleBack

    // --- Private helpers ---

    /**
     * Loads the login screen and replaces the current scene.
     *
     * @throws IOException if the FXML cannot be loaded
     */
    private void goToLogin() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/drs/client/view/login.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 1280, 800);
        String css = getClass()
                .getResource("/drs/client/view/drs.css")
                .toExternalForm();
        scene.getStylesheets().add(css);
        Stage stage = (Stage) this.fullNameField.getScene().getWindow();
        stage.setScene(scene);
        stage.setTitle("DRS-Enhanced - Login");
        stage.setMaximized(true);
    } // end method goToLogin

    /**
     * Shows an error message.
     *
     * @param message the message
     */
    private void showError(String message) {
        this.errorLabel.setText(message);
        this.errorBox.setVisible(true);
        this.errorBox.setManaged(true);
    } // end method showError

    /**
     * Shows a success message.
     *
     * @param message the message
     */
    private void showSuccess(String message) {
        this.successLabel.setText(message);
        this.successBox.setVisible(true);
        this.successBox.setManaged(true);
    } // end method showSuccess

    /**
     * Hides both message boxes.
     */
    private void hideMessages() {
        this.errorBox.setVisible(false);
        this.errorBox.setManaged(false);
        this.successBox.setVisible(false);
        this.successBox.setManaged(false);
    } // end method hideMessages

} // end class RegisterController
