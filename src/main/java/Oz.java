import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Main entry point and controller for the Oz chatbot.
 */
public class Oz {
    private static final String DIVIDER = "____________________________________________________________\n";
    private static final Path FILE_PATH = Path.of("data", "oz.txt");
    private static final Pattern COMMAND_PATTERN =
            Pattern.compile("^(?<command>\\S+)(?:\\s+(?<details>.*))?$");
    private static final Pattern DEADLINE_ARGUMENTS_PATTERN =
            Pattern.compile("^(?<desc>.+?)\\s+/by\\s+(?<by>.+)$");
    private static final Pattern EVENT_ARGUMENTS_PATTERN =
            Pattern.compile("^(?<desc>.+?)\\s+/from\\s+(?<from>.+?)\\s+/to\\s+(?<to>.+)$");

    public static void main(String[] args) {
        String banner = """
                  ___    ____\s
                 / _ \\  |_  /
                | | | |   / /\s
                | |_| |  / /_\s
                 \\___/  /____|
                """;
        String greeting = DIVIDER
                + banner
                + "Hello! I'm Oz.\n"
                + "What can I do for you? ᕙ(  •̀ ᗜ •́  )ᕗ\n";

        String bye = DIVIDER
                + "  Bye. Hope to see you again soon! („• ֊ •„)੭\n"
                + DIVIDER;

        System.out.print(greeting);

        try (Scanner scanner = new Scanner(System.in)) {
            ArrayList<Task> list = loadTasks();

            while (scanner.hasNextLine()) {
                String desc = scanner.nextLine().trim();
                if (desc.equals("bye")) {
                    break;
                }

                String reply;
                try {
                    Matcher commandMatcher = COMMAND_PATTERN.matcher(desc);
                    if (!commandMatcher.matches()) {
                        throw new OzException("I could not understand that input.");
                    }

                    String command = commandMatcher.group("command");
                    String details = commandMatcher.group("details");
                    if (details == null) {
                        details = "";
                    }
                    details = details.trim();

                    if (command.equals("list")) {
                        if (!details.isBlank()) {
                            throw new OzException("The list command does not take arguments.");
                        }

                        StringBuilder response =
                                new StringBuilder(DIVIDER + "Here are the tasks in your list:\n");
                        for (int i = 0; i < list.size(); i++) {
                            response.append(i + 1)
                                    .append(". ")
                                    .append(list.get(i))
                                    .append("\n");
                        }
                        reply = response.append(DIVIDER).toString();

                    } else if (command.equals("mark")) {
                        int index = parseTaskIndex(details, list.size());
                        list.get(index).mark();
                        saveTasks(list);
                        reply = DIVIDER
                                + "Nice! I've marked this task as done:\n  "
                                + list.get(index) + "\n" + DIVIDER;

                    } else if (command.equals("unmark")) {
                        int index = parseTaskIndex(details, list.size());
                        list.get(index).unmark();
                        saveTasks(list);
                        reply = DIVIDER
                                + "OK! I've marked this task as not done yet:\n  "
                                + list.get(index) + "\n" + DIVIDER;

                    } else if (command.equals("todo")) {
                        if (details.isBlank()) {
                            throw new OzException(
                                    "The description of a todo cannot be empty.");
                        }

                        list.add(new ToDo(details));
                        saveTasks(list);
                        reply = DIVIDER
                                + String.format(
                                """
                                        Got it. I've added this task:
                                        %s
                                        Now you have %d tasks in the list.
                                        """,
                                        list.get(list.size() - 1), list.size())
                                + DIVIDER;

                    } else if (command.equals("deadline")) {
                        Matcher deadlineMatcher =
                                DEADLINE_ARGUMENTS_PATTERN.matcher(details);
                        if (!deadlineMatcher.matches()) {
                            throw new OzException(
                                    "Use: deadline <description> /by <date>.");
                        }

                        TaskDateTime deadlineTime =
                                TaskDateTime.parse(deadlineMatcher.group("by").trim());
                        list.add(new Deadlines(
                                deadlineMatcher.group("desc").trim(),
                                deadlineTime));
                        saveTasks(list);
                        reply = DIVIDER
                                + String.format(
                                """
                                        Got it. I've added this task:
                                        %s
                                        Now you have %d tasks in the list.
                                        """,
                                        list.get(list.size() - 1), list.size())
                                + DIVIDER;

                    } else if (command.equals("event")) {
                        Matcher eventMatcher =
                                EVENT_ARGUMENTS_PATTERN.matcher(details);
                        if (!eventMatcher.matches()) {
                            throw new OzException(
                                    "Use: event <description> /from <start> /to <end>.");
                        }

                        TaskDateTime fromTime =
                                TaskDateTime.parse(eventMatcher.group("from").trim());
                        TaskDateTime toTime =
                                TaskDateTime.parse(eventMatcher.group("to").trim());
                        list.add(new Event(
                                eventMatcher.group("desc").trim(),
                                fromTime,
                                toTime));
                        saveTasks(list);
                        reply = DIVIDER
                                + String.format(
                                """
                                        Got it. I've added this task:
                                        %s
                                        Now you have %d tasks in the list.
                                        """,
                                        list.get(list.size() - 1), list.size())
                                + DIVIDER;

                    } else if (command.equals("delete")) {
                        int index = parseTaskIndex(details, list.size());
                        Task removedTask = list.get(index);
                        list.remove(index);
                        saveTasks(list);
                        reply = DIVIDER
                                + String.format(
                                """
                                        Ok the following task has been removed!:
                                        %s
                                        Now you have %d tasks in the list.
                                        """,
                                        removedTask, list.size())
                                + DIVIDER;

                    } else {
                        throw new OzException(
                                "Sorry, I do not understand that command.");
                    }
                } catch (OzException exception) {
                    reply = DIVIDER + "OOPS! " + exception.getMessage()
                            + "\n" + DIVIDER;
                }

                System.out.print(reply);
            }
        }

        System.out.print(bye);
    }

