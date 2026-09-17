package oz.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import oz.exception.OzException;
import oz.task.Deadline;
import oz.task.Event;
import oz.task.RecurringEvent;
import oz.task.Task;
import oz.task.TaskDateTime;
import oz.task.TaskList;
import oz.task.ToDo;

/**
 * Handles loading tasks from a storage file and saving tasks to the storage
 * file.
 */
public class Storage {
    /** Visual divider line for console output. */
    private static final String DIVIDER = "____________________________________________________________\n";

    /** Separator accepting optional whitespace around a stored pipe. */
    private static final String FIELD_SEPARATOR_PATTERN = "\\s*\\|\\s*";

    /** Optional Unicode marker ignored when reading stored lines. */
    private static final String BYTE_ORDER_MARK = "\uFEFF";

    /** Error shown when a stored record has too few fields. */
    private static final String INSUFFICIENT_FIELDS_MESSAGE =
            "Malformed task entry: insufficient fields.";

    /** Template used when a stored completion status is invalid. */
    private static final String INVALID_STATUS_MESSAGE_FORMAT =
            "Invalid completion status (must be 0 or 1): %s";

    /** Error shown when a recurring record has a global completed status. */
    private static final String INVALID_RECURRING_STATUS_MESSAGE =
            "A recurring series status must be 0.";

    /** Template used when a stored task type is unknown. */
    private static final String UNKNOWN_TASK_TYPE_MESSAGE_FORMAT = "Unknown task type: %s";

    /** Error shown when a stored todo description is missing. */
    private static final String EMPTY_TODO_DESCRIPTION_MESSAGE =
            "Todo description cannot be empty.";

    /** Error shown when a deadline record has too few fields. */
    private static final String DEADLINE_FIELDS_MESSAGE =
            "Deadline task requires description and deadline date.";

    /** Error shown when a deadline record contains an empty required field. */
    private static final String EMPTY_DEADLINE_FIELD_MESSAGE =
            "Deadline description and date cannot be empty.";

    /** Error shown when an event record has too few fields. */
    private static final String EVENT_FIELDS_MESSAGE =
            "Event task requires description, start time, and end time.";

    /** Error shown when an event record contains an empty required field. */
    private static final String EMPTY_EVENT_FIELD_MESSAGE =
            "Event description, start time, and end time cannot be empty.";

    /** Error shown when a recurring record has too few fields. */
    private static final String RECURRING_FIELDS_MESSAGE =
            "Recurring event requires all recurrence fields.";

    /** Error shown when a recurring record contains an empty required field. */
    private static final String EMPTY_RECURRING_FIELD_MESSAGE =
            "Recurring event fields cannot be empty.";

    /** Error shown when a stored recurrence interval is not positive. */
    private static final String INVALID_RECURRING_INTERVAL_MESSAGE =
            "Recurring interval must be a positive whole number.";

    /** Position of the task type code. */
    private static final int TYPE_FIELD_INDEX = 0;

    /** Position of the completion status code. */
    private static final int STATUS_FIELD_INDEX = 1;

    /** Position of the task description. */
    private static final int DESCRIPTION_FIELD_INDEX = 2;

    /** Position of a deadline's date or date-time. */
    private static final int DEADLINE_FIELD_INDEX = 3;

    /** Position of an event's start date or date-time. */
    private static final int EVENT_START_FIELD_INDEX = 3;

    /** Position of an event's end date or date-time. */
    private static final int EVENT_END_FIELD_INDEX = 4;

    /** Position of a recurring event's first start date or date-time. */
    private static final int RECURRING_START_FIELD_INDEX = 3;

    /** Position of a recurring event's first end date or date-time. */
    private static final int RECURRING_END_FIELD_INDEX = 4;

    /** Position of a recurring event's weekly interval. */
    private static final int RECURRING_INTERVAL_FIELD_INDEX = 5;

    /** Position of a recurring event's optional inclusive end date. */
    private static final int RECURRING_UNTIL_FIELD_INDEX = 6;

