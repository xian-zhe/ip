package oz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import javafx.util.Pair;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests the response generation logic in {@link Oz} that drives GUI interactions.
 */
public class OzResponseTest {

    @TempDir
    Path temporaryFolder;

    private Oz oz;

    /**
     * Sets up an Oz instance backed by a temporary file before each test.
     */
    @BeforeEach
    public void setUp() {
        Path storageFile = temporaryFolder.resolve("test_tasks.txt");
        this.oz = new Oz(storageFile.toString());
    }

    @Test
    public void getResponse_emptyOrBlankInput_promptsUser() {
        Pair<String, String> empty = this.oz.getResponse("");
        assertEquals("Please enter a command.", empty.getKey());
        assertEquals("Blank", empty.getValue());

        Pair<String, String> blank = this.oz.getResponse("   ");
        assertEquals("Please enter a command.", blank.getKey());
        assertEquals("Blank", blank.getValue());

        Pair<String, String> nullInput = this.oz.getResponse(null);
        assertEquals("Please enter a command.", nullInput.getKey());
        assertEquals("Blank", nullInput.getValue());
    }

    @Test
    public void getResponse_unknownCommand_returnsErrorMessage() {
        Pair<String, String> response = this.oz.getResponse("unknownCommand");
        assertEquals("OOPS! Sorry, I do not understand that command.", response.getKey());
        assertEquals("Error", response.getValue());
    }

    @Test
    public void getResponse_byeCommand_returnsFarewellMessage() {
        Pair<String, String> response = this.oz.getResponse("bye");
        assertEquals("Bye. Hope to see you again soon! („• ֊ •„)੭", response.getKey());
        assertEquals("Bye", response.getValue());
        assertTrue(this.oz.isExit());
    }

    @Test
    public void isExit_defaultState_returnsFalse() {
        assertFalse(this.oz.isExit());
        this.oz.getResponse("list");
        assertFalse(this.oz.isExit());
    }

    @Test
    public void getResponse_todoCommand_taskAddedAndReported() {
        Pair<String, String> response = this.oz.getResponse("todo borrow book");
        assertTrue(response.getKey().contains("Got it. I've added this task:"));
        assertTrue(response.getKey().contains("[T][ ] borrow book"));
        assertTrue(response.getKey().contains("Now you have 1 tasks in the list."));
        assertEquals("AddCommand", response.getValue());
    }

    @Test
    public void getResponse_todoCommandWithEmptyDescription_returnsErrorMessage() {
        Pair<String, String> response = this.oz.getResponse("todo ");
        assertEquals("OOPS! The description of a todo cannot be empty.", response.getKey());
        assertEquals("Error", response.getValue());
    }

    @Test
    public void getResponse_deadlinesCommand_taskAddedAndReported() {
        Pair<String, String> response = this.oz.getResponse("deadline return book /by 2026-12-31");
        assertTrue(response.getKey().contains("Got it. I've added this task:"));
        assertTrue(response.getKey().contains("[D][ ] return book"));
        assertTrue(response.getKey().contains("Dec 31 2026"));
        assertEquals("AddCommand", response.getValue());
    }

    @Test
    public void getResponse_eventCommand_taskAddedAndReported() {
        Pair<String, String> response = this.oz.getResponse(
                "event project meeting /from 2026-10-10 14:00 /to 2026-10-10 16:00");
        assertTrue(response.getKey().contains("Got it. I've added this task:"));
        assertTrue(response.getKey().contains("[E][ ] project meeting"));
        assertEquals("AddCommand", response.getValue());
    }

    @Test
    public void getResponse_markAndUnmarkCommands_updatesTaskStatus() {
        this.oz.getResponse("todo finish homework");
        Pair<String, String> markResponse = this.oz.getResponse("mark 1");
        assertTrue(markResponse.getKey().contains("Nice! I've marked this task as done:"));
        assertTrue(markResponse.getKey().contains("[T][X] finish homework"));
        assertEquals("ChangeMarkCommand", markResponse.getValue());

        Pair<String, String> unmarkResponse = this.oz.getResponse("unmark 1");
        assertTrue(unmarkResponse.getKey().contains("OK! I've marked this task as not done yet:"));
        assertTrue(unmarkResponse.getKey().contains("[T][ ] finish homework"));
        assertEquals("ChangeMarkCommand", unmarkResponse.getValue());
    }

    @Test
    public void getResponse_deleteCommand_removesTask() {
        this.oz.getResponse("todo task to remove");
        Pair<String, String> deleteResponse = this.oz.getResponse("delete 1");
        assertTrue(deleteResponse.getKey().contains("Ok the following task has been removed!:"));
        assertTrue(deleteResponse.getKey().contains("[T][ ] task to remove"));
        assertTrue(deleteResponse.getKey().contains("Now you have 0 tasks in the list."));
        assertEquals("DeleteCommand", deleteResponse.getValue());
    }

    @Test
    public void getResponse_findCommand_returnsMatchingTasks() {
        this.oz.getResponse("todo read book");
        this.oz.getResponse("todo buy book");
        this.oz.getResponse("todo wash car");

        Pair<String, String> findResponse = this.oz.getResponse("find book");
        assertTrue(findResponse.getKey().contains("Here are the matching tasks in your list:"));
        assertTrue(findResponse.getKey().contains("read book"));
        assertTrue(findResponse.getKey().contains("buy book"));
        assertEquals("Find", findResponse.getValue());
    }
}
