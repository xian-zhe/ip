package oz;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.util.Pair;
import oz.exception.OzException;
import oz.storage.Storage;
import oz.task.Deadline;
import oz.task.Event;
import oz.task.RecurringEvent;
import oz.task.Task;
import oz.task.TaskDateTime;
import oz.task.TaskList;
import oz.task.ToDo;

/**
 * Main entry point and controller for the Oz chatbot.
 */
public class Oz {
    /** Default task file shared by the console and graphical entry points. */
    public static final String DEFAULT_STORAGE_PATH = "data/oz.txt";

    /** Visual divider line for console output. */
    private static final String DIVIDER = "____________________________________________________________\n";

    /** Error shown when the general command pattern cannot parse an input. */
    private static final String UNRECOGNIZED_INPUT_MESSAGE = "I could not understand that input.";

    /** Error shown when a command word is unknown. */
    private static final String UNKNOWN_COMMAND_MESSAGE =
            "Sorry, I do not understand that command.";

    /** Error shown when list receives arguments. */
    private static final String LIST_ARGUMENTS_MESSAGE =
            "The list command does not take arguments.";

    /** Usage message for date-filtered task listing. */
    private static final String ON_USAGE_MESSAGE =
            "Use: on <date> (e.g., on 2019-10-15 or on 2/12/2019).";

    /** Error shown when find receives no keyword. */
    private static final String EMPTY_FIND_KEYWORD_MESSAGE =
            "The keyword for find cannot be empty.";

    /** Error shown when an occurrence date is supplied for an ordinary task. */
    private static final String ON_RECURRING_ONLY_MESSAGE =
            "The /on argument can only be used with recurring tasks.";

    /** Usage message for marking a recurring occurrence. */
    private static final String MARK_RECURRING_USAGE_MESSAGE =
            "Please specify which occurrence to mark. Use: mark <number> /on <date>.";

    /** Usage message for unmarking a recurring occurrence. */
    private static final String UNMARK_RECURRING_USAGE_MESSAGE =
            "Please specify which occurrence to unmark. Use: unmark <number> /on <date>.";

    /** Error shown when a todo description is missing. */
    private static final String EMPTY_TODO_DESCRIPTION_MESSAGE =
            "The description of a todo cannot be empty.";

    /** Usage message for adding a deadline. */
    private static final String DEADLINE_USAGE_MESSAGE =
            "Use: deadline <description> /by <date>.";

    /** Error shown when a deadline description is missing. */
    private static final String EMPTY_DEADLINE_DESCRIPTION_MESSAGE =
            "The description of a deadline cannot be empty.";

    /** Error shown when a deadline date-time is missing. */
    private static final String EMPTY_DEADLINE_DATE_TIME_MESSAGE =
            "The deadline date/time (/by) cannot be empty.";

    /** Usage message for adding an event. */
    private static final String EVENT_USAGE_MESSAGE =
            "Use: event <description> /from <start> /to <end>.";

    /** Error shown when an event description is missing. */
    private static final String EMPTY_EVENT_DESCRIPTION_MESSAGE =
            "The description of an event cannot be empty.";

    /** Error shown when an event start or end is missing. */
    private static final String EMPTY_EVENT_DATE_TIME_MESSAGE =
            "The event start (/from) and end (/to) dates cannot be empty.";

    /** Usage message for adding a recurring event. */
    private static final String RECURRING_EVENT_USAGE_MESSAGE =
            "Use: recurring <description> /on <date> /start <time> /end <time> "
                    + "/every <interval> week|weeks [/until <date>].";

    /** Error shown when a recurring event description is missing. */
    private static final String EMPTY_RECURRING_DESCRIPTION_MESSAGE =
            "The description of a recurring event cannot be empty.";

    /** Error shown when a recurrence unit is not weekly. */
    private static final String INVALID_RECURRENCE_UNIT_MESSAGE =
            "The recurrence unit must be week or weeks.";