    /** Position of a recurring event's completed occurrence dates. */
    private static final int RECURRING_COMPLETED_FIELD_INDEX = 7;

    /** Number of fields in a todo entry and in the initial header split. */
    private static final int TODO_FIELD_COUNT = 3;

    /** Number of fields in a deadline entry. */
    private static final int DEADLINE_FIELD_COUNT = 4;

    /** Number of fields in an event entry. */
    private static final int EVENT_FIELD_COUNT = 5;

    /** Number of fields in a recurring event entry. */
    private static final int RECURRING_EVENT_FIELD_COUNT = 8;

    /** Path to the task storage file on disk. */
    private final Path filePath;

    /**
     * Constructs a Storage object with the specified file path.
     *
     * @param filePath Path to the storage file.
     */
    public Storage(String filePath) {
        this.filePath = Path.of(filePath);
    }

    /**
     * Loads tasks from the storage file on the hard disk.
     * If the file does not exist, an empty list is returned.
     * Corrupted lines are reported and skipped.
     *
     * @return List of tasks loaded from the storage file.
     */
    public ArrayList<Task> load() {
        ArrayList<Task> tasks = new ArrayList<>();
        if (!Files.exists(this.filePath)) {
            return tasks;
        }

        try {
            List<String> lines = Files.readAllLines(this.filePath);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.startsWith(BYTE_ORDER_MARK)) {
                    line = line.substring(BYTE_ORDER_MARK.length()).trim();
                }
                if (line.isEmpty()) {
                    continue;
                }
                try {
                    Task task = parseTaskLine(line);
                    tasks.add(task);
                } catch (OzException exception) {
                    System.out.println(DIVIDER + "WARNING: Skipping corrupted task entry at line "
                            + (i + 1) + ": " + exception.getMessage() + "\n" + DIVIDER);
                }
            }
        } catch (IOException exception) {
            System.out.println(DIVIDER + "OOPS! Could not read tasks from file: "
                    + exception.getMessage() + "\n" + DIVIDER);
        }
        return tasks;
    }

    /**
     * Saves the list of tasks to the storage file on the hard disk.
     *
     * @param tasks The list of tasks to save.
     */
    public void save(ArrayList<Task> tasks) {
        assert tasks != null : "The task collection to save must be non-null";
        assert tasks.stream().noneMatch(task -> task == null)
                : "The task collection to save must not contain null tasks";
        try {
            if (this.filePath.getParent() != null) {
                Files.createDirectories(this.filePath.getParent());
            }
            List<String> lines = new ArrayList<>();
            for (Task task : tasks) {
                lines.add(task.toFileFormat());
            }
            Files.write(this.filePath, lines);
        } catch (IOException exception) {
            System.out.println(DIVIDER + "OOPS! Could not save tasks to file: "
                    + exception.getMessage() + "\n" + DIVIDER);
        }
    }

    /**
     * Saves the tasks from a TaskList to the storage file on the hard disk.
     *
     * @param taskList The TaskList instance to save.
     */
    public void save(TaskList taskList) {
        save(taskList.getTasks());
    }

    /**
     * Parses a line from the storage file into a corresponding Task object.
     *
     * @param line Raw line text from storage.
     * @return A Task instance with status and descriptions populated.
     * @throws OzException If the line format is invalid or has missing/corrupted
     *                     fields.
     */
    private Task parseTaskLine(String line) throws OzException {
        String[] initialParts = line.split(FIELD_SEPARATOR_PATTERN, TODO_FIELD_COUNT);
        if (initialParts.length < TODO_FIELD_COUNT) {
            throw new OzException(INSUFFICIENT_FIELDS_MESSAGE);
        }

        String type = initialParts[TYPE_FIELD_INDEX].trim();
        String status = initialParts[STATUS_FIELD_INDEX].trim();
        if (!status.equals(Task.STORAGE_NOT_DONE) && !status.equals(Task.STORAGE_DONE)) {
            throw new OzException(String.format(INVALID_STATUS_MESSAGE_FORMAT, status));
        }
        if (type.equals(RecurringEvent.TYPE_CODE)
                && !status.equals(RecurringEvent.STORAGE_SERIES_STATUS)) {
            throw new OzException(INVALID_RECURRING_STATUS_MESSAGE);
        }
        boolean isDone = status.equals(Task.STORAGE_DONE);

        Task task = parseTaskDetails(type, line);
        assert task != null : "A recognized task type must produce a task";
        if (isDone) {
            task.markAsDone();
        }
        return task;
    }

    /**
     * Selects the parser for the stored task type.
     *
     * @param type Stored task type code.
     * @param line Complete task entry from storage.
     * @return Parsed task before its completion status is restored.
     * @throws OzException If the type is unknown or its fields are invalid.
     */
    private Task parseTaskDetails(String type, String line) throws OzException {
        switch (type) {
            case ToDo.TYPE_CODE:
                return parseTodo(line);
            case Deadline.TYPE_CODE:
                return parseDeadline(line);
            case Event.TYPE_CODE:
                return parseEvent(line);
            case RecurringEvent.TYPE_CODE:
                return parseRecurringEvent(line);
            default:
                throw new OzException(String.format(UNKNOWN_TASK_TYPE_MESSAGE_FORMAT, type));
        }
    }

    /**
     * Parses and validates a stored todo task.
     *
     * @param line Complete task entry from storage.
     * @return Task with its stored description.
     * @throws OzException If required fields are missing or invalid.
     */
    private Task parseTodo(String line) throws OzException {
        // Limit splitting so a todo description can contain literal pipe characters.
        String[] todoParts = line.split(FIELD_SEPARATOR_PATTERN, TODO_FIELD_COUNT);
        String todoDescription = todoParts[DESCRIPTION_FIELD_INDEX].trim();
        if (todoDescription.isEmpty()) {
            throw new OzException(EMPTY_TODO_DESCRIPTION_MESSAGE);
        }
        return new ToDo(todoDescription);
    }

    /**
     * Parses and validates a stored deadline task.
     *
     * @param line Complete task entry from storage.
     * @return Task with its stored description and date fields.
     * @throws OzException If required fields are missing or invalid.
     */
    private Task parseDeadline(String line) throws OzException {
        String[] deadlineParts = line.split(FIELD_SEPARATOR_PATTERN, DEADLINE_FIELD_COUNT);
        if (deadlineParts.length < DEADLINE_FIELD_COUNT) {
            throw new OzException(DEADLINE_FIELDS_MESSAGE);
        }
        String deadlineDescription = deadlineParts[DESCRIPTION_FIELD_INDEX].trim();
        String deadlineTimeArgument = deadlineParts[DEADLINE_FIELD_INDEX].trim();
        if (deadlineDescription.isEmpty() || deadlineTimeArgument.isEmpty()) {
            throw new OzException(EMPTY_DEADLINE_FIELD_MESSAGE);
        }
        TaskDateTime deadlineTime = TaskDateTime.parse(deadlineTimeArgument);
        return new Deadline(deadlineDescription, deadlineTime);
    }

    /**
     * Parses and validates a stored event task.
     *
     * @param line Complete task entry from storage.
     * @return Task with its stored description and date fields.
     * @throws OzException If required fields are missing or invalid.
     */
    private Task parseEvent(String line) throws OzException {
        String[] eventParts = line.split(FIELD_SEPARATOR_PATTERN, EVENT_FIELD_COUNT);
        if (eventParts.length < EVENT_FIELD_COUNT) {
            throw new OzException(EVENT_FIELDS_MESSAGE);
        }
        String eventDescription = eventParts[DESCRIPTION_FIELD_INDEX].trim();
        String fromTimeArgument = eventParts[EVENT_START_FIELD_INDEX].trim();
        String toTimeArgument = eventParts[EVENT_END_FIELD_INDEX].trim();
        if (eventDescription.isEmpty() || fromTimeArgument.isEmpty() || toTimeArgument.isEmpty()) {
            throw new OzException(EMPTY_EVENT_FIELD_MESSAGE);
        }
        TaskDateTime fromTime = TaskDateTime.parse(fromTimeArgument);
        TaskDateTime toTime = TaskDateTime.parse(toTimeArgument);
        return new Event(eventDescription, fromTime, toTime);
    }

    /**
     * Parses and validates a stored weekly recurring event.
     *
     * @param line Complete recurring event entry from storage.
     * @return Recurring event with its occurrence completion state restored.
     * @throws OzException If required fields or completed occurrence dates are invalid.
     */
    private Task parseRecurringEvent(String line) throws OzException {
        String[] recurringParts = line.split(FIELD_SEPARATOR_PATTERN, RECURRING_EVENT_FIELD_COUNT);
        if (recurringParts.length < RECURRING_EVENT_FIELD_COUNT) {
            throw new OzException(RECURRING_FIELDS_MESSAGE);
        }

        String description = recurringParts[DESCRIPTION_FIELD_INDEX].trim();
        String firstStartArgument = recurringParts[RECURRING_START_FIELD_INDEX].trim();
        String firstEndArgument = recurringParts[RECURRING_END_FIELD_INDEX].trim();
        String intervalArgument = recurringParts[RECURRING_INTERVAL_FIELD_INDEX].trim();
        String untilArgument = recurringParts[RECURRING_UNTIL_FIELD_INDEX].trim();
        String completedDatesArgument = recurringParts[RECURRING_COMPLETED_FIELD_INDEX].trim();
        if (description.isEmpty() || firstStartArgument.isEmpty() || firstEndArgument.isEmpty()
                || intervalArgument.isEmpty() || untilArgument.isEmpty()
                || completedDatesArgument.isEmpty()) {
            throw new OzException(EMPTY_RECURRING_FIELD_MESSAGE);
        }

        int weekInterval = parsePositiveInterval(intervalArgument);
        LocalDate untilDate = untilArgument.equals(RecurringEvent.STORAGE_NONE)
                ? null
                : TaskDateTime.parseDate(untilArgument);
        RecurringEvent recurringEvent = new RecurringEvent(description,
                TaskDateTime.parse(firstStartArgument), TaskDateTime.parse(firstEndArgument),
                weekInterval, untilDate);
        restoreCompletedOccurrences(recurringEvent, completedDatesArgument);
        return recurringEvent;
    }

    /**
     * Parses a positive weekly interval stored in a recurring record.
     *
     * @param argument Stored interval value.
     * @return Positive interval.
     * @throws OzException If the value is not a positive integer.
     */
    private int parsePositiveInterval(String argument) throws OzException {
        if (!argument.matches("\\d+")) {
            throw new OzException(INVALID_RECURRING_INTERVAL_MESSAGE);
        }

        try {
            int interval = Integer.parseInt(argument);
            if (interval <= 0) {
                throw new OzException(INVALID_RECURRING_INTERVAL_MESSAGE);
            }
            return interval;
        } catch (NumberFormatException exception) {
            throw new OzException(INVALID_RECURRING_INTERVAL_MESSAGE);
        }
    }

    /**
     * Restores occurrence completion dates from a stored comma-separated list.
     *
     * @param recurringEvent Recurring series being restored.
     * @param argument Stored completion dates or the no-value marker.
     * @throws OzException If any date is invalid, duplicated, or not an occurrence.
     */
    private void restoreCompletedOccurrences(RecurringEvent recurringEvent, String argument)
            throws OzException {
        if (argument.equals(RecurringEvent.STORAGE_NONE)) {
            return;
        }

        String[] completedDates = argument.split(",", -1);
        for (String completedDate : completedDates) {
            recurringEvent.markOccurrence(TaskDateTime.parseDate(completedDate.trim()));
        }
    }
}
