package oz.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import oz.exception.OzException;
import oz.task.Deadline;
import oz.task.Event;
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

    /** Number of fields in a todo entry and in the initial header split. */
    private static final int TODO_FIELD_COUNT = 3;

    /** Number of fields in a deadline entry. */
    private static final int DEADLINE_FIELD_COUNT = 4;

    /** Number of fields in an event entry. */
    private static final int EVENT_FIELD_COUNT = 5;

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
            throw new OzException("Malformed task entry: insufficient fields.");
        }

        String type = initialParts[TYPE_FIELD_INDEX].trim();
        String status = initialParts[STATUS_FIELD_INDEX].trim();
        if (!status.equals(Task.STORAGE_NOT_DONE) && !status.equals(Task.STORAGE_DONE)) {
            throw new OzException("Invalid completion status (must be 0 or 1): " + status);
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
            default:
                throw new OzException("Unknown task type: " + type);
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
            throw new OzException("Todo description cannot be empty.");
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
            throw new OzException("Deadline task requires description and deadline date.");
        }
        String deadlineDescription = deadlineParts[DESCRIPTION_FIELD_INDEX].trim();
        String deadlineTimeArgument = deadlineParts[DEADLINE_FIELD_INDEX].trim();
        if (deadlineDescription.isEmpty() || deadlineTimeArgument.isEmpty()) {
            throw new OzException("Deadline description and date cannot be empty.");
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
            throw new OzException("Event task requires description, start time, and end time.");
        }
        String eventDescription = eventParts[DESCRIPTION_FIELD_INDEX].trim();
        String fromTimeArgument = eventParts[EVENT_START_FIELD_INDEX].trim();
        String toTimeArgument = eventParts[EVENT_END_FIELD_INDEX].trim();
        if (eventDescription.isEmpty() || fromTimeArgument.isEmpty() || toTimeArgument.isEmpty()) {
            throw new OzException("Event description, start time, and end time cannot be empty.");
        }
        TaskDateTime fromTime = TaskDateTime.parse(fromTimeArgument);
        TaskDateTime toTime = TaskDateTime.parse(toTimeArgument);
        return new Event(eventDescription, fromTime, toTime);
    }
}
