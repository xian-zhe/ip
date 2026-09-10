package oz;

import java.io.IOException;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

/**
 * A custom control representing a dialog box consisting of an ImageView to
 * represent the speaker's avatar and a Label containing text from the speaker.
 */
public class DialogBox extends HBox {

    /** Text label for the dialog message. */
    @FXML
    private Label dialog;

    /** Avatar picture representing the speaker. */
    @FXML
    private ImageView displayPicture;

    /**
     * Constructs a dialog box with the specified text and avatar image.
     *
     * @param text  Message text to display.
     * @param image Avatar image of the speaker.
     */
    private DialogBox(String text, Image image) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(DialogBox.class.getResource("/view/DialogBox.fxml"));
            fxmlLoader.setController(this);
            fxmlLoader.setRoot(this);
            fxmlLoader.load();
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        this.dialog.setText(text);
        this.displayPicture.setImage(image);
    }

    /**
     * Flips the dialog box such that the ImageView is on the left and text on the
     * right.
     */
    private void flip() {
        this.setAlignment(Pos.TOP_LEFT);
        ObservableList<Node> temporaryChildren = FXCollections.observableArrayList(this.getChildren());
        FXCollections.reverse(temporaryChildren);
        this.getChildren().setAll(temporaryChildren);
        this.dialog.getStyleClass().add("reply-label");
    }

    /**
     * Returns a dialog box for the user, aligned to the right.
     *
     * @param text  Text of the user's message.
     * @param image User's avatar image.
     * @return DialogBox for the user.
     */
    public static DialogBox getUserDialog(String text, Image image) {
        return new DialogBox(text, image);
    }

    /**
     * Returns a dialog box for Oz with default styling, flipped so the avatar is on
     * the left.
     *
     * @param text  Text of Oz's response message.
     * @param image Oz's avatar image.
     * @return DialogBox for Oz.
     */
    public static DialogBox getOzDialog(String text, Image image) {
        return getOzDialog(text, image, CommandType.DEFAULT);
    }

    /**
     * Returns a dialog box for Oz with command-specific styling, flipped so the
     * avatar is on the left.
     *
     * @param text        Text of Oz's response message.
     * @param image       Oz's avatar image.
     * @param commandType Type of command used to style the dialog bubble.
     * @return DialogBox for Oz.
     */
    public static DialogBox getOzDialog(String text, Image image, CommandType commandType) {
        DialogBox dialogBox = new DialogBox(text, image);
        dialogBox.flip();
        dialogBox.changeDialogStyle(commandType);
        return dialogBox;
    }

    /**
     * Applies a command-specific style class to the dialog bubble.
     *
     * @param commandType The command type indicating which CSS style class to
     *                    apply.
     */
    private void changeDialogStyle(CommandType commandType) {
        if (commandType == null) {
            return;
        }

        switch (commandType) {
            case ADD:
                this.dialog.getStyleClass().add("add-label");
                break;
            case CHANGE_MARK:
                this.dialog.getStyleClass().add("marked-label");
                break;
            case DELETE:
                this.dialog.getStyleClass().add("delete-label");
                break;
            case ERROR:
                this.dialog.getStyleClass().add("error-label");
                break;
            default:
                // Do nothing for default or unspecified command types
                break;
        }
    }
}