    /** Error shown when a recurrence interval is not positive. */
    private static final String INVALID_RECURRENCE_INTERVAL_MESSAGE =
            "The recurrence interval must be a positive whole number.";

    /** Error shown when a task number is not numeric. */
    private static final String INVALID_TASK_NUMBER_MESSAGE =
            "Please provide a valid task number.";

    /** Error shown when a task number is outside the task list. */
    private static final String TASK_NOT_FOUND_MESSAGE = "That task number does not exist.";

    /** Error shown when a numeric task number cannot fit in an integer. */
    private static final String TASK_NUMBER_TOO_LARGE_MESSAGE = "That task number is too large.";

    /** Regex pattern matching user command and optional arguments. */
    private static final Pattern COMMAND_PATTERN = Pattern
            .compile("^(?<command>\\S+)(?:\\s+(?<details>.*))?$");

    /** Regex pattern parsing deadline description and /by argument. */
    private static final Pattern DEADLINE_ARGUMENTS_PATTERN = Pattern
            .compile("^(?<description>.+?)\\s+/by\\s+(?<byTime>.+)$");

    /** Regex pattern parsing event description, /from, and /to arguments. */
    private static final Pattern EVENT_ARGUMENTS_PATTERN = Pattern
            .compile("^(?<description>.+?)\\s+/from\\s+(?<fromTime>.+?)\\s+/to\\s+(?<toTime>.+)$");

    /** Regex pattern parsing a weekly recurring event and optional end date. */
    private static final Pattern RECURRING_EVENT_ARGUMENTS_PATTERN = Pattern.compile(
            "^(?<description>.+?)\\s+/on\\s+(?<eventDate>.+?)"
                    + "\\s+/start\\s+(?<startTime>.+?)\\s+/end\\s+(?<endTime>.+?)"
                    + "\\s+/every\\s+(?<interval>\\S+)\\s+(?<unit>\\S+)"
                    + "(?:\\s+/until\\s+(?<untilDate>.+))?$");

