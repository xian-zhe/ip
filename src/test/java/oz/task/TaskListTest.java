package oz.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import oz.exception.OzException;

/**
 * Tests task management operations of {@link TaskList}.
 */
public class TaskListTest {

    @Test
    public void add_validTask_sizeIncreases() throws OzException {
        TaskList taskList = new TaskList();
        assertEquals(0, taskList.size());

        taskList.add(new ToDo("read book"));
        assertEquals(1, taskList.size());
        assertEquals("[T][ ] read book", taskList.get(0).toString());
    }

    @Test
    public void delete_validIndex_taskRemoved() throws OzException {
        TaskList taskList = new TaskList();
        taskList.add(new ToDo("task 1"));
        taskList.add(new ToDo("task 2"));

        Task removed = taskList.delete(0);
        assertEquals("[T][ ] task 1", removed.toString());
        assertEquals(1, taskList.size());
        assertEquals("[T][ ] task 2", taskList.get(0).toString());
    }

    @Test
    public void delete_outOfBoundsIndex_exceptionThrown() {
        TaskList taskList = new TaskList();
        taskList.add(new ToDo("only task"));

        OzException exception = assertThrows(OzException.class, () -> taskList.delete(5));
        assertEquals("That task number does not exist.", exception.getMessage());
    }

    @Test
    public void findTasksOn_matchingDate_returnsFilteredTasks() throws OzException {
        TaskList taskList = new TaskList();
        TaskDateTime targetDateTime = TaskDateTime.parse("2026-09-01 1800");
        TaskDateTime otherDateTime = TaskDateTime.parse("2026-09-05 1800");

        taskList.add(new Deadlines("submit report", targetDateTime));
        taskList.add(new Deadlines("pay bill", otherDateTime));
        taskList.add(new ToDo("read book"));

        ArrayList<Task> matching = taskList.findTasksOn(LocalDate.of(2026, 9, 1));
        assertEquals(1, matching.size());
        assertEquals("submit report", matching.get(0).description);
    }
}
