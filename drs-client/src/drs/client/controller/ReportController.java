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
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * JavaFX controller for {@code report.fxml}. Lets a public user (or a logged-in
 * operator) submit a new disaster report, which is sent to the remote server
 * through the shared {@link ClientService}.
 *
 * @author DRS Team (Client tier)
 * @version 2.0 (DRS-Enhanced)
 */
public class ReportController {

    /** Disaster type selector. */
    @FXML private ComboBox<String> disasterTypeCombo;

    /** Location input field. */
    @FXML private TextField locationField;

    /** Description input area. */
    @FXML private TextArea descriptionArea;

    /** Reporter name field (shown only for anonymous public reports). */
    @FXML private TextField reporterNameField;

    /** Error message container. */
    @FXML private VBox errorBox;

    /** Error message label. */
    @FXML private Label errorLabel;

    /** Success message container. */
    @FXML private VBox successBox;

    /** Success message label. */
    @FXML private Label successLabel;

    /** Back-to-dashboard link (shown only for logged-in operators). */
    @FXML private Label backLink;

    /** Shared client service used for all server calls. */
    private final ClientService service = MainApp.getService();

    /** The standard list of disaster types offered in the selector. */
    private static final String[] DISASTER_TYPES = {
        "Fire", "Flood", "Earthquake", "Hurricane",
        "Landslide", "Tsunami", "Storm", "Other"
    };

    /**
     * Populates the disaster-type selector. Called automatically by the
     * FXMLLoader after the FXML fields are injected.
     */
    @FXML
    public void initialize() {
        this.disasterTypeCombo.setItems(
                FXCollections.observableArrayList(DISASTER_TYPES));
    } // end method initialize

    /**
     * Configures the form for the current user. A non-public logged-in user
     * gets the Back-to-Dashboard link; an anonymous public user gets the name
     * field so the report can be attributed.
     */
    public void initForCurrentUser() {
        User user = this.service.getCurrentUser();
        boolean loggedIn = user != null;
        boolean operator = loggedIn
                && !User.ROLE_PUBLIC.equals(user.getRole());

        this.backLink.setManaged(operator);
        this.backLink.setVisible(operator);

        // Show the name field only for truly anonymous reporters.
        boolean anonymous = !loggedIn;
        this.reporterNameField.setManaged(anonymous);
        this.reporterNameField.setVisible(anonymous);
    } // end method initForCurrentUser

    /**
     * Handles the Submit Report button click.
     *
     * @param event the triggering action event
     */
    @FXML
    public void handleSubmit(ActionEvent event) {
        this.hideMessages();

        String type = this.disasterTypeCombo.getValue();
        String location = this.locationField.getText().trim();
        String description = this.descriptionArea.getText().trim();

        if (type == null || type.isEmpty()
                || location.isEmpty() || description.isEmpty()) {
            this.showError("Please fill in all required fields.");
            return;
        }

        String reportedBy = this.resolveReporterName();
        if (reportedBy.isEmpty()) {
            this.showError("Please enter your name.");
            return;
        }

        try {
            Response response = this.service.reportDisaster(
                    type, location, description, reportedBy);
            if (response.isSuccess()) {
                this.showSuccess(response.getMessage()
                        + " Emergency services have been informed.");
                this.clearForm();
            } else {
                this.showError(response.getMessage());
            }
        } catch (IOException ex) {
            this.showError("Server error: " + ex.getMessage());
        }
    } // end method handleSubmit

    /**
     * Returns to the dashboard (operators only).
     *
     * @param event the triggering mouse event
     */
    @FXML
    public void goToDashboard(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/drs/client/view/main.fxml"));
            Parent root = loader.load();
            MainController controller = loader.getController();
            controller.initDashboard();
            this.swapScene(root, "DRS-Enhanced - Dashboard");
        } catch (IOException ex) {
            this.showError("Error: " + ex.getMessage());
        }
    } // end method goToDashboard

    /**
     * Handles the Home / Log In nav button by logging out and returning to the
     * login screen.
     *
     * @param event the triggering action event
     */
    @FXML
    public void goHome(ActionEvent event) {
        try {
            this.service.logout();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/drs/client/view/login.fxml"));
            Parent root = loader.load();
            this.swapScene(root, "DRS-Enhanced - Login");
        } catch (IOException ex) {
            this.showError("Error: " + ex.getMessage());
        }
    } // end method goHome

    // --- Private helpers ---

    /**
     * Resolves the reporter name: the logged-in user's full name, or the typed
     * name for an anonymous report.
     *
     * @return the reporter name (possibly empty if anonymous and blank)
     */
    private String resolveReporterName() {
        User user = this.service.getCurrentUser();
        if (user != null) {
            return user.getFullName();
        }
        return this.reporterNameField.getText().trim();
    } // end method resolveReporterName

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
        Stage stage = (Stage) this.locationField.getScene().getWindow();
        stage.setScene(scene);
        stage.setTitle(title);
        stage.setMaximized(true);
    } // end method swapScene

    /**
     * Clears the form inputs after a successful submission.
     */
    private void clearForm() {
        this.disasterTypeCombo.setValue(null);
        this.locationField.clear();
        this.descriptionArea.clear();
        if (this.reporterNameField.isVisible()) {
            this.reporterNameField.clear();
        }
    } // end method clearForm

    /**
     * Shows an error message.
     *
     * @param message the message to display
     */
    private void showError(String message) {
        this.errorLabel.setText(message);
        this.errorBox.setVisible(true);
        this.errorBox.setManaged(true);
    } // end method showError

    /**
     * Shows a success message.
     *
     * @param message the message to display
     */
    private void showSuccess(String message) {
        this.successLabel.setText(message);
        this.successBox.setVisible(true);
        this.successBox.setManaged(true);
    } // end method showSuccess

    /**
     * Hides both the error and success message boxes.
     */
    private void hideMessages() {
        this.errorBox.setVisible(false);
        this.errorBox.setManaged(false);
        this.successBox.setVisible(false);
        this.successBox.setManaged(false);
    } // end method hideMessages

} // end class ReportController
