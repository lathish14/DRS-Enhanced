package drs.client;

import drs.client.net.ClientService;
import drs.client.net.ServerConnection;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * JavaFX application entry point for the DRS-Enhanced client tier. On startup
 * it opens a connection to the multi-threaded {@code DrsServer}, then loads the
 * login screen. A single shared {@link ClientService} is created here and
 * passed to each controller so the whole client shares one server connection.
 *
 * <p>If the server cannot be reached, a clear dialog tells the user to start
 * the server first, since the client is useless without it.</p>
 *
 * @author DRS Team (Client tier)
 * @version 2.0 (DRS-Enhanced)
 */
public class MainApp extends Application {

    /** Title displayed in the primary application window title bar. */
    public static final String APP_TITLE =
            "Disaster Response System (DRS-Enhanced) - COIT20258";

    /** The single shared client service used across all controllers. */
    private static final ClientService SERVICE = new ClientService();

    /**
     * Returns the shared ClientService instance.
     *
     * @return the shared ClientService
     */
    public static ClientService getService() {
        return SERVICE;
    } // end method getService

    /**
     * JavaFX start method. Connects to the server, then loads login.fxml.
     *
     * @param primaryStage the primary window provided by the JavaFX runtime
     */
    @Override
    public void start(Stage primaryStage) {
        if (!this.connectToServer()) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/drs/client/view/login.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1280, 800);
            this.addCss(scene);

            primaryStage.setTitle(APP_TITLE);
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1100);
            primaryStage.setMinHeight(700);
            primaryStage.setMaximized(true);
            primaryStage.show();
        } catch (IOException ex) {
            this.showFatal("Unable to load the login screen: "
                    + ex.getMessage());
        }
    } // end method start

    /**
     * Attempts to connect to the DRS server, showing a guidance dialog on
     * failure.
     *
     * @return true if connected, false otherwise
     */
    private boolean connectToServer() {
        try {
            ServerConnection.getInstance().connect();
            return true;
        } catch (IOException ex) {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Cannot Connect to DRS Server");
            alert.setHeaderText("The DRS server is not reachable.");
            alert.setContentText(
                    "Please start the DRS server first, then launch the"
                    + " client again.\n\nExpected server at "
                    + ServerConnection.DEFAULT_HOST + ":"
                    + ServerConnection.DEFAULT_PORT
                    + "\n\nDetails: " + ex.getMessage());
            alert.showAndWait();
            return false;
        }
    } // end method connectToServer

    /**
     * Adds the DRS stylesheet to a scene.
     *
     * @param scene the scene to style
     */
    private void addCss(Scene scene) {
        String css = getClass()
                .getResource("/drs/client/view/drs.css")
                .toExternalForm();
        scene.getStylesheets().add(css);
    } // end method addCss

    /**
     * Shows a fatal error dialog.
     *
     * @param message the error message
     */
    private void showFatal(String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("DRS Client Error");
        alert.setHeaderText("A fatal error occurred.");
        alert.setContentText(message);
        alert.showAndWait();
    } // end method showFatal

    /**
     * Application entry point.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        launch(args);
    } // end method main

} // end class MainApp
