package oz;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.util.Pair;
import oz.exception.OzException;
import oz.storage.Storage;
import oz.task.Deadline;
import oz.task.Event;
import oz.task.Task;
import oz.task.TaskDateTime;
import oz.task.TaskList;
import oz.task.ToDo;

/**
 * Main entry point and controller for the Oz chatbot.
 */
public class Oz {
    /** Visual divider line for console output. */
    private static final String DIVIDER = "____________________________________________________________\n";

    /** Regex pattern matching user command and optional arguments. */
    private static final Pattern COMMAND_PATTERN = Pattern
            .compile("^(?<command>\\S+)(?:\\s+(?<details>.*))?$");

    /** Regex pattern parsing deadline description and /by argument. */
    private static final Pattern DEADLINE_ARGUMENTS_PATTERN = Pattern
            .compile("^(?<description>.+?)\\s+/by\\s+(?<byTime>.+)$");

    /** Regex pattern parsing event description, /from, and /to arguments. */
    private static final Pattern EVENT_ARGUMENTS_PATTERN = Pattern
            .compile("^(?<description>.+?)\\s+/from\\s+(?<fromTime>.+?)\\s+/to\\s+(?<toTime>.+)$");

    /** Storage manager for reading and writing tasks to disk. */
    private final Storage storage;

    /** In-memory task list. */
    private final TaskList tasks;

    /** Flag indicating whether an exit command was issued. */
    private boolean isExit = false;

    /**
     * Constructs an Oz chatbot instance configured with the specified storage file
     * path.
     *
     * @param filePath Path to the task storage file.
     */
    public Oz(String filePath) {
        this.storage = new Storage(filePath);
        this.tasks = new TaskList(this.storage.load());
    }

    /**
     * Returns true if the chatbot has received an exit command.
     *
     * @return True if the exit command has been received; false otherwise.
     */
    public boolean isExit() {
        return this.isExit;
    }

    /**
     * Runs the main command loop of the chatbot.
     */
    public void run() {
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
            while (scanner.hasNextLine()) {
                String fullCommand = scanner.nextLine().trim();
                if (fullCommand.equals("bye")) {
                    this.isExit = true;
                    break;
                }

                Pair<String, CommandType> reply = getResponse(fullCommand);
                System.out.print(DIVIDER + reply.getKey() + "\n" + DIVIDER);
            }
        }

