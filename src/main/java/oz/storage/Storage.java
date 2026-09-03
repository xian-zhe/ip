package oz.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import oz.exception.OzException;
import oz.task.Deadlines;
import oz.task.Event;
import oz.task.Task;
import oz.task.TaskDateTime;
import oz.task.TaskList;
import oz.task.ToDo;

/**
 * Handles loading tasks from a storage file and saving tasks to the storage file.
 */
public class Storage {
    private static final String DIVIDER = "____________________________________________________________\n";
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
                if (line.startsWith("\uFEFF")) {
                    line = line.substring(1).trim();
                }
                if (line.isEmpty()) {
                    continue;
                }
                try {
                    Task task = parseTaskFromFile(line);
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
     * @throws OzException If the line format is invalid or has missing/corrupted fields.
     */
    private Task parseTaskFromFile(String line) throws OzException {
        String[] initialParts = line.split("\\s*\\|\\s*", 3);
        if (initialParts.length < 3) {
            throw new OzException("Malformed task entry: insufficient fields.");
        }

        String type = initialParts[0].trim();
        String status = initialParts[1].trim();
        if (!status.equals("0") && !status.equals("1")) {
            throw new OzException("Invalid completion status (must be 0 or 1): " + status);
        }
        boolean isDone = status.equals("1");

        Task task;
        switch (type) {
            case "T":
                String todoDescription = initialParts[2].trim();
                if (todoDescription.isEmpty()) {
                    throw new OzException("Todo description cannot be empty.");
                }
                task = new ToDo(todoDescription);
                break;

            case "D":
                String[] deadlineParts = line.split("\\s*\\|\\s*", 4);
                if (deadlineParts.length < 4) {
                    throw new OzException("Deadline task requires description and deadline date.");
                }
                String deadlineDescription = deadlineParts[2].trim();
                String deadlineTimeArgument = deadlineParts[3].trim();
                if (deadlineDescription.isEmpty() || deadlineTimeArgument.isEmpty()) {
                    throw new OzException("Deadline description and date cannot be empty.");
                }
                TaskDateTime deadlineTime = TaskDateTime.parse(deadlineTimeArgument);
                task = new Deadlines(deadlineDescription, deadlineTime);
                break;

            case "E":
                String[] eventParts = line.split("\\s*\\|\\s*", 5);
                if (eventParts.length < 5) {
                    throw new OzException("Event task requires description, start time, and end time.");
                }
                String eventDescription = eventParts[2].trim();
                String fromTimeArgument = eventParts[3].trim();
                String toTimeArgument = eventParts[4].trim();
                if (eventDescription.isEmpty() || fromTimeArgument.isEmpty() || toTimeArgument.isEmpty()) {
                    throw new OzException("Event description, start time, and end time cannot be empty.");
                }
                TaskDateTime fromTime = TaskDateTime.parse(fromTimeArgument);
                TaskDateTime toTime = TaskDateTime.parse(toTimeArgument);
                task = new Event(eventDescription, fromTime, toTime);
                break;

            default:
                throw new OzException("Unknown task type: " + type);
        }




        if (isDone) {
            task.mark();
        }
        return task;
    }
}
