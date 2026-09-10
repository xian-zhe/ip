package oz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.util.Pair;

/**
 * Tests the response generation logic in {@link Oz} that drives GUI interactions.
 */
public class OzResponseTest {

    /** Temporary folder for test storage file. */
    @TempDir
    Path temporaryFolder;

    /** Chatbot instance under test. */
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
        Pair<String, CommandType> empty = this.oz.getResponse("");
        assertEquals("Please enter a command.", empty.getKey());
        assertEquals(CommandType.BLANK, empty.getValue());

        Pair<String, CommandType> blank = this.oz.getResponse("   ");
        assertEquals("Please enter a command.", blank.getKey());
        assertEquals(CommandType.BLANK, blank.getValue());

        Pair<String, CommandType> nullInput = this.oz.getResponse(null);
        assertEquals("Please enter a command.", nullInput.getKey());
        assertEquals(CommandType.BLANK, nullInput.getValue());
    }

    @Test
    public void getResponse_unknownCommand_returnsErrorMessage() {
        Pair<String, CommandType> response = this.oz.getResponse("unknownCommand");
        assertEquals("OOPS! Sorry, I do not understand that command.", response.getKey());
        assertEquals(CommandType.ERROR, response.getValue());
    }

    @Test
    public void getResponse_byeCommand_returnsFarewellMessage() {
        Pair<String, CommandType> response = this.oz.getResponse("bye");
        assertEquals("Bye. Hope to see you again soon! („• ֊ •„)੭", response.getKey());
        assertEquals(CommandType.BYE, response.getValue());
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
        Pair<String, CommandType> response = this.oz.getResponse("todo borrow book");
        assertTrue(response.getKey().contains("Got it. I've added this task:"));
        assertTrue(response.getKey().contains("[T][ ] borrow book"));
        assertTrue(response.getKey().contains("Now you have 1 tasks in the list."));
        assertEquals(CommandType.ADD, response.getValue());
    }

    @Test
    public void getResponse_todoCommandWithEmptyDescription_returnsErrorMessage() {
        Pair<String, CommandType> response = this.oz.getResponse("todo ");
        assertEquals("OOPS! The description of a todo cannot be empty.", response.getKey());
        assertEquals(CommandType.ERROR, response.getValue());
    }

    @Test
    public void getResponse_deadlinesCommand_taskAddedAndReported() {
        Pair<String, CommandType> response = this.oz.getResponse("deadline return book /by 2026-12-31");
        assertTrue(response.getKey().contains("Got it. I've added this task:"));
        assertTrue(response.getKey().contains("[D][ ] return book"));
        assertTrue(response.getKey().contains("Dec 31 2026"));
        assertEquals(CommandType.ADD, response.getValue());
    }

    @Test
    public void getResponse_eventCommand_taskAddedAndReported() {
        Pair<String, CommandType> response = this.oz.getResponse(
                "event project meeting /from 2026-10-10 14:00 /to 2026-10-10 16:00");
        assertTrue(response.getKey().contains("Got it. I've added this task:"));
        assertTrue(response.getKey().contains("[E][ ] project meeting"));
        assertEquals(CommandType.ADD, response.getValue());
    }

    @Test
    public void getResponse_markAndUnmarkCommands_updatesTaskStatus() {
        this.oz.getResponse("todo finish homework");
        Pair<String, CommandType> markResponse = this.oz.getResponse("mark 1");
        assertTrue(markResponse.getKey().contains("Nice! I've marked this task as done:"));
        assertTrue(markResponse.getKey().contains("[T][X] finish homework"));
        assertEquals(CommandType.CHANGE_MARK, markResponse.getValue());

        Pair<String, CommandType> unmarkResponse = this.oz.getResponse("unmark 1");
        assertTrue(unmarkResponse.getKey().contains("OK! I've marked this task as not done yet:"));
        assertTrue(unmarkResponse.getKey().contains("[T][ ] finish homework"));
        assertEquals(CommandType.CHANGE_MARK, unmarkResponse.getValue());
    }

    @Test
    public void getResponse_deleteCommand_removesTask() {
        this.oz.getResponse("todo task to remove");
        Pair<String, CommandType> deleteResponse = this.oz.getResponse("delete 1");
        assertTrue(deleteResponse.getKey().contains("Ok the following task has been removed!:"));
        assertTrue(deleteResponse.getKey().contains("[T][ ] task to remove"));
        assertTrue(deleteResponse.getKey().contains("Now you have 0 tasks in the list."));
        assertEquals(CommandType.DELETE, deleteResponse.getValue());
    }

    @Test
    public void getResponse_findCommand_returnsMatchingTasks() {
        this.oz.getResponse("todo read book");
        this.oz.getResponse("todo buy book");
        this.oz.getResponse("todo wash car");

        Pair<String, CommandType> findResponse = this.oz.getResponse("find book");
        assertTrue(findResponse.getKey().contains("Here are the matching tasks in your list:"));
        assertTrue(findResponse.getKey().contains("read book"));
        assertTrue(findResponse.getKey().contains("buy book"));
        assertEquals(CommandType.FIND, findResponse.getValue());
    }

    /** Verifies that invalid arguments are reported without changing the task list. */
    @Test
    public void getResponse_invalidArguments_returnsErrorWithoutAddingTasks() {
        String[] invalidCommands = {
            "list extra", "on", "on invalid-date", "find", "deadline return book",
            "deadline return book /by invalid-date", "event meeting",
            "event meeting /from 2026-10-11 /to 2026-10-10"
        };
        for (String command : invalidCommands) {
            Pair<String, CommandType> response = this.oz.getResponse(command);
            assertEquals(CommandType.ERROR, response.getValue(), command);
            assertTrue(response.getKey().startsWith("OOPS! "), command);
        }
        assertEquals("Here are the tasks in your list:", this.oz.getResponse("list").getKey());
    }

    /** Verifies task-number validation for all commands that select an existing task. */
    @Test
    public void getResponse_invalidTaskNumbers_preservesTask() {
        this.oz.getResponse("todo keep task");
        String[] commands = {"mark", "unmark", "delete"};
        String[] invalidNumbers = {"", "abc", "-1", "0", "2", "999999999999999999999"};
        for (String command : commands) {
            for (String number : invalidNumbers) {
                Pair<String, CommandType> response = this.oz.getResponse(command + " " + number);
                assertEquals(CommandType.ERROR, response.getValue(), command + " " + number);
            }
        }
        assertEquals("Here are the tasks in your list:\n1. [T][ ] keep task",
                this.oz.getResponse("list").getKey());
    }

    /** Verifies date filtering and local numbering when unrelated tasks precede the matches. */
    @Test
    public void getResponse_onCommand_listsOnlyTasksOccurringOnDate() {
        this.oz.getResponse("todo unrelated task");
        this.oz.getResponse("deadline return book /by 2026-10-10");
        this.oz.getResponse("event conference /from 2026-10-09 /to 2026-10-11");
        Pair<String, CommandType> response = this.oz.getResponse("  on  2026-10-10  ");
        assertEquals(CommandType.LIST, response.getValue());
        assertEquals("Here are the tasks occurring on Oct 10 2026:\n"
                + "1. [D][ ] return book (by: Oct 10 2026)\n"
                + "2. [E][ ] conference (from: Oct 09 2026 to: Oct 11 2026)", response.getKey());
        assertEquals("There are no tasks occurring on Jan 01 2027.",
                this.oz.getResponse("on 2027-01-01").getKey());
    }
}
