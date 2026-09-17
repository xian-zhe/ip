package oz;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oz.exception.OzException;
import oz.parser.CommandParser;
import oz.parser.ParsedCommand;
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

    /** Error shown when a command word is unknown. */
    private static final String UNKNOWN_COMMAND_MESSAGE = "Unknown command. Check your blueprint syntax.";

    /** Error shown when list receives arguments. */
    private static final String LIST_ARGUMENTS_MESSAGE = "The list command does not take arguments.";

    /** Error shown when the bye command receives arguments. */
    private static final String BYE_ARGUMENTS_MESSAGE = "The bye command does not take arguments.";

    /** Error template shown when duplicate parameter flags are detected. */
    private static final String DUPLICATE_FLAG_MESSAGE_FORMAT = "Duplicate '%s' parameter detected.";

    /** Error shown when an unexpected parameter flag is supplied to todo. */
    private static final String TODO_UNEXPECTED_FLAG_MESSAGE =
            "The todo command does not accept parameter flags like /by, /from, or /to.";

    /** Error template shown when a required parameter flag is missing. */
    private static final String MISSING_FLAG_MESSAGE_FORMAT = "Missing required '%s' parameter. %s";

    /** Error template shown when parameters are supplied out of order. */
    private static final String MISPLACED_FLAG_MESSAGE_FORMAT = "The '%s' parameter must precede '%s'. %s";

    /** Error template shown when an unexpected parameter flag is supplied. */
    private static final String UNEXPECTED_FLAG_MESSAGE_FORMAT = "Unexpected '%s' parameter in %s command. %s";

    /** Usage message for date-filtered task listing. */
    private static final String ON_USAGE_MESSAGE = "Use: on <date> (e.g., on 2019-10-15 or on 2/12/2019).";

    /** Error shown when find receives no keyword. */
    private static final String EMPTY_FIND_KEYWORD_MESSAGE = "The keyword for find cannot be empty.";

    /** Error shown when an occurrence date is supplied for an ordinary task. */
    private static final String ON_RECURRING_ONLY_MESSAGE = "The /on argument can only be used with recurring tasks.";

    /** Usage message for marking a recurring occurrence. */
    private static final String MARK_RECURRING_USAGE_MESSAGE =
            "Please specify which occurrence to mark. Use: mark <number> /on <date>.";

    /** Error template shown when marking an already completed task. */
    private static final String TASK_ALREADY_DONE_MESSAGE_FORMAT = "Task %d is already marked as done.";

    /** Error template shown when unmarking an incomplete task. */
    private static final String TASK_ALREADY_NOT_DONE_MESSAGE_FORMAT = "Task %d is not marked as done yet.";

    /** Error shown when task input contains the storage delimiter '|'. */
    private static final String RESERVED_DELIMITER_MESSAGE =
            "Task input cannot contain the '|' character because it is reserved for storage.";

    /** Usage message for unmarking a recurring occurrence. */
    private static final String UNMARK_RECURRING_USAGE_MESSAGE =
            "Please specify which occurrence to unmark. Use: unmark <number> /on <date>.";

    /** Error shown when a todo description is missing. */
    private static final String EMPTY_TODO_DESCRIPTION_MESSAGE = "The description of a todo cannot be empty.";

    /** Usage message for adding a deadline. */
    private static final String DEADLINE_USAGE_MESSAGE = "Use: deadline <description> /by <date>.";

    /** Error shown when a deadline description is missing. */
    private static final String EMPTY_DEADLINE_DESCRIPTION_MESSAGE = "The description of a deadline cannot be empty.";

    /** Error shown when a deadline date-time is missing. */
    private static final String EMPTY_DEADLINE_DATE_TIME_MESSAGE = "The deadline date/time (/by) cannot be empty.";

    /** Usage message for adding an event. */
    private static final String EVENT_USAGE_MESSAGE = "Use: event <description> /from <start> /to <end>.";

    /** Error shown when an event description is missing. */
    private static final String EMPTY_EVENT_DESCRIPTION_MESSAGE = "The description of an event cannot be empty.";

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
    private static final String INVALID_RECURRENCE_UNIT_MESSAGE = "The recurrence unit must be week or weeks.";

    /** Error shown when a recurrence interval is not positive. */
    private static final String INVALID_RECURRENCE_INTERVAL_MESSAGE =
            "The recurrence interval must be a positive whole number.";

    /** Error shown when no task number is provided. */
    private static final String EMPTY_TASK_NUMBER_MESSAGE = "Please specify a task number.";

    /** Error shown when the task list is empty. */
    private static final String EMPTY_TASK_LIST_MESSAGE = "Your task list is empty. Add tasks before referencing them.";

    /** Error shown when a task number is not positive. */
    private static final String NON_POSITIVE_TASK_NUMBER_MESSAGE =
            "Task number must be a positive whole number starting from 1.";

    /** Error template shown when a task number is out of bounds. */
    private static final String TASK_INDEX_OUT_OF_BOUNDS_MESSAGE_FORMAT =
            "Task number %d does not exist. Please provide a number between 1 and %d.";

    /** Error shown when a task number is not numeric. */
    private static final String INVALID_TASK_NUMBER_MESSAGE = "Please provide a valid whole number for the task index.";

    /** Error shown when a numeric task number cannot fit in an integer. */
    private static final String TASK_NUMBER_TOO_LARGE_MESSAGE = "That task number is too large.";

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
     * Processes a user command and returns the response message.
     *
     * @param fullCommand Full command string entered by the user.
     * @return Result containing the response message and its presentation type.
     */
    public CommandResult getResponse(String fullCommand) {
        if (fullCommand == null || fullCommand.isBlank()) {
            return new CommandResult("Please enter a command.", ResponseType.BLANK);
        }

        try {
            ParsedCommand command = CommandParser.parse(fullCommand);
            return executeCommand(command.commandWord(), command.arguments());
        } catch (OzException exception) {
            return new CommandResult("Confound it! " + exception.getMessage(), ResponseType.ERROR);
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
    private CommandResult executeCommand(String command, String details) throws OzException {
        switch (command) {
            case "bye":
                return exit(details);
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
     * @param details Arguments supplied after the command word.
     * @return Response message and command type.
     * @throws OzException If arguments are supplied to the bye command.
     */
    private CommandResult exit(String details) throws OzException {
        if (!details.isBlank()) {
            throw new OzException(BYE_ARGUMENTS_MESSAGE);
        }
        this.isExit = true;
        return new CommandResult("Farewell! Back to my contraptions. *oink*", ResponseType.BYE);
    }

    /**
     * Lists all tasks after validating that no arguments were supplied.
     *
     * @param details Arguments supplied after the command word.
     * @return Response message and command type.
     * @throws OzException If the command arguments are invalid.
     */
    private CommandResult listTasks(String details) throws OzException {
        if (!details.isBlank()) {
            throw new OzException(LIST_ARGUMENTS_MESSAGE);
        }

        String response = formatTaskList("Here is the master task list:\n",
                this.tasks.getTasks());
        return new CommandResult(response, ResponseType.LIST);
    }

    /**
     * Lists tasks occurring on the date supplied by the user.
     *
     * @param details Arguments supplied after the command word.
     * @return Response message and command type.
     * @throws OzException If the command arguments are invalid.
     */
    private CommandResult listTasksOn(String details) throws OzException {
        if (details.isBlank()) {
            throw new OzException(ON_USAGE_MESSAGE);
        }

        TaskDateTime targetDateTime = TaskDateTime.parse(details);
        LocalDate targetDate = targetDateTime.toLocalDate();
        String dateHeader = targetDate.format(TaskDateTime.DISPLAY_DATE_FORMAT);

        ArrayList<Task> matchingTasks = this.tasks.findTasksOn(targetDate);

        if (matchingTasks.isEmpty()) {
            return new CommandResult("No tasks found for " + dateHeader + ".",
                    ResponseType.LIST);
        }

        String response = formatTasksOnDate(
                "Tasks occurring on " + dateHeader + ":\n",
                matchingTasks, targetDate);
        return new CommandResult(response, ResponseType.LIST);
    }

    /**
     * Finds tasks containing the supplied keyword.
     *
     * @param details Arguments supplied after the command word.
     * @return Response message and command type.
     * @throws OzException If the command arguments are invalid.
     */
    private CommandResult findTasks(String details) throws OzException {
        if (details.isBlank()) {
            throw new OzException(EMPTY_FIND_KEYWORD_MESSAGE);
        }

        ArrayList<Task> matchingTasks = this.tasks.findTasksByKeyword(details);

        if (matchingTasks.isEmpty()) {
            return new CommandResult("No matching tasks found in the ledger.", ResponseType.FIND);
        }

        String response = formatTaskList("Matching tasks located:\n",
                matchingTasks);
        return new CommandResult(response, ResponseType.FIND);
    }

    /**
     * Marks the selected task as done and saves the updated list.
     *
     * @param details Arguments supplied after the command word.
     * @return Response message and command type.
     * @throws OzException If the command arguments are invalid.
     */
    private CommandResult markTask(String details) throws OzException {
        if (countFlagOccurrences(details, "/on") > 1) {
            throw new OzException(String.format(DUPLICATE_FLAG_MESSAGE_FORMAT, "/on"));
        }
        if (countFlagOccurrences(details, "/by") > 0
                || countFlagOccurrences(details, "/from") > 0
                || countFlagOccurrences(details, "/to") > 0) {
            throw new OzException(String.format(UNEXPECTED_FLAG_MESSAGE_FORMAT,
                    "flag", "mark", "Use: mark <number> [/on <date>]."));
        }

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
            try {
                this.storage.save(this.tasks);
            } catch (OzException exception) {
                recurringEvent.unmarkOccurrence(occurrenceDate);
                throw exception;
            }
            return new CommandResult("*Oink* Marked occurrence as done:\n  "
                    + recurringEvent.toOccurrenceString(occurrenceDate), ResponseType.CHANGE_MARK);
        }

        int index = parseTaskIndex(details, this.tasks.size());
        Task task = this.tasks.get(index);
        if (task instanceof RecurringEvent) {
            throw new OzException(MARK_RECURRING_USAGE_MESSAGE);
        }
        if (task.isDone()) {
            throw new OzException(String.format(TASK_ALREADY_DONE_MESSAGE_FORMAT, index + 1));
        }

        this.tasks.markAsDone(index);
        try {
            this.storage.save(this.tasks);
        } catch (OzException exception) {
            this.tasks.markAsNotDone(index);
            throw exception;
        }
        return new CommandResult("*Oink* Marked as done:\n  " + this.tasks.get(index),
                ResponseType.CHANGE_MARK);
    }

    /**
     * Marks the selected task as not done and saves the updated list.
     *
     * @param details Arguments supplied after the command word.
     * @return Response message and command type.
     * @throws OzException If the command arguments are invalid.
     */
    private CommandResult unmarkTask(String details) throws OzException {
        if (countFlagOccurrences(details, "/on") > 1) {
            throw new OzException(String.format(DUPLICATE_FLAG_MESSAGE_FORMAT, "/on"));
        }
        if (countFlagOccurrences(details, "/by") > 0
                || countFlagOccurrences(details, "/from") > 0
                || countFlagOccurrences(details, "/to") > 0) {
            throw new OzException(String.format(UNEXPECTED_FLAG_MESSAGE_FORMAT,
                    "flag", "unmark", "Use: unmark <number> [/on <date>]."));
        }

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
            try {
                this.storage.save(this.tasks);
            } catch (OzException exception) {
                recurringEvent.markOccurrence(occurrenceDate);
                throw exception;
            }
            return new CommandResult("*Snort* Marked occurrence as not done yet:\n  "
                    + recurringEvent.toOccurrenceString(occurrenceDate), ResponseType.CHANGE_MARK);
        }

        int index = parseTaskIndex(details, this.tasks.size());
        Task task = this.tasks.get(index);
        if (task instanceof RecurringEvent) {
            throw new OzException(UNMARK_RECURRING_USAGE_MESSAGE);
        }
        if (!task.isDone()) {
            throw new OzException(String.format(TASK_ALREADY_NOT_DONE_MESSAGE_FORMAT, index + 1));
        }

        this.tasks.markAsNotDone(index);
        try {
            this.storage.save(this.tasks);
        } catch (OzException exception) {
            this.tasks.markAsDone(index);
            throw exception;
        }
        return new CommandResult("*Snort* Marked as not done yet:\n  " + this.tasks.get(index),
                ResponseType.CHANGE_MARK);
    }

    /**
     * Validates and adds a todo task.
     *
     * @param details Arguments supplied after the command word.
     * @return Response message and command type.
     * @throws OzException If the command arguments are invalid.
     */
    private CommandResult addTodo(String details) throws OzException {
        if (details.isBlank()) {
            throw new OzException(EMPTY_TODO_DESCRIPTION_MESSAGE);
        }
        validateNoStorageDelimiter(details);
        if (countFlagOccurrences(details, "/by") > 0
                || countFlagOccurrences(details, "/from") > 0
                || countFlagOccurrences(details, "/to") > 0
                || countFlagOccurrences(details, "/on") > 0) {
            throw new OzException(TODO_UNEXPECTED_FLAG_MESSAGE);
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
    private CommandResult addDeadline(String details) throws OzException {
        validateNoStorageDelimiter(details);
        int byCount = countFlagOccurrences(details, "/by");
        if (byCount > 1) {
            throw new OzException(String.format(DUPLICATE_FLAG_MESSAGE_FORMAT, "/by"));
        }
        if (byCount == 0) {
            throw new OzException(DEADLINE_USAGE_MESSAGE);
        }
        if (countFlagOccurrences(details, "/from") > 0) {
            throw new OzException(String.format(UNEXPECTED_FLAG_MESSAGE_FORMAT,
                    "/from", "deadline", DEADLINE_USAGE_MESSAGE));
        }
        if (countFlagOccurrences(details, "/to") > 0) {
            throw new OzException(String.format(UNEXPECTED_FLAG_MESSAGE_FORMAT,
                    "/to", "deadline", DEADLINE_USAGE_MESSAGE));
        }

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
    private CommandResult addEvent(String details) throws OzException {
        validateNoStorageDelimiter(details);
        int fromCount = countFlagOccurrences(details, "/from");
        int toCount = countFlagOccurrences(details, "/to");
        if (fromCount > 1) {
            throw new OzException(String.format(DUPLICATE_FLAG_MESSAGE_FORMAT, "/from"));
        }
        if (toCount > 1) {
            throw new OzException(String.format(DUPLICATE_FLAG_MESSAGE_FORMAT, "/to"));
        }
        if (fromCount == 0 && toCount == 0) {
            throw new OzException(EVENT_USAGE_MESSAGE);
        }
        if (fromCount == 0) {
            throw new OzException(String.format(MISSING_FLAG_MESSAGE_FORMAT,
                    "/from", EVENT_USAGE_MESSAGE));
        }
        if (toCount == 0) {
            throw new OzException(String.format(MISSING_FLAG_MESSAGE_FORMAT,
                    "/to", EVENT_USAGE_MESSAGE));
        }
        if (details.indexOf("/to") < details.indexOf("/from")) {
            throw new OzException(String.format(MISPLACED_FLAG_MESSAGE_FORMAT,
                    "/from", "/to", EVENT_USAGE_MESSAGE));
        }
        if (countFlagOccurrences(details, "/by") > 0) {
            throw new OzException(String.format(UNEXPECTED_FLAG_MESSAGE_FORMAT,
                    "/by", "event", EVENT_USAGE_MESSAGE));
        }

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
    private CommandResult addRecurringEvent(String details) throws OzException {
        validateNoStorageDelimiter(details);
        int onCount = countFlagOccurrences(details, "/on");
        int startCount = countFlagOccurrences(details, "/start");
        int endCount = countFlagOccurrences(details, "/end");
        int everyCount = countFlagOccurrences(details, "/every");
        int untilCount = countFlagOccurrences(details, "/until");
        if (onCount > 1) {
            throw new OzException(String.format(DUPLICATE_FLAG_MESSAGE_FORMAT, "/on"));
        }
        if (startCount > 1) {
            throw new OzException(String.format(DUPLICATE_FLAG_MESSAGE_FORMAT, "/start"));
        }
        if (endCount > 1) {
            throw new OzException(String.format(DUPLICATE_FLAG_MESSAGE_FORMAT, "/end"));
        }
        if (everyCount > 1) {
            throw new OzException(String.format(DUPLICATE_FLAG_MESSAGE_FORMAT, "/every"));
        }
        if (untilCount > 1) {
            throw new OzException(String.format(DUPLICATE_FLAG_MESSAGE_FORMAT, "/until"));
        }
        if (onCount == 0 || startCount == 0 || endCount == 0 || everyCount == 0) {
            throw new OzException(RECURRING_EVENT_USAGE_MESSAGE);
        }
        int onIdx = details.indexOf("/on");
        int startIdx = details.indexOf("/start");
        int endIdx = details.indexOf("/end");
        int everyIdx = details.indexOf("/every");
        int untilIdx = details.indexOf("/until");
        if (!(onIdx < startIdx && startIdx < endIdx && endIdx < everyIdx
                && (untilIdx == -1 || everyIdx < untilIdx))) {
            throw new OzException("Recurring parameters are out of order. "
                    + RECURRING_EVENT_USAGE_MESSAGE);
        }

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
    private CommandResult deleteTask(String details) throws OzException {
        int index = parseTaskIndex(details, this.tasks.size());
        Task removedTask = this.tasks.delete(index);
        try {
            this.storage.save(this.tasks);
        } catch (OzException exception) {
            this.tasks.add(index, removedTask);
            throw exception;
        }
        return new CommandResult(String.format(
                """
                        Scrapped! Removed task:
                        %s
                        Now you have %d tasks in the list.
                        """,
                removedTask, this.tasks.size()).stripTrailing(), ResponseType.DELETE);
    }

    /**
     * Adds a validated task, saves the list, and reports the new task count.
     *
     * @param task Task to add.
     * @return Confirmation message and the add command type.
     * @throws OzException If saving the updated task list to storage fails.
     */
    private CommandResult addTask(Task task) throws OzException {
        this.tasks.add(task);
        try {
            this.storage.save(this.tasks);
        } catch (OzException exception) {
            this.tasks.delete(this.tasks.size() - 1);
            throw exception;
        }
        return new CommandResult(String.format(
                """
                        *Snort* Added to the list:
                        %s
                        Now you have %d tasks in the list.
                        """,
                task, this.tasks.size()).stripTrailing(), ResponseType.ADD);
    }

    /**
     * Formats tasks in their existing order with consecutive display numbers.
     *
     * @param header         Heading to place before the numbered tasks, including
     *                       its newline.
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
     * Formats tasks for a date query, expanding recurring series to their
     * occurrence view.
     *
     * @param header         Heading to place before the numbered tasks.
     * @param tasksToDisplay Tasks occurring on the target date.
     * @param targetDate     Date whose occurrences should be displayed.
     * @return Heading and numbered task descriptions without trailing whitespace.
     * @throws OzException If a recurring task cannot produce its expected
     *                     occurrence.
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
        String trimmed = argument == null ? "" : argument.trim();
        if (trimmed.isEmpty()) {
            throw new OzException(EMPTY_TASK_NUMBER_MESSAGE);
        }

        int taskNumber;
        try {
            taskNumber = Integer.parseInt(trimmed);
        } catch (NumberFormatException exception) {
            if (trimmed.startsWith("-") && trimmed.substring(1).matches("\\d+")) {
                throw new OzException(NON_POSITIVE_TASK_NUMBER_MESSAGE);
            }
            if (trimmed.matches("\\d+")) {
                throw new OzException(TASK_NUMBER_TOO_LARGE_MESSAGE);
            }
            throw new OzException(INVALID_TASK_NUMBER_MESSAGE);
        }

        if (taskNumber <= 0) {
            throw new OzException(NON_POSITIVE_TASK_NUMBER_MESSAGE);
        }

        if (taskCount == 0) {
            throw new OzException(EMPTY_TASK_LIST_MESSAGE);
        }

        if (taskNumber > taskCount) {
            throw new OzException(String.format(
                    TASK_INDEX_OUT_OF_BOUNDS_MESSAGE_FORMAT, taskNumber, taskCount));
        }

        int index = taskNumber - 1;
        assert index >= 0 && index < taskCount
                : "A validated task number must map to an existing index";
        return index;
    }

    /**
     * Counts occurrences of a parameter flag in the argument string.
     *
     * @param input Raw arguments string.
     * @param flag  Parameter flag including the leading slash (e.g., "/by").
     * @return Number of times the flag appears as a distinct argument word.
     */
    private static int countFlagOccurrences(String input, String flag) {
        Matcher matcher = Pattern.compile("(?<=\\s|^)" + Pattern.quote(flag) + "(?=\\s|$)").matcher(input);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /**
     * Validates that the input does not contain the storage delimiter '|'.
     * Credit to user Gnanes99 (https://github.com/Gnanes99) for this check.
     * see https://github.com/NUS-CS2103-AY2627-S1/forum/issues/238 for the whole
     * thread
     *
     * @param input Raw input text to validate.
     * @throws OzException If the input contains the '|' character.
     */
    private static void validateNoStorageDelimiter(String input) throws OzException {
        // Credit to Gnanes99 (https://github.com/Gnanes99): forbid '|' to prevent
        // storage corruption.
        if (input.contains("|")) {
            throw new OzException(RESERVED_DELIMITER_MESSAGE);
        }
    }

    /**
     * Entry point for running the Oz application.
     *
     * @param args Command-line arguments.
     */
    public static void main(String[] args) {
        Oz oz = new Oz(DEFAULT_STORAGE_PATH);
        new ConsoleUi(oz).run();
    }
}
