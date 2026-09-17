package oz;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

/**
 * A graphical user interface application for Oz using JavaFX.
 */
public class Main extends Application {

    /** Application title displayed on the primary stage window. */
    public static final String APP_TITLE = "Oz";

    /** Classpath location of the main window layout. */
    private static final String MAIN_WINDOW_FXML_PATH = "/view/MainWindow.fxml";

    /** Classpath location of the application icon. */
    private static final String APP_ICON_IMAGE_PATH = "/images/icon.png";

    /** The Oz chatbot instance handling application logic. */
    private Oz oz = new Oz(Oz.DEFAULT_STORAGE_PATH);

    /**
     * Initializes and configures the main GUI layout and displays the primary
     * stage.
     *
     * @param stage Primary stage for this JavaFX application.
     */
    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource(MAIN_WINDOW_FXML_PATH));
            AnchorPane mainLayout = fxmlLoader.load();
            Scene scene = new Scene(mainLayout);
            stage.setTitle(APP_TITLE);
            stage.getIcons().add(new Image(Main.class.getResourceAsStream(APP_ICON_IMAGE_PATH)));
            stage.setScene(scene);
            fxmlLoader.<MainWindow>getController().setOz(oz); // Injects the Oz instance
            stage.show();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}
