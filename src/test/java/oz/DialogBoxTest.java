package oz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests the GUI presentation components in {@link DialogBox}.
 */
public class DialogBoxTest {

    /** 1x1 pixel image used as dummy avatar for testing. */
    private static Image dummyImage;

    /**
     * Initializes the JavaFX toolkit once before running GUI unit tests.
     */
    @BeforeAll
    public static void initJavaFx() {
        try {
            Platform.startup(() -> {
            });
        } catch (IllegalStateException exception) {
            // Toolkit has already been initialized
        }
        dummyImage = new WritableImage(1, 1);
    }

    @Test
    public void getUserDialog_validTextAndImage_alignedRightWithLabelFirst() {
        String message = "User test message";
        DialogBox dialogBox = DialogBox.getUserDialog(message, dummyImage);

        assertEquals(Pos.TOP_RIGHT, dialogBox.getAlignment());
        assertEquals(2, dialogBox.getChildren().size());

        assertInstanceOf(Label.class, dialogBox.getChildren().get(0));
        assertInstanceOf(ImageView.class, dialogBox.getChildren().get(1));

        Label label = (Label) dialogBox.getChildren().get(0);
        assertEquals(message, label.getText());
        assertTrue(label.isWrapText());
        assertFalse(label.getStyleClass().contains("reply-label"));

        ImageView imageView = (ImageView) dialogBox.getChildren().get(1);
        assertEquals(dummyImage, imageView.getImage());
        assertEquals(99.0, imageView.getFitWidth());
        assertEquals(99.0, imageView.getFitHeight());
    }

    @Test
    public void getOzDialog_validTextAndImage_alignedLeftWithImageViewFirst() {
        String message = "Oz reply message";
        DialogBox dialogBox = DialogBox.getOzDialog(message, dummyImage);

        assertEquals(Pos.TOP_LEFT, dialogBox.getAlignment());
        assertEquals(2, dialogBox.getChildren().size());

        // Due to flipping, ImageView is at index 0 and Label is at index 1
        assertInstanceOf(ImageView.class, dialogBox.getChildren().get(0));
        assertInstanceOf(Label.class, dialogBox.getChildren().get(1));

        ImageView imageView = (ImageView) dialogBox.getChildren().get(0);
        assertEquals(dummyImage, imageView.getImage());

        Label label = (Label) dialogBox.getChildren().get(1);
        assertEquals(message, label.getText());
        assertTrue(label.isWrapText());
        assertTrue(label.getStyleClass().contains("reply-label"));
    }

    @Test
    public void getUserDialog_multilineText_preservesTextAndFormatting() {
        String multilineMessage = "Here are the tasks:\n1. [T][ ] task 1\n2. [T][X] task 2";
        DialogBox dialogBox = DialogBox.getUserDialog(multilineMessage, dummyImage);

        Label label = (Label) dialogBox.getChildren().get(0);
        assertEquals(multilineMessage, label.getText());
    }

    @Test
    public void getOzDialog_commandTypes_appliesCorrectStyles() {
        DialogBox addDialog = DialogBox.getOzDialog("Added", dummyImage, CommandType.ADD);
        Label addLabel = (Label) addDialog.getChildren().get(1);
        assertTrue(addLabel.getStyleClass().contains("add-label"));

        DialogBox markedDialog = DialogBox.getOzDialog("Marked", dummyImage, CommandType.CHANGE_MARK);
        Label markedLabel = (Label) markedDialog.getChildren().get(1);
        assertTrue(markedLabel.getStyleClass().contains("marked-label"));

        DialogBox deleteDialog = DialogBox.getOzDialog("Deleted", dummyImage, CommandType.DELETE);
        Label deleteLabel = (Label) deleteDialog.getChildren().get(1);
        assertTrue(deleteLabel.getStyleClass().contains("delete-label"));

        DialogBox errorDialog = DialogBox.getOzDialog("Error", dummyImage, CommandType.ERROR);
        Label errorLabel = (Label) errorDialog.getChildren().get(1);
        assertTrue(errorLabel.getStyleClass().contains("error-label"));

        DialogBox defaultDialog = DialogBox.getOzDialog("Default", dummyImage, CommandType.DEFAULT);
        Label defaultLabel = (Label) defaultDialog.getChildren().get(1);
        assertFalse(defaultLabel.getStyleClass().contains("add-label"));
        assertFalse(defaultLabel.getStyleClass().contains("marked-label"));
        assertFalse(defaultLabel.getStyleClass().contains("delete-label"));
        assertFalse(defaultLabel.getStyleClass().contains("error-label"));
    }
}
