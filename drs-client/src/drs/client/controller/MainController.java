package drs.client.controller;

import drs.client.MainApp;
import drs.client.net.ClientService;
import drs.common.Alert;
import drs.common.Department;
import drs.common.Disaster;
import drs.common.Dispatch;
import drs.common.Resource;
import drs.common.Response;
import drs.common.ResponseLog;
import drs.common.Stats;
import drs.common.User;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * JavaFX controller for {@code main.fxml}, the operator dashboard of the
 * DRS-Enhanced client. It presents disasters, resources/dispatch (Feature 1),
 * departments, the audit log, statistics, and (for admins) user management,
 * and it shows a live alert banner driven by server pushes (Feature 2).
 *
 * <p>All data is fetched from the remote server through the shared
 * {@link ClientService}. Because pushed alerts arrive on a background thread,
 * the alert handler marshals UI updates onto the JavaFX Application Thread with
 * {@link Platform#runLater(Runnable)}.</p>
 *
 * @author DRS Team (Client tier)
 * @version 2.0 (DRS-Enhanced)
 */
public class MainController {

    // --- Top bar / alert banner ---
    @FXML private Label userLabel;
    @FXML private Button reportNewBtn;
    @FXML private Button logoutBtn;
    @FXML private HBox alertBanner;
    @FXML private Label alertBannerLabel;
    @FXML private TabPane tabPane;

    // --- Disasters tab ---
    @FXML private TableView<Disaster> disastersTable;
    @FXML private TextField severityField;
    @FXML private Button assessBtn;
    @FXML private Button notifyBtn;
    @FXML private ComboBox<String> statusCombo;
    @FXML private Button updateStatusBtn;
    @FXML private Button refreshDisastersBtn;

    // --- Resources & dispatch tab (Feature 1) ---
    @FXML private Label selectedDisasterLabel;
    @FXML private TableView<Resource> resourcesTable;
    @FXML private TextField unitsField;
    @FXML private Button dispatchBtn;
    @FXML private Button refreshResourcesBtn;
    @FXML private TableView<Dispatch> dispatchTable;
    @FXML private Button recallBtn;

    // --- Departments / logs ---
    @FXML private TableView<Department> departmentsTable;
    @FXML private TableView<ResponseLog> logsTable;

    // --- Statistics ---
    @FXML private FlowPane statsPane;
    @FXML private TextArea statsByTypeArea;

    // --- Users tab (admin) ---
    @FXML private Tab usersTab;
    @FXML private TableView<User> usersTable;
    @FXML private Button toggleActiveBtn;
    @FXML private TextField newFullName;
    @FXML private TextField newUsername;
    @FXML private TextField newPassword;
    @FXML private ComboBox<String> newRole;
    @FXML private TextField newDept;
    @FXML private Button addUserBtn;

    /** Shared client service for all server calls. */
    private final ClientService service = MainApp.getService();

    /**
     * Initialises the dashboard after login: builds table columns, applies
     * role-based visibility, registers the live-alert listener, subscribes to
     * broadcasts, and loads the initial data.
     */
    public void initDashboard() {
        User user = this.service.getCurrentUser();
        this.userLabel.setText("Signed in: " + user.getFullName()
                + "  (" + user.getRole() + ")");

        this.buildDisasterColumns();
        this.buildResourceColumns();
        this.buildDispatchColumns();
        this.buildDepartmentColumns();
        this.buildLogColumns();
        this.buildUserColumns();

        this.statusCombo.setItems(FXCollections.observableArrayList(
                Disaster.STATUS_REPORTED, Disaster.STATUS_UNDER_ASSESSMENT,
                Disaster.STATUS_RESPONDING, Disaster.STATUS_RESOLVED));
        this.newRole.setItems(FXCollections.observableArrayList(
                User.ROLE_ADMIN, User.ROLE_COORDINATOR,
                User.ROLE_DEPARTMENT_STAFF, User.ROLE_PUBLIC));

        // When the selected disaster changes, refresh its dispatch history.
        this.disastersTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, sel) -> this.onDisasterSelected(sel));

        this.applyRolePermissions(user);
        this.registerAlertListener();
        this.refreshAll();

        try {
            this.service.subscribeAlerts();
        } catch (IOException ex) {
            System.err.println("Could not subscribe to alerts: "
                    + ex.getMessage());
        }
    } // end method initDashboard

    // --- Column builders ---

    /**
     * Builds the disasters table columns.
     */
    private void buildDisasterColumns() {
        this.disastersTable.getColumns().setAll(
                col("ID", d -> String.valueOf(d.getDisasterId()), 50),
                col("Type", Disaster::getDisasterType, 110),
                col("Location", Disaster::getLocation, 160),
                col("Severity", d -> String.valueOf(d.getSeverityScore()), 75),
                col("Priority", Disaster::getPriorityLevel, 90),
                col("Status", Disaster::getStatus, 140),
                col("Reported By", Disaster::getReportedBy, 130),
                col("Reported At", Disaster::getReportedAtFormatted, 140),
                col("Departments", Disaster::getNotifiedDepartments, 200));
    } // end method buildDisasterColumns

    /**
     * Builds the resources table columns.
     */
    private void buildResourceColumns() {
        this.resourcesTable.getColumns().setAll(
                col("ID", r -> String.valueOf(r.getResourceId()), 45),
                col("Department", Resource::getDepartmentName, 150),
                col("Resource", Resource::getResourceName, 140),
                col("Type", Resource::getResourceType, 100),
                col("Total", r -> String.valueOf(r.getTotalUnits()), 70),
                col("Deployed", r -> String.valueOf(r.getDeployedUnits()), 80),
                col("Available",
                        r -> String.valueOf(r.getAvailableUnits()), 80));
    } // end method buildResourceColumns

    /**
     * Builds the dispatch history table columns.
     */
    private void buildDispatchColumns() {
        this.dispatchTable.getColumns().setAll(
                col("ID", d -> String.valueOf(d.getDispatchId()), 45),
                col("Resource", Dispatch::getResourceName, 140),
                col("Department", Dispatch::getDepartmentName, 150),
                col("Units", d -> String.valueOf(d.getUnitsDispatched()), 60),
                col("Status", Dispatch::getStatus, 90),
                col("By", Dispatch::getDispatchedBy, 110),
                col("At", Dispatch::getDispatchedAtFormatted, 150));
    } // end method buildDispatchColumns

    /**
     * Builds the departments table columns.
     */
    private void buildDepartmentColumns() {
        this.departmentsTable.getColumns().setAll(
                col("ID", d -> String.valueOf(d.getDepartmentId()), 45),
                col("Name", Department::getDepartmentName, 160),
                col("Specialization", Department::getSpecialization, 130),
                col("Status", Department::getResponseStatus, 110),
                col("Contact", Department::getContactNumber, 120),
                col("Email", Department::getContactEmail, 200));
    } // end method buildDepartmentColumns

    /**
     * Builds the audit log table columns.
     */
    private void buildLogColumns() {
        this.logsTable.getColumns().setAll(
                col("Time", ResponseLog::getLogTimeFormatted, 160),
                col("Disaster", l -> String.valueOf(l.getDisasterId()), 70),
                col("By", ResponseLog::getActionBy, 110),
                col("Type", ResponseLog::getActionType, 150),
                col("Action", ResponseLog::getActionDescription, 360));
    } // end method buildLogColumns

    /**
     * Builds the users table columns.
     */
    private void buildUserColumns() {
        this.usersTable.getColumns().setAll(
                col("ID", u -> String.valueOf(u.getUserId()), 45),
                col("Full Name", User::getFullName, 150),
                col("Username", User::getUsername, 120),
                col("Role", User::getRole, 140),
                col("Department", User::getDepartment, 160),
                col("Active", u -> u.isActive() ? "Yes" : "No", 70));
    } // end method buildUserColumns

    /**
     * Helper that creates a read-only string column from a value extractor.
     *
     * @param <T>       the row type
     * @param title     the column header
     * @param extractor maps a row to its display string
     * @param width     the preferred column width
     * @return the configured TableColumn
     */
    private <T> TableColumn<T, String> col(String title,
                                           ValueExtractor<T> extractor,
                                           double width) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(cell ->
                new SimpleStringProperty(
                        safe(extractor.apply(cell.getValue()))));
        return column;
    } // end method col

    /**
     * Functional interface for extracting a display string from a row object.
     *
     * @param <T> the row type
     */
    @FunctionalInterface
    private interface ValueExtractor<T> {
        /**
         * Maps a row to a display string.
         *
         * @param row the row object
         * @return the display string
         */
        String apply(T row);
    } // end interface ValueExtractor

    // --- Role permissions ---

    /**
     * Applies role-based visibility to dashboard controls.
     *
     * @param user the logged-in user
     */
    private void applyRolePermissions(User user) {
        String role = user.getRole();
        boolean isAdmin = User.ROLE_ADMIN.equals(role);
        boolean isCoordinator = User.ROLE_COORDINATOR.equals(role);
        boolean canRespond = isAdmin || isCoordinator;

        // Only admins manage users; remove the tab entirely otherwise.
        if (!isAdmin) {
            this.tabPane.getTabs().remove(this.usersTab);
        }

        // Assess/notify/dispatch are coordinator/admin operations.
        this.assessBtn.setDisable(!canRespond);
        this.notifyBtn.setDisable(!canRespond);
        this.dispatchBtn.setDisable(!canRespond);
        this.recallBtn.setDisable(!canRespond);
        // Department staff may still update response status.
    } // end method applyRolePermissions

    // --- Alert listener (Feature 2) ---

    /**
     * Registers the live-alert listener. Alerts arrive on a background thread,
     * so UI changes are pushed onto the JavaFX Application Thread.
     */
    private void registerAlertListener() {
        this.service.setAlertListener(alert ->
                Platform.runLater(() -> this.showAlertBanner(alert)));
    } // end method registerAlertListener

    /**
     * Displays an alert in the banner and auto-refreshes the disaster list so
     * the dashboard reflects the broadcast event.
     *
     * @param alert the pushed alert
     */
    private void showAlertBanner(Alert alert) {
        String prefix = Alert.LEVEL_CRITICAL.equals(alert.getLevel())
                ? "CRITICAL" : "INFO";
        this.alertBannerLabel.setText("[" + alert.getCreatedAtFormatted()
                + "] " + prefix + " - " + alert.getTitle() + ": "
                + alert.getMessage());
        this.alertBanner.getStyleClass().removeAll(
                "alert-banner-critical", "alert-banner-info");
        this.alertBanner.getStyleClass().add(
                Alert.LEVEL_CRITICAL.equals(alert.getLevel())
                        ? "alert-banner-critical" : "alert-banner-info");
        this.alertBanner.setManaged(true);
        this.alertBanner.setVisible(true);
        // Keep data fresh when something noteworthy happened elsewhere.
        this.handleRefreshDisasters(null);
    } // end method showAlertBanner

    /**
     * Dismisses the alert banner.
     *
     * @param event the triggering action event
     */
    @FXML
    public void handleDismissAlert(ActionEvent event) {
        this.alertBanner.setManaged(false);
        this.alertBanner.setVisible(false);
    } // end method handleDismissAlert

    // --- Data refresh ---

    /**
     * Refreshes every dataset shown on the dashboard.
     */
    private void refreshAll() {
        this.handleRefreshDisasters(null);
        this.handleRefreshResources(null);
        this.handleRefreshDepartments(null);
        this.handleRefreshLogs(null);
        this.handleRefreshStats(null);
        if (this.tabPane.getTabs().contains(this.usersTab)) {
            this.refreshUsers();
        }
    } // end method refreshAll

    /**
     * Reloads the disasters table.
     *
     * @param event the triggering action event (may be null)
     */
    @FXML
    public void handleRefreshDisasters(ActionEvent event) {
        try {
            List<Disaster> disasters = this.service.getDisasters();
            this.disastersTable.setItems(
                    FXCollections.observableArrayList(disasters));
        } catch (IOException ex) {
            this.error("Failed to load disasters: " + ex.getMessage());
        }
    } // end method handleRefreshDisasters

    /**
     * Reloads the resources table.
     *
     * @param event the triggering action event (may be null)
     */
    @FXML
    public void handleRefreshResources(ActionEvent event) {
        try {
            List<Resource> resources = this.service.getResources();
            this.resourcesTable.setItems(
                    FXCollections.observableArrayList(resources));
        } catch (IOException ex) {
            this.error("Failed to load resources: " + ex.getMessage());
        }
    } // end method handleRefreshResources

    /**
     * Reloads the departments table.
     *
     * @param event the triggering action event (may be null)
     */
    @FXML
    public void handleRefreshDepartments(ActionEvent event) {
        try {
            List<Department> departments = this.service.getDepartments();
            this.departmentsTable.setItems(
                    FXCollections.observableArrayList(departments));
        } catch (IOException ex) {
            this.error("Failed to load departments: " + ex.getMessage());
        }
    } // end method handleRefreshDepartments

    /**
     * Reloads the audit log table (newest first).
     *
     * @param event the triggering action event (may be null)
     */
    @FXML
    public void handleRefreshLogs(ActionEvent event) {
        try {
            List<ResponseLog> logs = this.service.getLogs();
            java.util.Collections.reverse(logs);
            this.logsTable.setItems(
                    FXCollections.observableArrayList(logs));
        } catch (IOException ex) {
            this.error("Failed to load logs: " + ex.getMessage());
        }
    } // end method handleRefreshLogs

    /**
     * Reloads the statistics panel.
     *
     * @param event the triggering action event (may be null)
     */
    @FXML
    public void handleRefreshStats(ActionEvent event) {
        try {
            Stats stats = this.service.getStats();
            if (stats == null) {
                return;
            }
            this.statsPane.getChildren().setAll(
                    statCard("Total Disasters", stats.getTotalCount()),
                    statCard("Active", stats.getActiveCount()),
                    statCard("High Priority", stats.getHighPriorityCount()),
                    statCard("Medium Priority",
                            stats.getMediumPriorityCount()),
                    statCard("Resolved", stats.getResolvedCount()),
                    statCard("Units Deployed",
                            stats.getTotalUnitsDeployed()));
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Integer> e
                    : stats.getCountByType().entrySet()) {
                sb.append(e.getKey()).append(": ")
                        .append(e.getValue()).append("\n");
            }
            sb.append("\nAverage severity: ")
                    .append(String.format("%.1f", stats.getAverageSeverity()));
            this.statsByTypeArea.setText(sb.toString());
        } catch (IOException ex) {
            this.error("Failed to load statistics: " + ex.getMessage());
        }
    } // end method handleRefreshStats

    /**
     * Reloads the users table (admin only).
     */
    private void refreshUsers() {
        try {
            List<User> users = this.service.getUsers();
            this.usersTable.setItems(
                    FXCollections.observableArrayList(users));
        } catch (IOException ex) {
            this.error("Failed to load users: " + ex.getMessage());
        }
    } // end method refreshUsers

    /**
     * Builds a small statistic "card" node for the stats FlowPane.
     *
     * @param title the stat label
     * @param value the stat value
     * @return a styled VBox card
     */
    private VBox statCard(String title, int value) {
        Label valueLabel = new Label(String.valueOf(value));
        valueLabel.getStyleClass().add("stat-value");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-label");
        VBox card = new VBox(6, valueLabel, titleLabel);
        card.getStyleClass().add("stat-card");
        return card;
    } // end method statCard

    // --- Disaster selection / dispatch history ---

    /**
     * Called when the selected disaster changes; updates the label and reloads
     * the dispatch history for that disaster.
     *
     * @param selected the newly selected disaster (may be null)
     */
    private void onDisasterSelected(Disaster selected) {
        if (selected == null) {
            this.selectedDisasterLabel.setText(
                    "Select a disaster on the first tab, then a resource"
                    + " below, to dispatch.");
            this.dispatchTable.getItems().clear();
            return;
        }
        this.selectedDisasterLabel.setText("Selected disaster: #"
                + selected.getDisasterId() + " - "
                + selected.getDisasterType() + " at "
                + selected.getLocation());
        this.loadDispatchHistory(selected.getDisasterId());
    } // end method onDisasterSelected

    /**
     * Loads the dispatch history for a disaster into the dispatch table.
     *
     * @param disasterId the disaster whose dispatches to show
     */
    private void loadDispatchHistory(int disasterId) {
        try {
            List<Dispatch> dispatches =
                    this.service.getDispatches(disasterId);
            this.dispatchTable.setItems(
                    FXCollections.observableArrayList(dispatches));
        } catch (IOException ex) {
            this.error("Failed to load dispatches: " + ex.getMessage());
        }
    } // end method loadDispatchHistory

    // --- Response actions ---

    /**
     * Assesses the selected disaster with the entered severity score.
     *
     * @param event the triggering action event
     */
    @FXML
    public void handleAssess(ActionEvent event) {
        Disaster selected = this.selectedDisaster();
        if (selected == null) {
            return;
        }
        Integer severity = this.parseIntField(this.severityField);
        if (severity == null || severity < 1 || severity > 10) {
            this.error("Enter a severity score between 1 and 10.");
            return;
        }
        try {
            Response response =
                    this.service.assess(selected.getDisasterId(), severity);
            this.afterAction(response);
        } catch (IOException ex) {
            this.error("Assess failed: " + ex.getMessage());
        }
    } // end method handleAssess

    /**
     * Notifies departments for the selected disaster.
     *
     * @param event the triggering action event
     */
    @FXML
    public void handleNotify(ActionEvent event) {
        Disaster selected = this.selectedDisaster();
        if (selected == null) {
            return;
        }
        try {
            Response response =
                    this.service.notifyDepartments(selected.getDisasterId());
            this.afterAction(response);
            this.handleRefreshDepartments(null);
        } catch (IOException ex) {
            this.error("Notify failed: " + ex.getMessage());
        }
    } // end method handleNotify

    /**
     * Updates the status of the selected disaster.
     *
     * @param event the triggering action event
     */
    @FXML
    public void handleUpdateStatus(ActionEvent event) {
        Disaster selected = this.selectedDisaster();
        if (selected == null) {
            return;
        }
        String status = this.statusCombo.getValue();
        if (status == null) {
            this.error("Choose a status to apply.");
            return;
        }
        try {
            Response response = this.service.updateStatus(
                    selected.getDisasterId(), status);
            this.afterAction(response);
        } catch (IOException ex) {
            this.error("Status update failed: " + ex.getMessage());
        }
    } // end method handleUpdateStatus

    /**
     * Dispatches resource units to the selected disaster (Feature 1).
     *
     * @param event the triggering action event
     */
    @FXML
    public void handleDispatch(ActionEvent event) {
        Disaster disaster = this.selectedDisaster();
        if (disaster == null) {
            this.error("Select a disaster on the first tab first.");
            return;
        }
        Resource resource =
                this.resourcesTable.getSelectionModel().getSelectedItem();
        if (resource == null) {
            this.error("Select a resource to dispatch.");
            return;
        }
        Integer units = this.parseIntField(this.unitsField);
        if (units == null || units < 1) {
            this.error("Enter a positive number of units.");
            return;
        }
        try {
            Response response = this.service.dispatchResource(
                    disaster.getDisasterId(), resource.getResourceId(), units);
            this.afterAction(response);
            this.handleRefreshResources(null);
            this.loadDispatchHistory(disaster.getDisasterId());
        } catch (IOException ex) {
            this.error("Dispatch failed: " + ex.getMessage());
        }
    } // end method handleDispatch

    /**
     * Recalls the selected dispatch (Feature 1).
     *
     * @param event the triggering action event
     */
    @FXML
    public void handleRecall(ActionEvent event) {
        Dispatch selected =
                this.dispatchTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            this.error("Select a dispatch to recall.");
            return;
        }
        try {
            Response response =
                    this.service.recallDispatch(selected.getDispatchId());
            this.afterAction(response);
            this.handleRefreshResources(null);
            Disaster disaster = this.selectedDisaster();
            if (disaster != null) {
                this.loadDispatchHistory(disaster.getDisasterId());
            }
        } catch (IOException ex) {
            this.error("Recall failed: " + ex.getMessage());
        }
    } // end method handleRecall

    // --- User management (admin) ---

    /**
     * Toggles the active flag of the selected user.
     *
     * @param event the triggering action event
     */
    @FXML
    public void handleToggleActive(ActionEvent event) {
        User selected =
                this.usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            this.error("Select a user first.");
            return;
        }
        try {
            Response response = this.service.setUserActive(
                    selected.getUserId(), !selected.isActive());
            if (response.isSuccess()) {
                this.refreshUsers();
            } else {
                this.error(response.getMessage());
            }
        } catch (IOException ex) {
            this.error("Update failed: " + ex.getMessage());
        }
    } // end method handleToggleActive

    /**
     * Adds a new user account from the entry form.
     *
     * @param event the triggering action event
     */
    @FXML
    public void handleAddUser(ActionEvent event) {
        String fullName = this.newFullName.getText().trim();
        String username = this.newUsername.getText().trim();
        String password = this.newPassword.getText().trim();
        String role = this.newRole.getValue();
        String dept = this.newDept.getText().trim();

        if (fullName.isEmpty() || username.isEmpty()
                || password.isEmpty() || role == null) {
            this.error("Full name, username, password and role are required.");
            return;
        }
        try {
            Response response = this.service.addUser(
                    fullName, username, password, role, dept);
            if (response.isSuccess()) {
                this.info(response.getMessage());
                this.newFullName.clear();
                this.newUsername.clear();
                this.newPassword.clear();
                this.newRole.setValue(null);
                this.newDept.clear();
                this.refreshUsers();
            } else {
                this.error(response.getMessage());
            }
        } catch (IOException ex) {
            this.error("Add user failed: " + ex.getMessage());
        }
    } // end method handleAddUser

    // --- Navigation ---

    /**
     * Opens the report screen to file a new disaster.
     *
     * @param event the triggering action event
     */
    @FXML
    public void handleReportNew(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/drs/client/view/report.fxml"));
            Parent root = loader.load();
            ReportController controller = loader.getController();
            controller.initForCurrentUser();
            this.swapScene(root, "DRS-Enhanced - Report Disaster");
        } catch (IOException ex) {
            this.error("Error: " + ex.getMessage());
        }
    } // end method handleReportNew

    /**
     * Logs out and returns to the login screen.
     *
     * @param event the triggering action event
     */
    @FXML
    public void handleLogout(ActionEvent event) {
        this.service.setAlertListener(null);
        this.service.logout();
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/drs/client/view/login.fxml"));
            Parent root = loader.load();
            this.swapScene(root, "DRS-Enhanced - Login");
        } catch (IOException ex) {
            this.error("Error: " + ex.getMessage());
        }
    } // end method handleLogout

    // --- Small helpers ---

    /**
     * Returns the currently selected disaster, showing a hint if none.
     *
     * @return the selected Disaster, or null
     */
    private Disaster selectedDisaster() {
        Disaster selected =
                this.disastersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            this.error("Select a disaster from the table first.");
        }
        return selected;
    } // end method selectedDisaster

    /**
     * After a server action: on success refresh disasters and logs; on failure
     * show the message.
     *
     * @param response the server response
     */
    private void afterAction(Response response) {
        if (response.isSuccess()) {
            this.handleRefreshDisasters(null);
            this.handleRefreshLogs(null);
            this.handleRefreshStats(null);
        } else {
            this.error(response.getMessage());
        }
    } // end method afterAction

    /**
     * Parses a text field as an integer, returning null if invalid.
     *
     * @param field the field to parse
     * @return the integer, or null
     */
    private Integer parseIntField(TextField field) {
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    } // end method parseIntField

    /**
     * Replaces the current scene with a new root.
     *
     * @param root  the new root
     * @param title the new window title
     */
    private void swapScene(Parent root, String title) {
        Scene scene = new Scene(root, 1280, 800);
        String css = getClass()
                .getResource("/drs/client/view/drs.css")
                .toExternalForm();
        scene.getStylesheets().add(css);
        Stage stage = (Stage) this.userLabel.getScene().getWindow();
        stage.setScene(scene);
        stage.setTitle(title);
        stage.setMaximized(true);
    } // end method swapScene

    /**
     * Shows an error dialog.
     *
     * @param message the message
     */
    private void error(String message) {
        this.dialog(AlertType.ERROR, "Error", message);
    } // end method error

    /**
     * Shows an information dialog.
     *
     * @param message the message
     */
    private void info(String message) {
        this.dialog(AlertType.INFORMATION, "Success", message);
    } // end method info

    /**
     * Shows a dialog of the given type.
     *
     * @param type    the alert type
     * @param title   the dialog title
     * @param message the dialog content
     */
    private void dialog(AlertType type, String title, String message) {
        javafx.scene.control.Alert dialog =
                new javafx.scene.control.Alert(type);
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(message);
        dialog.showAndWait();
    } // end method dialog

    /**
     * Returns the given string, or an empty string if null.
     *
     * @param value the value
     * @return a non-null string
     */
    private static String safe(String value) {
        return value == null ? "" : value;
    } // end method safe

} // end class MainController
