/**
 * Represents a task that spans a start date/time and an end date/time.
 */
public class Event extends Task {
    protected TaskDateTime from;
    protected TaskDateTime to;

    /**
     * Constructs an Event task with description, start time, and end time.
     *
     * @param description Description of the event.
     * @param from Start date/time of the event.
     * @param to End date/time of the event.
     * @throws OzException If the start date/time is after the end date/time.
     */
    public Event(String description, TaskDateTime from, TaskDateTime to) throws OzException {
        super(description);
        if (from.isAfter(to)) {
            throw new OzException("The start date/time (/from) cannot be after the end date/time (/to).");
        }
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
        return String.format("E | %s | %s | %s",
                super.toFileFormat(), this.from.toStorageString(), this.to.toStorageString());
    }

    @Override
    public String toString() {
        return String.format("[E][%s] %s (from: %s to: %s)",
                super.getStatusIcon(), this.description, this.from.toDisplayString(), this.to.toDisplayString());
    }
}
