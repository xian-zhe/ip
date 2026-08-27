/**
 * Represents a task that spans a start time and an end time.
 */
public class Event extends Task {
    protected String from;
    protected String to;


    /**
     * Constructs an Event task with description, start time, and end time.
     *
     * @param description Description of the event.
     * @param from Start time of the event.
     * @param to End time of the event.
     */
    public Event(String description, String from, String to) {
        super(description);
        this.from = from;
        this.to = to;
    }

    /**
     * Converts the event task into a file storage representation.
     *
     * @return Formatted string for writing to file.
     */
    @Override
    public String toFileFormat() {
        return String.format("E | %s | %s | %s", super.toFileFormat(), this.from, this.to);
    }

    @Override
    public String toString() {
        return String.format("[E][%s] %s (from: %s to: %s)",
                super.getStatusIcon(), this.description, this.from, this.to);
    }
}