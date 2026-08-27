/**
 * Represents a task with a specific deadline.
 */
public class Deadlines extends Task {
    protected String deadline;


    /**
     * Constructs a Deadlines task with description and deadline date/time.
     *
     * @param description Description of the deadline task.
     * @param deadline Date or time by which the task should be completed.
     */
    public Deadlines(String description, String deadline) {
        super(description);
        this.deadline = deadline;
    }

    /**
     * Converts the deadline task into a file storage representation.
     *
     * @return Formatted string for writing to file.
     */
    @Override
    public String toFileFormat() {
        return String.format("D | %s | %s", super.toFileFormat(), this.deadline);
    }

    @Override
    public String toString() {
        return String.format("[D][%s] %s (by: %s)", super.getStatusIcon(), this.description, this.deadline);
    }
}