    /** Regex pattern parsing a task number and occurrence date. */
    private static final Pattern OCCURRENCE_ARGUMENTS_PATTERN = Pattern
            .compile("^(?<taskNumber>\\d+)\\s+/on\\s+(?<occurrenceDate>.+)$");

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
                throw new OzException(UNRECOGNIZED_INPUT_MESSAGE);
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
            case "recurring":
                return addRecurringEvent(details);
            case "delete":
                return deleteTask(details);
            default:
                throw new OzException(UNKNOWN_COMMAND_MESSAGE);
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
            throw new OzException(LIST_ARGUMENTS_MESSAGE);
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
            throw new OzException(ON_USAGE_MESSAGE);
        }

        TaskDateTime targetDateTime = TaskDateTime.parse(details);
        LocalDate targetDate = targetDateTime.toLocalDate();
        String dateHeader = targetDate.format(TaskDateTime.DISPLAY_DATE_FORMAT);

        ArrayList<Task> matchingTasks = this.tasks.findTasksOn(targetDate);

        if (matchingTasks.isEmpty()) {
            return new Pair<>("There are no tasks occurring on " + dateHeader + ".",
                    CommandType.LIST);
        }

        String response = formatTasksOnDate(
                "Here are the tasks occurring on " + dateHeader + ":\n",
                matchingTasks, targetDate);
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
            throw new OzException(EMPTY_FIND_KEYWORD_MESSAGE);
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
        Matcher occurrenceMatcher = OCCURRENCE_ARGUMENTS_PATTERN.matcher(details);
        if (occurrenceMatcher.matches()) {
            int index = parseTaskIndex(occurrenceMatcher.group("taskNumber"), this.tasks.size());
            Task task = this.tasks.get(index);
            if (!(task instanceof RecurringEvent recurringEvent)) {
                throw new OzException(ON_RECURRING_ONLY_MESSAGE);
            }

            LocalDate occurrenceDate = TaskDateTime.parseDate(
                    occurrenceMatcher.group("occurrenceDate").trim());
            recurringEvent.markOccurrence(occurrenceDate);
            this.storage.save(this.tasks);
            return new Pair<>("Nice! I've marked this occurrence as done:\n  "
                    + recurringEvent.toOccurrenceString(occurrenceDate), CommandType.CHANGE_MARK);
        }

        int index = parseTaskIndex(details, this.tasks.size());
        Task task = this.tasks.get(index);
        if (task instanceof RecurringEvent) {
            throw new OzException(MARK_RECURRING_USAGE_MESSAGE);
        }

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
        Matcher occurrenceMatcher = OCCURRENCE_ARGUMENTS_PATTERN.matcher(details);
        if (occurrenceMatcher.matches()) {
            int index = parseTaskIndex(occurrenceMatcher.group("taskNumber"), this.tasks.size());
            Task task = this.tasks.get(index);
            if (!(task instanceof RecurringEvent recurringEvent)) {
                throw new OzException(ON_RECURRING_ONLY_MESSAGE);
            }

            LocalDate occurrenceDate = TaskDateTime.parseDate(
                    occurrenceMatcher.group("occurrenceDate").trim());
            recurringEvent.unmarkOccurrence(occurrenceDate);
            this.storage.save(this.tasks);
            return new Pair<>("OK! I've marked this occurrence as not done yet:\n  "
                    + recurringEvent.toOccurrenceString(occurrenceDate), CommandType.CHANGE_MARK);
        }

        int index = parseTaskIndex(details, this.tasks.size());
        Task task = this.tasks.get(index);
        if (task instanceof RecurringEvent) {
            throw new OzException(UNMARK_RECURRING_USAGE_MESSAGE);
        }

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
            throw new OzException(EMPTY_TODO_DESCRIPTION_MESSAGE);
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
            throw new OzException(DEADLINE_USAGE_MESSAGE);
        }

        String deadlineDescription = deadlineMatcher.group("description").trim();
        String deadlineTimeArgument = deadlineMatcher.group("byTime").trim();
        if (deadlineDescription.isEmpty()) {
            throw new OzException(EMPTY_DEADLINE_DESCRIPTION_MESSAGE);
        }
        if (deadlineTimeArgument.isEmpty()) {
            throw new OzException(EMPTY_DEADLINE_DATE_TIME_MESSAGE);
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
            throw new OzException(EVENT_USAGE_MESSAGE);
        }

        String eventDescription = eventMatcher.group("description").trim();
        String fromTimeArgument = eventMatcher.group("fromTime").trim();
        String toTimeArgument = eventMatcher.group("toTime").trim();
        if (eventDescription.isEmpty()) {
            throw new OzException(EMPTY_EVENT_DESCRIPTION_MESSAGE);
        }
        if (fromTimeArgument.isEmpty() || toTimeArgument.isEmpty()) {
            throw new OzException(EMPTY_EVENT_DATE_TIME_MESSAGE);
        }

        TaskDateTime fromTime = TaskDateTime.parse(fromTimeArgument);
        TaskDateTime toTime = TaskDateTime.parse(toTimeArgument);
        Task task = new Event(eventDescription, fromTime, toTime);
        return addTask(task);
    }

    /**
     * Parses and adds a same-day event that repeats at a weekly interval.
     *
     * @param details Arguments supplied after the command word.
     * @return Response message and command type.
     * @throws OzException If the recurring event arguments are invalid.
     */
    private Pair<String, CommandType> addRecurringEvent(String details) throws OzException {
        Matcher recurringEventMatcher = RECURRING_EVENT_ARGUMENTS_PATTERN.matcher(details);
        if (!recurringEventMatcher.matches()) {
            throw new OzException(RECURRING_EVENT_USAGE_MESSAGE);
        }

        String description = recurringEventMatcher.group("description").trim();
        String eventDateArgument = recurringEventMatcher.group("eventDate").trim();
        String startTimeArgument = recurringEventMatcher.group("startTime").trim();
        String endTimeArgument = recurringEventMatcher.group("endTime").trim();
        String intervalArgument = recurringEventMatcher.group("interval").trim();
        String unitArgument = recurringEventMatcher.group("unit").trim();
        String untilDateArgument = recurringEventMatcher.group("untilDate");

        if (description.isEmpty()) {
            throw new OzException(EMPTY_RECURRING_DESCRIPTION_MESSAGE);
        }
        if (!unitArgument.equalsIgnoreCase("week")
                && !unitArgument.equalsIgnoreCase("weeks")) {
            throw new OzException(INVALID_RECURRENCE_UNIT_MESSAGE);
        }

        int weekInterval = parseRecurrenceInterval(intervalArgument);
        LocalDate eventDate = TaskDateTime.parseDate(eventDateArgument);
        LocalTime startTime = TaskDateTime.parseTime(startTimeArgument);
        LocalTime endTime = TaskDateTime.parseTime(endTimeArgument);
        TaskDateTime firstStartDateTime = TaskDateTime.fromDateTime(
                LocalDateTime.of(eventDate, startTime));
        TaskDateTime firstEndDateTime = TaskDateTime.fromDateTime(
                LocalDateTime.of(eventDate, endTime));
        LocalDate untilDate = untilDateArgument == null
                ? null
                : TaskDateTime.parseDate(untilDateArgument.trim());
        Task task = new RecurringEvent(description, firstStartDateTime,
                firstEndDateTime, weekInterval, untilDate);
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
     * Formats tasks for a date query, expanding recurring series to their occurrence view.
     *
     * @param header Heading to place before the numbered tasks.
     * @param tasksToDisplay Tasks occurring on the target date.
     * @param targetDate Date whose occurrences should be displayed.
     * @return Heading and numbered task descriptions without trailing whitespace.
     * @throws OzException If a recurring task cannot produce its expected occurrence.
     */
    private static String formatTasksOnDate(String header, List<Task> tasksToDisplay,
            LocalDate targetDate) throws OzException {
        StringBuilder response = new StringBuilder(header);
        for (int i = 0; i < tasksToDisplay.size(); i++) {
            Task task = tasksToDisplay.get(i);
            String taskDescription = task instanceof RecurringEvent recurringEvent
                    ? recurringEvent.toOccurrenceString(targetDate)
                    : task.toString();
            response.append(i + 1)
                    .append(". ")
                    .append(taskDescription)
                    .append("\n");
        }
        return response.toString().stripTrailing();
    }

    /**
     * Parses and validates a positive recurrence interval.
     *
     * @param argument Raw interval argument.
     * @return Positive interval in weeks.
     * @throws OzException If the argument is not a positive whole number.
     */
    private static int parseRecurrenceInterval(String argument) throws OzException {
        if (!argument.matches("\\d+")) {
            throw new OzException(INVALID_RECURRENCE_INTERVAL_MESSAGE);
        }

        try {
            int interval = Integer.parseInt(argument);
            if (interval <= 0) {
                throw new OzException(INVALID_RECURRENCE_INTERVAL_MESSAGE);
            }
            return interval;
        } catch (NumberFormatException exception) {
            throw new OzException(INVALID_RECURRENCE_INTERVAL_MESSAGE);
        }
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
        assert taskCount >= 0 : "The task count must not be negative";
        if (!argument.matches("\\d+")) {
            throw new OzException(INVALID_TASK_NUMBER_MESSAGE);
        }

        try {
            int taskNumber = Integer.parseInt(argument);
            if (taskNumber < 1 || taskNumber > taskCount) {
                throw new OzException(TASK_NOT_FOUND_MESSAGE);
            }
            int index = taskNumber - 1;
            assert index >= 0 && index < taskCount
                    : "A validated task number must map to an existing index";
            return index;
        } catch (NumberFormatException exception) {
            throw new OzException(TASK_NUMBER_TOO_LARGE_MESSAGE);
        }
    }

    /**
     * Entry point for running the Oz application.
     *
     * @param args Command-line arguments.
     */
    public static void main(String[] args) {
        new Oz(DEFAULT_STORAGE_PATH).run();
    }
}
