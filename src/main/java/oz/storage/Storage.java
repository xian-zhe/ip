package oz.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import oz.exception.OzException;
import oz.task.Task;
import oz.task.TaskList;

/**
 * Handles loading tasks from a storage file and saving tasks to the storage
 * file.
 */
public class Storage {
    /** Visual divider line for console output. */
    private static final String DIVIDER = "____________________________________________________________\n";

    /** Optional Unicode marker ignored when reading stored lines. */
    private static final String BYTE_ORDER_MARK = "\uFEFF";

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
                    Task task = TaskRecordParser.parse(line);
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
     * @throws OzException If the file cannot be written due to permissions or I/O error.
     */
    public void save(ArrayList<Task> tasks) throws OzException {
        assert tasks != null : "The task collection to save must be non-null";
        assert tasks.stream().noneMatch(task -> task == null)
                : "The task collection to save must not contain null tasks";
        if (Files.exists(this.filePath) && !Files.isWritable(this.filePath)) {
            throw new OzException("Access denied: storage file is read-only or write-protected.");
        }

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
            throw new OzException("Could not save tasks to storage file: "
                    + (exception.getMessage() != null ? exception.getMessage() : "permission denied."));
        }
    }

    /**
     * Saves the tasks from a TaskList to the storage file on the hard disk.
     *
     * @param taskList The TaskList instance to save.
     * @throws OzException If the file cannot be written due to permissions or I/O error.
     */
    public void save(TaskList taskList) throws OzException {
        save(taskList.getTasks());
    }

}