        System.out.print(bye);
    }

    /**
     * Processes a user command and returns the response message.
     *
     * @param fullCommand Full command string entered by the user.
     * @return A pair containing the response message and the command type tag.
     */
    public Pair<String, CommandType> getResponse(String fullCommand) {
        if (fullCommand == null || fullCommand.isBlank()) {
            return new Pair<>("Please enter a command.", CommandType.BLANK);
        }

        try {
            Matcher commandMatcher = COMMAND_PATTERN.matcher(fullCommand.trim());
            if (!commandMatcher.matches()) {
                throw new OzException("I could not understand that input.");
            }

            String command = commandMatcher.group("command");
            String rawDetails = commandMatcher.group("details");
            String details = rawDetails == null ? "" : rawDetails.trim();
            return executeCommand(command, details);
        } catch (OzException exception) {
            return new Pair<>("OOPS! " + exception.getMessage(), CommandType.ERROR);
        }
    }

    /**
     * Routes a recognized command word to its handler.
     *
     * @param command Command word entered by the user.
     * @param details Arguments supplied after the command word.
     * @return Response message and command type.
     * @throws OzException If the command is unknown or its arguments are invalid.
     */
    private Pair<String, CommandType> executeCommand(String command, String details) throws OzException {
        switch (command) {
            case "bye":
                return exit();
            case "list":
                return listTasks(details);
            case "on":
                return listTasksOn(details);
            case "find":
                return findTasks(details);
            case "mark":
                return markTask(details);
            case "unmark":
                return unmarkTask(details);
            case "todo":
                return addTodo(details);
            case "deadline":
                return addDeadline(details);
            case "event":
                return addEvent(details);
            case "delete":
                return deleteTask(details);
            default:
                throw new OzException("Sorry, I do not understand that command.");
        }
    }

    /**
     * Records the exit request and returns the farewell message.
     *
     * @return Response message and command type.
     */
    private Pair<String, CommandType> exit() {
        this.isExit = true;
        return new Pair<>("Bye. Hope to see you again soon! („• ֊ •„)੭", CommandType.BYE);
    }

    /**
     * Lists all tasks after validating that no arguments were supplied.
     *
     * @param details Arguments supplied after the command word.
     * @return Response message and command type.
     * @throws OzException If the command arguments are invalid.
     */
    private Pair<String, CommandType> listTasks(String details) throws OzException {
        if (!details.isBlank()) {
            throw new OzException("The list command does not take arguments.");
        }

        String response = formatTaskList("Here are the tasks in your list:\n",
                this.tasks.getTasks());
        return new Pair<>(response, CommandType.LIST);
    }

    /**
     * Lists tasks occurring on the date supplied by the user.
     *
     * @param details Arguments supplied after the command word.
     * @return Response message and command type.
     * @throws OzException If the command arguments are invalid.
     */
    private Pair<String, CommandType> listTasksOn(String details) throws OzException {
        if (details.isBlank()) {
            throw new OzException("Use: on <date> (e.g., on 2019-10-15 or on 2/12/2019).");
        }

        TaskDateTime targetDateTime = TaskDateTime.parse(details);
        LocalDate targetDate = targetDateTime.toLocalDate();
        String dateHeader = targetDate.format(
                DateTimeFormatter.ofPattern("MMM dd yyyy", Locale.ENGLISH));

        ArrayList<Task> matchingTasks = this.tasks.findTasksOn(targetDate);

        if (matchingTasks.isEmpty()) {
            return new Pair<>("There are no tasks occurring on " + dateHeader + ".",
                    CommandType.LIST);
        }

        String response = formatTaskList("Here are the tasks occurring on " + dateHeader + ":\n",
                matchingTasks);
        return new Pair<>(response, CommandType.LIST);
    }

    /**
     * Finds tasks containing the supplied keyword.
     *
     * @param details Arguments supplied after the command word.
     * @return Response message and command type.
     * @throws OzException If the command arguments are invalid.
     */
    private Pair<String, CommandType> findTasks(String details) throws OzException {
        if (details.isBlank()) {
            throw new OzException("The keyword for find cannot be empty.");
        }

        ArrayList<Task> matchingTasks = this.tasks.findTasksByKeyword(details);

        if (matchingTasks.isEmpty()) {
            return new Pair<>("There are no matching tasks in your list.", CommandType.FIND);
        }

        String response = formatTaskList("Here are the matching tasks in your list:\n",
                matchingTasks);
        return new Pair<>(response, CommandType.FIND);
    }

    /**
     * Marks the selected task as done and saves the updated list.
     *
     * @param details Arguments supplied after the command word.
     * @return Response message and command type.
     * @throws OzException If the command arguments are invalid.
     */
    private Pair<String, CommandType> markTask(String details) throws OzException {
        int index = parseTaskIndex(details, this.tasks.size());
        this.tasks.markAsDone(index);
        this.storage.save(this.tasks);
        return new Pair<>("Nice! I've marked this task as done:\n  " + this.tasks.get(index),
                CommandType.CHANGE_MARK);
    }

    /**
     * Marks the selected task as not done and saves the updated list.
     *
     * @param details Arguments supplied after the command word.
     * @return Response message and command type.
     * @throws OzException If the command arguments are invalid.
     */
    private Pair<String, CommandType> unmarkTask(String details) throws OzException {
        int index = parseTaskIndex(details, this.tasks.size());
        this.tasks.markAsNotDone(index);
        this.storage.save(this.tasks);
        return new Pair<>("OK! I've marked this task as not done yet:\n  " + this.tasks.get(index),
                CommandType.CHANGE_MARK);
    }

    /**
     * Validates and adds a todo task.
     *
     * @param details Arguments supplied after the command word.
     * @return Response message and command type.
     * @throws OzException If the command arguments are invalid.
     */
    private Pair<String, CommandType> addTodo(String details) throws OzException {
        if (details.isBlank()) {
            throw new OzException("The description of a todo cannot be empty.");
        }

        Task task = new ToDo(details);
        return addTask(task);
    }

    /**
     * Parses and adds a deadline task.
     *
     * @param details Arguments supplied after the command word.
     * @return Response message and command type.
     * @throws OzException If the command arguments are invalid.
     */
    private Pair<String, CommandType> addDeadline(String details) throws OzException {
        Matcher deadlineMatcher = DEADLINE_ARGUMENTS_PATTERN.matcher(details);
        if (!deadlineMatcher.matches()) {
            throw new OzException("Use: deadline <description> /by <date>.");
        }

        String deadlineDescription = deadlineMatcher.group("description").trim();
        String deadlineTimeArgument = deadlineMatcher.group("byTime").trim();
        if (deadlineDescription.isEmpty()) {
            throw new OzException("The description of a deadline cannot be empty.");
        }
        if (deadlineTimeArgument.isEmpty()) {
            throw new OzException("The deadline date/time (/by) cannot be empty.");
        }

        TaskDateTime deadlineTime = TaskDateTime.parse(deadlineTimeArgument);
        Task task = new Deadline(deadlineDescription, deadlineTime);
        return addTask(task);
    }

    /**
     * Parses and adds an event task.
     *
     * @param details Arguments supplied after the command word.
     * @return Response message and command type.
     * @throws OzException If the command arguments are invalid.
     */
    private Pair<String, CommandType> addEvent(String details) throws OzException {
        Matcher eventMatcher = EVENT_ARGUMENTS_PATTERN.matcher(details);
        if (!eventMatcher.matches()) {
            throw new OzException("Use: event <description> /from <start> /to <end>.");
        }

        String eventDescription = eventMatcher.group("description").trim();
        String fromTimeArgument = eventMatcher.group("fromTime").trim();
        String toTimeArgument = eventMatcher.group("toTime").trim();
        if (eventDescription.isEmpty()) {
            throw new OzException("The description of an event cannot be empty.");
        }
        if (fromTimeArgument.isEmpty() || toTimeArgument.isEmpty()) {
            throw new OzException("The event start (/from) and end (/to) dates cannot be empty.");
        }

        TaskDateTime fromTime = TaskDateTime.parse(fromTimeArgument);
        TaskDateTime toTime = TaskDateTime.parse(toTimeArgument);
        Task task = new Event(eventDescription, fromTime, toTime);
        return addTask(task);
    }

    /**
     * Deletes the selected task and saves the updated list.
     *
     * @param details Arguments supplied after the command word.
     * @return Response message and command type.
     * @throws OzException If the command arguments are invalid.
     */
    private Pair<String, CommandType> deleteTask(String details) throws OzException {
        int index = parseTaskIndex(details, this.tasks.size());
        Task removedTask = this.tasks.delete(index);
        this.storage.save(this.tasks);
        return new Pair<>(String.format(
                """
                        Ok the following task has been removed!:
                        %s
                        Now you have %d tasks in the list.
                        """,
                removedTask, this.tasks.size()).stripTrailing(), CommandType.DELETE);
    }

    /**
     * Adds a validated task, saves the list, and reports the new task count.
     *
     * @param task Task to add.
     * @return Confirmation message and the add command type.
     */
    private Pair<String, CommandType> addTask(Task task) {
        this.tasks.add(task);
        this.storage.save(this.tasks);
        return new Pair<>(String.format(
                """
                        Got it. I've added this task:
                        %s
                        Now you have %d tasks in the list.
                        """,
                task, this.tasks.size()).stripTrailing(), CommandType.ADD);
    }

    /**
     * Formats tasks in their existing order with consecutive display numbers.
     *
     * @param header Heading to place before the numbered tasks, including its newline.
     * @param tasksToDisplay Tasks to include in the response.
     * @return Heading and numbered task descriptions without trailing whitespace.
     */
    private static String formatTaskList(String header, List<Task> tasksToDisplay) {
        StringBuilder response = new StringBuilder(header);
        for (int i = 0; i < tasksToDisplay.size(); i++) {
            response.append(i + 1)
                    .append(". ")
                    .append(tasksToDisplay.get(i))
                    .append("\n");
        }
        return response.toString().stripTrailing();
    }

    /**
     * Parses the zero-based task index from user command arguments.
     *
     * @param argument  Argument string containing the 1-based task number.
     * @param taskCount Current total number of tasks in the list.
     * @return 0-based task index.
     * @throws OzException If the input is invalid or out of range.
     */
    private static int parseTaskIndex(String argument, int taskCount)
            throws OzException {
        if (!argument.matches("\\d+")) {
            throw new OzException("Please provide a valid task number.");
        }

        try {
            int taskNumber = Integer.parseInt(argument);
            if (taskNumber < 1 || taskNumber > taskCount) {
                throw new OzException("That task number does not exist.");
            }
            return taskNumber - 1;
        } catch (NumberFormatException exception) {
            throw new OzException("That task number is too large.");
        }
    }

    /**
     * Entry point for running the Oz application.
     *
     * @param args Command-line arguments.
     */
    public static void main(String[] args) {
        new Oz("data/oz.txt").run();
    }
}
