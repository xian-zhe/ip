package oz.task;

/**
 * Represents a simple todo task without date or time constraints.
 */
public class ToDo extends Task {

    /**
     * Constructs a ToDo task with the given description.
     *
     * @param description Description of the todo task.
     */
    public ToDo(String description) {
        super(description);
    }

    /**
     * Converts the todo task into a file storage representation.
     *
     * @return Formatted string for writing to file.
     */
    @Override
    public String toFileFormat() {
        return String.format("T | %s", super.toFileFormat());
    }

    @Override
    public String toString() {
        return String.format("[T][%s] %s", super.getStatusIcon(), this.description);
    }
}
