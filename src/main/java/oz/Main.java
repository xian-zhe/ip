package oz;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

/**
 * A graphical user interface application for Oz using JavaFX.
 */
public class Main extends Application {

    private Oz oz = new Oz("data/oz.txt");

    /**
     * Initializes and configures the main GUI layout and displays the primary
     * stage.
     *
     * @param stage Primary stage for this JavaFX application.
     */
    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/view/MainWindow.fxml"));
            AnchorPane mainLayout = fxmlLoader.load();
            Scene scene = new Scene(mainLayout);
            stage.setScene(scene);
            fxmlLoader.<MainWindow>getController().setOz(oz); // Injects the Oz instance
            stage.show();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}
