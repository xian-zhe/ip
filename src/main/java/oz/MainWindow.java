package oz;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.Pair;

/**
 * Controller for the main GUI.
 */
public class MainWindow extends AnchorPane {
    /** Introductory guidance displayed when the GUI opens. */
    private static final String WELCOME_MESSAGE =
            "Welcome to Oz! Try 'list', 'todo Read a book', or 'find book'.";

    /** Time allowed for reading the farewell message before the application closes. */
    private static final Duration FAREWELL_DISPLAY_DURATION = Duration.seconds(1.5);

    /** Classpath location of the Oz avatar image. */
    private static final String OZ_IMAGE_PATH = "/images/DaOz.png";

    /** Scroll pane containing the chat dialog. */
    @FXML
    private ScrollPane scrollPane;

    /** Vertical container holding all user and bot dialog bubbles. */
    @FXML
    private VBox dialogContainer;

    /** Text input field for user commands. */
    @FXML
    private TextField userInput;

    /** Button triggering submission of user command. */
    @FXML
    private Button sendButton;

    /** The Oz chatbot instance handling command logic. */
    private Oz oz;

    /** Oz avatar image. */
    private Image ozImage = new Image(this.getClass().getResourceAsStream(OZ_IMAGE_PATH));

    /**
     * Initializes the controller, binding the scroll pane to the dialog container's height.
     */
    @FXML
    public void initialize() {
        scrollPane.vvalueProperty().bind(dialogContainer.heightProperty());
        sendButton.disableProperty().bind(Bindings.createBooleanBinding(() -> userInput.getText().isBlank(),
                userInput.textProperty()));
        Platform.runLater(userInput::requestFocus);
    }

    /**
     * Injects the Oz chatbot instance into the controller.
     *
     * @param oz The Oz instance to inject.
     */
    public void setOz(Oz oz) {
        this.oz = oz;
        dialogContainer.getChildren().add(DialogBox.getOzDialog(WELCOME_MESSAGE, ozImage));
    }

    /**
     * Creates two dialog boxes, one echoing user input and the other containing
     * Oz's reply and then appends them to
     * the dialog container. Clears the user input after processing.
     */
    @FXML
    private void handleUserInput() {
        String input = userInput.getText();
        if (input.isBlank()) {
            return;
        }

        Pair<String, CommandType> response = oz.getResponse(input);
        dialogContainer.getChildren().addAll(
                DialogBox.getUserDialog(input),
                DialogBox.getOzDialog(response.getKey(), ozImage, response.getValue()));
        userInput.clear();

        if (oz.isExit()) {
            userInput.setDisable(true);
            sendButton.disableProperty().unbind();
            sendButton.setDisable(true);
            PauseTransition delay = new PauseTransition(FAREWELL_DISPLAY_DURATION);
            delay.setOnFinished((event) -> Platform.exit());
            delay.play();
        } else {
            userInput.requestFocus();
        }
    }
}
