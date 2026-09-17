package oz;

import java.io.IOException;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

/**
 * A custom control representing a dialog box consisting of an avatar and a
 * selectable message area.
 */
public class DialogBox extends HBox {

    /** Maximum share of a dialog row occupied by its text bubble. */
    private static final double DIALOG_WIDTH_RATIO = 0.75;

    /** Space reserved around the measured message text. */
    private static final double DIALOG_CONTENT_PADDING = 20;

    /** Minimum height of a single-line message bubble. */
    private static final double MINIMUM_DIALOG_HEIGHT = 34;

    /** Label for the action that copies a message to the system clipboard. */
    private static final String COPY_MESSAGE_MENU_TEXT = "Copy message";

    /** Classpath location of the dialog box layout. */
    private static final String DIALOG_BOX_FXML_PATH = "/view/DialogBox.fxml";

    /** CSS class applied to every Oz reply. */
    private static final String REPLY_LABEL_STYLE_CLASS = "reply-label";

    /** CSS class applied to successful task additions. */
    private static final String ADD_LABEL_STYLE_CLASS = "add-label";

    /** CSS class applied to task completion changes. */
    private static final String MARKED_LABEL_STYLE_CLASS = "marked-label";

    /** CSS class applied to task deletions. */
    private static final String DELETE_LABEL_STYLE_CLASS = "delete-label";

    /** CSS class applied to errors. */
    private static final String ERROR_LABEL_STYLE_CLASS = "error-label";

    /** Read-only selectable area containing the dialog message. */
    @FXML
    private TextArea dialog;

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
            FXMLLoader fxmlLoader = new FXMLLoader(DialogBox.class.getResource(DIALOG_BOX_FXML_PATH));
            fxmlLoader.setController(this);
            fxmlLoader.setRoot(this);
            fxmlLoader.load();
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        this.dialog.setText(text);
        this.displayPicture.setImage(image);
        this.dialog.maxWidthProperty().bind(this.widthProperty().multiply(DIALOG_WIDTH_RATIO));
        this.dialog.widthProperty().addListener((observable, oldWidth, newWidth) -> updateDialogHeight());
        this.dialog.fontProperty().addListener((observable, oldFont, newFont) -> updateDialogHeight());
        configureCopyMenu();
    }

    /** Updates the message area height to display all wrapped text without an inner scrollbar. */
    private void updateDialogHeight() {
        double availableTextWidth = dialog.getWidth() - DIALOG_CONTENT_PADDING;
        if (availableTextWidth <= 0) {
            return;
        }

        Text textMeasurement = new Text(dialog.getText());
        textMeasurement.setFont(dialog.getFont());
        textMeasurement.setWrappingWidth(availableTextWidth);
        double requiredHeight = textMeasurement.getLayoutBounds().getHeight() + DIALOG_CONTENT_PADDING;
        dialog.setPrefHeight(Math.max(MINIMUM_DIALOG_HEIGHT, Math.ceil(requiredHeight)));
    }

    /** Adds a context-menu action that copies this dialog's complete text. */
    private void configureCopyMenu() {
        MenuItem copyMessageItem = new MenuItem(COPY_MESSAGE_MENU_TEXT);
        copyMessageItem.setOnAction((event) -> {
            ClipboardContent clipboardContent = new ClipboardContent();
            clipboardContent.putString(dialog.getText());
            Clipboard.getSystemClipboard().setContent(clipboardContent);
        });
        dialog.setContextMenu(new ContextMenu(copyMessageItem));
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
        this.dialog.getStyleClass().add(REPLY_LABEL_STYLE_CLASS);
    }

    /**
     * Returns a dialog box for the user, aligned to the right.
     *
     * @param text Text of the user's message.
     * @return DialogBox for the user.
     */
    public static DialogBox getUserDialog(String text) {
        DialogBox dialogBox = new DialogBox(text, null);
        dialogBox.getChildren().remove(dialogBox.displayPicture);
        return dialogBox;
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
                this.dialog.getStyleClass().add(ADD_LABEL_STYLE_CLASS);
                break;
            case CHANGE_MARK:
                this.dialog.getStyleClass().add(MARKED_LABEL_STYLE_CLASS);
                break;
            case DELETE:
                this.dialog.getStyleClass().add(DELETE_LABEL_STYLE_CLASS);
                break;
            case ERROR:
                this.dialog.getStyleClass().add(ERROR_LABEL_STYLE_CLASS);
                break;
            default:
                // Do nothing for default or unspecified command types
                break;
        }
    }
}
