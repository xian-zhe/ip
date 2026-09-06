package oz;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

    private final Label text;
    private final ImageView displayPicture;

    /**
     * Constructs a dialog box with the specified text and avatar image.
     *
     * @param text Message text to display.
     * @param image Avatar image of the speaker.
     */
    public DialogBox(String text, Image image) {
        this.text = new Label(text);
        this.displayPicture = new ImageView(image);

        // Styling the dialog box
        this.text.setWrapText(true);
        this.displayPicture.setFitWidth(100.0);
        this.displayPicture.setFitHeight(100.0);
        this.setAlignment(Pos.TOP_RIGHT);

        this.getChildren().addAll(this.text, this.displayPicture);
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
    }

    /**
     * Returns a dialog box for the user, aligned to the right.
     *
     * @param text Text of the user's message.
     * @param image User's avatar image.
     * @return DialogBox for the user.
     */
    public static DialogBox getUserDialog(String text, Image image) {
        return new DialogBox(text, image);
    }

    /**
     * Returns a dialog box for Oz, flipped so the avatar is on the left.
     *
     * @param text Text of Oz's response message.
     * @param image Oz's avatar image.
     * @return DialogBox for Oz.
     */
    public static DialogBox getOzDialog(String text, Image image) {
        DialogBox dialogBox = new DialogBox(text, image);
        dialogBox.flip();
        return dialogBox;
    }
}
