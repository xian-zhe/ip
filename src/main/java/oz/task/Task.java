package oz.task;

import java.time.LocalDate;

/**
 * Represents a general task in the task list.
 */
public class Task {
    /** Description text of the task. */
    protected String description;
    /** Whether the task has been marked as completed. */
    protected boolean isDone;

    /**

     * Constructs a Task with the specified description.
     *
     * @param description Description of the task.
     */
    public Task(String description) {
        this.description = description;
        this.isDone = false;
    }

    /**
     * Returns the status icon indicating whether the task is completed.
     *
     * @return "X" if done, or " " if not done.
     */
    public String getStatusIcon() {
        return (isDone ? "X" : " "); // mark done task with X
    }

    /**
     * Marks the task as completed.
     */
    public void mark() {
        this.isDone = true;
    }

    /**
     * Marks the task as not completed.
     */
    public void unmark() {
        this.isDone = false;
    }

    /**
     * Checks if this task occurs on the given date.
     *
     * @param date The date to check against.
     * @return True if the task occurs on the specified date, false otherwise.
     */
    public boolean occursOn(LocalDate date) {
        return false;
    }

    /**
     * Converts the task into a file storage representation.
     *
     * @return Formatted string for writing to file.
     */
    public String toFileFormat() {
        return String.format("%s | %s", (this.isDone ? "1" : "0"), this.description);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s", this.getStatusIcon(), this.description);
    }
}
