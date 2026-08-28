package oz.task;

import java.time.LocalDate;
import java.util.ArrayList;

import oz.exception.OzException;

/**
 * Represents the list of tasks and provides operations to manipulate tasks.
 */
public class TaskList {
    private final ArrayList<Task> tasks;

    /**
     * Constructs an empty TaskList.
     */
    public TaskList() {
        this.tasks = new ArrayList<>();
    }

    /**
     * Constructs a TaskList initialized with existing tasks.
     *
     * @param tasks Initial list of tasks.
     */
    public TaskList(ArrayList<Task> tasks) {
        this.tasks = tasks != null ? tasks : new ArrayList<>();
    }

    /**
     * Adds a task to the list.
     *
     * @param task The task to add.
     */
    public void add(Task task) {
        this.tasks.add(task);
    }

    /**
     * Removes and returns the task at the specified zero-based index.
     *
     * @param index Zero-based index of the task to remove.
     * @return The removed task.
     * @throws OzException If the index is out of bounds.
     */
    public Task delete(int index) throws OzException {
        checkIndex(index);
        return this.tasks.remove(index);
    }

    /**
     * Retrieves the task at the specified zero-based index.
     *
     * @param index Zero-based index of the task.
     * @return The task at the specified index.
     * @throws OzException If the index is out of bounds.
     */
    public Task get(int index) throws OzException {
        checkIndex(index);
        return this.tasks.get(index);
    }

    /**
     * Returns the total number of tasks in the list.
     *
     * @return Size of the task list.
     */
    public int size() {
        return this.tasks.size();
    }

    /**
     * Returns the underlying list of tasks.
     *
     * @return ArrayList containing the tasks.
     */
    public ArrayList<Task> getTasks() {
        return this.tasks;
    }

    /**
     * Finds and returns all tasks that occur on the specified date.
     *
     * @param date The date to filter tasks by.
     * @return List of matching tasks occurring on that date.
     */
    public ArrayList<Task> findTasksOn(LocalDate date) {
        ArrayList<Task> matchingTasks = new ArrayList<>();
        for (Task task : this.tasks) {
            if (task.occursOn(date)) {
                matchingTasks.add(task);
            }
        }
        return matchingTasks;
    }

    /**
     * Finds and returns all tasks whose descriptions contain the specified keyword.
     *
     * @param keyword The keyword to filter tasks by.
     * @return List of tasks matching the keyword.
     */
    public ArrayList<Task> findTasksByKeyword(String keyword) {
        ArrayList<Task> matchingTasks = new ArrayList<>();
        for (Task task : this.tasks) {
            if (task.containsKeyword(keyword)) {
                matchingTasks.add(task);
            }
        }
        return matchingTasks;
    }

    /**
     * Marks the task at the specified zero-based index as completed.

     *
     * @param index Zero-based index of the task.
     * @throws OzException If the index is out of bounds.
     */
    public void mark(int index) throws OzException {
        checkIndex(index);
        this.tasks.get(index).mark();
    }

    /**
     * Marks the task at the specified zero-based index as not completed.
     *
     * @param index Zero-based index of the task.
     * @throws OzException If the index is out of bounds.
     */
    public void unmark(int index) throws OzException {
        checkIndex(index);
        this.tasks.get(index).unmark();
    }

    /**
     * Validates that the specified zero-based index is within list bounds.
     *
     * @param index Index to check.
     * @throws OzException If the index is negative or out of range.
     */
    private void checkIndex(int index) throws OzException {
        if (index < 0 || index >= this.tasks.size()) {
            throw new OzException("That task number does not exist.");
        }
    }
}
