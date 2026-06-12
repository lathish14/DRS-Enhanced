package drs.client.controller;

import drs.client.MainApp;
import drs.client.net.ClientService;
import drs.common.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * JavaFX controller for {@code login.fxml}. Authenticates against the remote
 * DRS server through the shared {@link ClientService} and routes the user to
 * the correct screen based on their role.
 *
 * @author DRS Team (Client tier)
 * @version 2.0 (DRS-Enhanced)
 */
public class LoginController {

    /** Username input field. */
    @FXML private TextField usernameField;

    /** Password input field. */
    @FXML private PasswordField passwordField;

    /** Container shown when an error must be displayed. */
    @FXML private VBox errorBox;

    /** Label holding the current error message. */
    @FXML private Label errorLabel;

    /** Shared client service used for all server calls. */
    private final ClientService service = MainApp.getService();

    /**
     * Handles the Log In button click (or Enter key in a field).
     *
     * @param event the triggering action event
     */
    @FXML
    public void handleLogin(ActionEvent event) {
        String username = this.usernameField.getText().trim();
        String password = this.passwordField.getText();
        this.hideError();

        if (username.isEmpty() || password.isEmpty()) {
            this.showError("Please enter your username and password.");
            return;
        }

        try {
            boolean success = this.service.login(username, password);
            if (!success) {
                this.showError("Invalid username or password.");
                this.passwordField.clear();
                return;
            }
            User user = this.service.getCurrentUser();
            if (User.ROLE_PUBLIC.equals(user.getRole())) {
                this.loadReport();
            } else {
                this.loadDashboard();
            }
        } catch (IOException ex) {
            this.showError("Server error: " + ex.getMessage());
        }
    } // end method handleLogin

    /**
     * Shows the About Us information dialog.
     *
     * @param event the triggering action event
     */
    @FXML
    public void showAboutUs(ActionEvent event) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("About - Disaster Response System");
        alert.setHeaderText("Disaster Response System (DRS-Enhanced)");
        alert.setContentText(
                "A three-tier distributed emergency management platform"
                + " developed for COIT20258 Software Engineering at"
                + " CQUniversity Australia.\n\n"
                + "Architecture:\n"
                + "- JavaFX client (this application)\n"
                + "- Multi-threaded socket server\n"
                + "- MySQL database backend\n\n"
                + "Enhanced features:\n"
                + "- Resource & Dispatch Management\n"
                + "- Real-time multi-user alerts & status broadcast\n\n"
                + "Unit: COIT20258 - Software Engineering\n"
                + "Institution: CQUniversity Australia");
        alert.showAndWait();
    } // end method showAboutUs

    /**
     * Shows the Contact information dialog.
     *
     * @param event the triggering action event
     */
    @FXML
    public void showContact(ActionEvent event) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Contact - Disaster Response System");
        alert.setHeaderText("Contact Information");
        alert.setContentText(
                "EMERGENCY: Call 000 immediately for any"
                + " life-threatening emergency.\n\n"
                + "DRS Support and Administration:\n"
                + "  Email:  drs-support@emergency.gov.au\n"
                + "  Phone:  1800 377 435\n"
                + "  Hours:  24 hours / 7 days");
        alert.showAndWait();
    } // end method showContact

    /**
     * Handles the Home nav button by clearing the form.
     *
     * @param event the triggering action event
     */
    @FXML
    public void goHome(ActionEvent event) {
        this.usernameField.clear();
        this.passwordField.clear();
        this.hideError();
    } // end method goHome

    /**
     * Handles the REGISTER nav button click (ActionEvent from Button).
     *
     * @param event the triggering action event
     */
    @FXML
    public void goRegister(ActionEvent event) {
        this.openRegisterScreen();
    } // end method goRegister

    /**
     * Handles the "Create one" label click (MouseEvent from Label).
     *
     * @param event the triggering mouse event
     */
    @FXML
    public void goSignUp(javafx.scene.input.MouseEvent event) {
        this.openRegisterScreen();
    } // end method goSignUp

    /**
     * Loads and shows the register screen.
     */
    private void openRegisterScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/drs/client/view/register.fxml"));
            Parent root = loader.load();
            this.swapScene(root, "DRS-Enhanced - Register");
        } catch (IOException ex) {
            this.showError("Error: " + ex.getMessage());
        }
    } // end method openRegisterScreen

    // --- Private helpers ---

    /**
     * Loads the main dashboard scene and initialises it.
     *
     * @throws IOException if the FXML cannot be loaded
     */
    private void loadDashboard() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/drs/client/view/main.fxml"));
        Parent root = loader.load();
        MainController controller = loader.getController();
        controller.initDashboard();
        this.swapScene(root, "DRS-Enhanced - Dashboard");
    } // end method loadDashboard

    /**
     * Loads the report screen and initialises it for the current user.
     *
     * @throws IOException if the FXML cannot be loaded
     */
    private void loadReport() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/drs/client/view/report.fxml"));
        Parent root = loader.load();
        ReportController controller = loader.getController();
        controller.initForCurrentUser();
        this.swapScene(root, "DRS-Enhanced - Report Disaster");
    } // end method loadReport

    /**
     * Replaces the current scene with the given root node.
     *
     * @param root  the new scene root
     * @param title the new window title
     */
    private void swapScene(Parent root, String title) {
        Scene scene = new Scene(root, 1280, 800);
        String css = getClass()
                .getResource("/drs/client/view/drs.css")
                .toExternalForm();
        scene.getStylesheets().add(css);
        Stage stage = (Stage) this.usernameField.getScene().getWindow();
        stage.setScene(scene);
        stage.setTitle(title);
        stage.setMaximized(true);
    } // end method swapScene

    /**
     * Shows the error box with a message.
     *
     * @param message the message to display
     */
    private void showError(String message) {
        this.errorLabel.setText(message);
        this.errorBox.setVisible(true);
        this.errorBox.setManaged(true);
    } // end method showError

    /**
     * Hides the error box.
     */
    private void hideError() {
        this.errorBox.setVisible(false);
        this.errorBox.setManaged(false);
    } // end method hideError

} // end class LoginController
