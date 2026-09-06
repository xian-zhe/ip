package oz;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Controller for the main GUI.
 */
public class MainWindow extends AnchorPane {
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private VBox dialogContainer;
    @FXML
    private TextField userInput;
    @FXML
    private Button sendButton;

    private Oz oz;

    private Image userImage = new Image(this.getClass().getResourceAsStream("/images/DaUser2.png"));
    private Image ozImage = new Image(this.getClass().getResourceAsStream("/images/DaOz.png"));

    /**
     * Initializes the controller, binding the scroll pane to the dialog container's height.
     */
    @FXML
    public void initialize() {
        scrollPane.vvalueProperty().bind(dialogContainer.heightProperty());
    }

    /**
     * Injects the Oz chatbot instance into the controller.
     *
     * @param oz The Oz instance to inject.
     */
    public void setOz(Oz oz) {
        this.oz = oz;
    }

    /**
     * Creates two dialog boxes, one echoing user input and the other containing
     * Oz's reply and then appends them to
     * the dialog container. Clears the user input after processing.
     */
    @FXML
    private void handleUserInput() {
        String input = userInput.getText();
        String response = oz.getResponse(input);
        dialogContainer.getChildren().addAll(
                DialogBox.getUserDialog(input, userImage),
                DialogBox.getOzDialog(response, ozImage));
        userInput.clear();

        if (oz.isExit()) {
            userInput.setDisable(true);
            sendButton.setDisable(true);
            PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
            delay.setOnFinished((event) -> Platform.exit());
            delay.play();
        }
    }
}
