package oz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

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
        assertEquals("Please enter a command.", this.oz.getResponse(""));
        assertEquals("Please enter a command.", this.oz.getResponse("   "));
        assertEquals("Please enter a command.", this.oz.getResponse(null));
    }

    @Test
    public void getResponse_unknownCommand_returnsErrorMessage() {
        String response = this.oz.getResponse("unknownCommand");
        assertEquals("OOPS! Sorry, I do not understand that command.", response);
    }

    @Test
    public void getResponse_byeCommand_returnsFarewellMessage() {
        String response = this.oz.getResponse("bye");
        assertEquals("Bye. Hope to see you again soon! („• ֊ •„)੭", response);
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
        String response = this.oz.getResponse("todo borrow book");
        assertTrue(response.contains("Got it. I've added this task:"));
        assertTrue(response.contains("[T][ ] borrow book"));
        assertTrue(response.contains("Now you have 1 tasks in the list."));
    }

    @Test
    public void getResponse_todoCommandWithEmptyDescription_returnsErrorMessage() {
        String response = this.oz.getResponse("todo ");
        assertEquals("OOPS! The description of a todo cannot be empty.", response);
    }

    @Test
    public void getResponse_deadlinesCommand_taskAddedAndReported() {
        String response = this.oz.getResponse("deadline return book /by 2026-12-31");
        assertTrue(response.contains("Got it. I've added this task:"));
        assertTrue(response.contains("[D][ ] return book"));
        assertTrue(response.contains("Dec 31 2026"));
    }

    @Test
    public void getResponse_eventCommand_taskAddedAndReported() {
        String response = this.oz.getResponse(
                "event project meeting /from 2026-10-10 14:00 /to 2026-10-10 16:00");
        assertTrue(response.contains("Got it. I've added this task:"));
        assertTrue(response.contains("[E][ ] project meeting"));
    }

    @Test
    public void getResponse_markAndUnmarkCommands_updatesTaskStatus() {
        this.oz.getResponse("todo finish homework");
        String markResponse = this.oz.getResponse("mark 1");
        assertTrue(markResponse.contains("Nice! I've marked this task as done:"));
        assertTrue(markResponse.contains("[T][X] finish homework"));

        String unmarkResponse = this.oz.getResponse("unmark 1");
        assertTrue(unmarkResponse.contains("OK! I've marked this task as not done yet:"));
        assertTrue(unmarkResponse.contains("[T][ ] finish homework"));
    }

    @Test
    public void getResponse_deleteCommand_removesTask() {
        this.oz.getResponse("todo task to remove");
        String deleteResponse = this.oz.getResponse("delete 1");
        assertTrue(deleteResponse.contains("Ok the following task has been removed!:"));
        assertTrue(deleteResponse.contains("[T][ ] task to remove"));
        assertTrue(deleteResponse.contains("Now you have 0 tasks in the list."));
    }

    @Test
    public void getResponse_findCommand_returnsMatchingTasks() {
        this.oz.getResponse("todo read book");
        this.oz.getResponse("todo buy book");
        this.oz.getResponse("todo wash car");

        String findResponse = this.oz.getResponse("find book");
        assertTrue(findResponse.contains("Here are the matching tasks in your list:"));
        assertTrue(findResponse.contains("read book"));
        assertTrue(findResponse.contains("buy book"));
    }
}