    /**
     * Parses the zero-based task index from user command arguments.
     *
     * @param args Arguments string containing the 1-based task number.
     * @param taskCount Current total number of tasks in the list.
     * @return 0-based task index.
     * @throws OzException If the input is invalid or out of range.
     */
    private static int parseTaskIndex(String args, int taskCount)
            throws OzException {
        if (!args.matches("\\d+")) {
            throw new OzException("Please provide a valid task number.");
        }

        try {
            int taskNumber = Integer.parseInt(args);
            if (taskNumber < 1 || taskNumber > taskCount) {
                throw new OzException("That task number does not exist.");
            }
            return taskNumber - 1;
        } catch (NumberFormatException exception) {
            throw new OzException("That task number is too large.");
        }
    }

    /**
     * Loads tasks from the storage file on the hard disk.
     * If the file does not exist, an empty list is returned.
     * Corrupted lines are reported and skipped.
     *
     * @return List of tasks loaded from the storage file.
     */
    private static ArrayList<Task> loadTasks() {
        ArrayList<Task> tasks = new ArrayList<>();
        if (!Files.exists(FILE_PATH)) {
            return tasks;
        }

        try {
            List<String> lines = Files.readAllLines(FILE_PATH);
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
     * Parses a line from the storage file into a corresponding Task object.
     *
     * @param line Raw line text from storage.
     * @return A Task instance with status and descriptions populated.
     * @throws OzException If the line format is invalid or has missing/corrupted fields.
     */
    private static Task parseTaskFromFile(String line) throws OzException {
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
            String todoDesc = initialParts[2].trim();
            if (todoDesc.isEmpty()) {
                throw new OzException("Todo description cannot be empty.");
            }
            task = new ToDo(todoDesc);
            break;

        case "D":
            String[] deadlineParts = line.split("\\s*\\|\\s*", 4);
            if (deadlineParts.length < 4) {
                throw new OzException("Deadline task requires description and deadline date.");
            }
            String deadlineDesc = deadlineParts[2].trim();
            String by = deadlineParts[3].trim();
            if (deadlineDesc.isEmpty() || by.isEmpty()) {
                throw new OzException("Deadline description and date cannot be empty.");
            }
            TaskDateTime deadlineTime = TaskDateTime.parse(by);
            task = new Deadlines(deadlineDesc, deadlineTime);
            break;

        case "E":
            String[] eventParts = line.split("\\s*\\|\\s*", 5);
            if (eventParts.length < 5) {
                throw new OzException("Event task requires description, start time, and end time.");
            }
            String eventDesc = eventParts[2].trim();
            String from = eventParts[3].trim();
            String to = eventParts[4].trim();
            if (eventDesc.isEmpty() || from.isEmpty() || to.isEmpty()) {
                throw new OzException("Event description, start time, and end time cannot be empty.");
            }
            TaskDateTime fromTime = TaskDateTime.parse(from);
            TaskDateTime toTime = TaskDateTime.parse(to);
            task = new Event(eventDesc, fromTime, toTime);
            break;

        default:
            throw new OzException("Unknown task type: " + type);
        }

        if (isDone) {
            task.mark();
        }
        return task;
    }


    /**
     * Saves the current list of tasks to the storage file on the hard disk.
     *
     * @param tasks The list of tasks to save.
     */
    private static void saveTasks(ArrayList<Task> tasks) {
        try {
            if (FILE_PATH.getParent() != null) {
                Files.createDirectories(FILE_PATH.getParent());
            }
            List<String> lines = new ArrayList<>();
            for (Task task : tasks) {
                lines.add(task.toFileFormat());
            }
            Files.write(FILE_PATH, lines);
        } catch (IOException exception) {
            System.out.println(DIVIDER + "OOPS! Could not save tasks to file: "
                    + exception.getMessage() + "\n" + DIVIDER);
        }
    }
}
