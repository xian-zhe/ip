package oz.parser;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oz.exception.OzException;
import oz.task.Deadline;
import oz.task.Event;
import oz.task.RecurringEvent;
import oz.task.Task;
import oz.task.TaskDateTime;
import oz.task.ToDo;

/**
 * Parses task-creation command arguments into validated task objects.
 */
public final class TaskParser {
    /** Error template shown when duplicate parameter flags are detected. */
    private static final String DUPLICATE_FLAG_MESSAGE_FORMAT = "Duplicate '%s' parameter detected.";

    /** Error shown when an unexpected parameter flag is supplied to todo. */
    private static final String TODO_UNEXPECTED_FLAG_MESSAGE =
            "The todo command does not accept parameter flags like /by, /from, or /to.";

    /** Error template shown when a required parameter flag is missing. */
    private static final String MISSING_FLAG_MESSAGE_FORMAT = "Missing required '%s' parameter. %s";

    /** Error template shown when parameters are supplied out of order. */
    private static final String MISPLACED_FLAG_MESSAGE_FORMAT =
            "The '%s' parameter must precede '%s'. %s";

    /** Error template shown when an unexpected parameter flag is supplied. */
    private static final String UNEXPECTED_FLAG_MESSAGE_FORMAT =
            "Unexpected '%s' parameter in %s command. %s";

    /** Error shown when task input contains the storage delimiter. */
    private static final String RESERVED_DELIMITER_MESSAGE =
            "Task input cannot contain the '|' character because it is reserved for storage.";

    /** Error shown when a todo description is missing. */
    private static final String EMPTY_TODO_DESCRIPTION_MESSAGE =
            "The description of a todo cannot be empty.";

    /** Usage message for adding a deadline. */
    private static final String DEADLINE_USAGE_MESSAGE = "Use: deadline <description> /by <date>.";

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

    /** Error shown when recurring parameters are out of order. */
    private static final String RECURRING_PARAMETER_ORDER_MESSAGE =
            "Recurring parameters are out of order. ";

    /** Error shown when a recurring event description is missing. */
    private static final String EMPTY_RECURRING_DESCRIPTION_MESSAGE =
            "The description of a recurring event cannot be empty.";

    /** Error shown when a recurrence unit is not weekly. */
    private static final String INVALID_RECURRENCE_UNIT_MESSAGE =
            "The recurrence unit must be week or weeks.";

    /** Error shown when a recurrence interval is not positive. */
    private static final String INVALID_RECURRENCE_INTERVAL_MESSAGE =
            "The recurrence interval must be a positive whole number.";

    /** Pattern parsing deadline description and deadline argument. */
    private static final Pattern DEADLINE_ARGUMENTS_PATTERN = Pattern
            .compile("^(?<description>.+?)\\s+/by\\s+(?<byTime>.+)$");

    /** Pattern parsing event description, start, and end arguments. */
    private static final Pattern EVENT_ARGUMENTS_PATTERN = Pattern
            .compile("^(?<description>.+?)\\s+/from\\s+(?<fromTime>.+?)\\s+/to\\s+(?<toTime>.+)$");

    /** Pattern parsing a weekly recurring event and optional end date. */
    private static final Pattern RECURRING_EVENT_ARGUMENTS_PATTERN = Pattern.compile(
            "^(?<description>.+?)\\s+/on\\s+(?<eventDate>.+?)"
                    + "\\s+/start\\s+(?<startTime>.+?)\\s+/end\\s+(?<endTime>.+?)"
                    + "\\s+/every\\s+(?<interval>\\S+)\\s+(?<unit>\\S+)"
                    + "(?:\\s+/until\\s+(?<untilDate>.+))?$");

    /** Prevents instantiation of this stateless parser. */
    private TaskParser() {
    }

    /**
     * Parses arguments for a todo task.
     *
     * @param arguments Todo command arguments.
     * @return Parsed todo task.
     * @throws OzException If the arguments are invalid.
     */
    public static Task parseTodo(String arguments) throws OzException {
        if (arguments.isBlank()) {
            throw new OzException(EMPTY_TODO_DESCRIPTION_MESSAGE);
        }
        validateNoStorageDelimiter(arguments);
        if (countFlagOccurrences(arguments, "/by") > 0
                || countFlagOccurrences(arguments, "/from") > 0
                || countFlagOccurrences(arguments, "/to") > 0
                || countFlagOccurrences(arguments, "/on") > 0) {
            throw new OzException(TODO_UNEXPECTED_FLAG_MESSAGE);
        }
        return new ToDo(arguments);
    }

    /**
     * Parses arguments for a deadline task.
     *
     * @param arguments Deadline command arguments.
     * @return Parsed deadline task.
     * @throws OzException If the arguments are invalid.
     */
    public static Task parseDeadline(String arguments) throws OzException {
        validateNoStorageDelimiter(arguments);
        int byCount = countFlagOccurrences(arguments, "/by");
        if (byCount > 1) {
            throw new OzException(String.format(DUPLICATE_FLAG_MESSAGE_FORMAT, "/by"));
        }
        if (byCount == 0) {
            throw new OzException(DEADLINE_USAGE_MESSAGE);
        }
        if (countFlagOccurrences(arguments, "/from") > 0) {
            throw new OzException(String.format(UNEXPECTED_FLAG_MESSAGE_FORMAT,
                    "/from", "deadline", DEADLINE_USAGE_MESSAGE));
        }
        if (countFlagOccurrences(arguments, "/to") > 0) {
            throw new OzException(String.format(UNEXPECTED_FLAG_MESSAGE_FORMAT,
                    "/to", "deadline", DEADLINE_USAGE_MESSAGE));
        }

        Matcher deadlineMatcher = DEADLINE_ARGUMENTS_PATTERN.matcher(arguments);
        if (!deadlineMatcher.matches()) {
            throw new OzException(DEADLINE_USAGE_MESSAGE);
        }

        String description = deadlineMatcher.group("description").trim();
        String deadlineTimeArgument = deadlineMatcher.group("byTime").trim();
        if (description.isEmpty()) {
            throw new OzException(EMPTY_DEADLINE_DESCRIPTION_MESSAGE);
        }
        if (deadlineTimeArgument.isEmpty()) {
            throw new OzException(EMPTY_DEADLINE_DATE_TIME_MESSAGE);
        }

        TaskDateTime deadlineTime = TaskDateTime.parse(deadlineTimeArgument);
        return new Deadline(description, deadlineTime);
    }

    /**
     * Parses arguments for an event task.
     *
     * @param arguments Event command arguments.
     * @return Parsed event task.
     * @throws OzException If the arguments are invalid.
     */
    public static Task parseEvent(String arguments) throws OzException {
        validateNoStorageDelimiter(arguments);
        int fromCount = countFlagOccurrences(arguments, "/from");
        int toCount = countFlagOccurrences(arguments, "/to");
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
        if (arguments.indexOf("/to") < arguments.indexOf("/from")) {
            throw new OzException(String.format(MISPLACED_FLAG_MESSAGE_FORMAT,
                    "/from", "/to", EVENT_USAGE_MESSAGE));
        }
        if (countFlagOccurrences(arguments, "/by") > 0) {
            throw new OzException(String.format(UNEXPECTED_FLAG_MESSAGE_FORMAT,
                    "/by", "event", EVENT_USAGE_MESSAGE));
        }

        Matcher eventMatcher = EVENT_ARGUMENTS_PATTERN.matcher(arguments);
        if (!eventMatcher.matches()) {
            throw new OzException(EVENT_USAGE_MESSAGE);
        }

        String description = eventMatcher.group("description").trim();
        String fromTimeArgument = eventMatcher.group("fromTime").trim();
        String toTimeArgument = eventMatcher.group("toTime").trim();
        if (description.isEmpty()) {
            throw new OzException(EMPTY_EVENT_DESCRIPTION_MESSAGE);
        }
        if (fromTimeArgument.isEmpty() || toTimeArgument.isEmpty()) {
            throw new OzException(EMPTY_EVENT_DATE_TIME_MESSAGE);
        }

        TaskDateTime fromTime = TaskDateTime.parse(fromTimeArgument);
        TaskDateTime toTime = TaskDateTime.parse(toTimeArgument);
        return new Event(description, fromTime, toTime);
    }

    /**
     * Parses arguments for a weekly recurring event.
     *
     * @param arguments Recurring event command arguments.
     * @return Parsed recurring event task.
     * @throws OzException If the arguments are invalid.
     */
    public static Task parseRecurringEvent(String arguments) throws OzException {
        validateNoStorageDelimiter(arguments);
        validateRecurringFlags(arguments);

        Matcher recurringEventMatcher = RECURRING_EVENT_ARGUMENTS_PATTERN.matcher(arguments);
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
        return new RecurringEvent(description, firstStartDateTime,
                firstEndDateTime, weekInterval, untilDate);
    }

    /**
     * Validates recurring-event flag presence, uniqueness, and order.
     *
     * @param arguments Recurring event command arguments.
     * @throws OzException If required flags are missing, repeated, or misplaced.
     */
    private static void validateRecurringFlags(String arguments) throws OzException {
        String[] flags = {"/on", "/start", "/end", "/every", "/until"};
        for (String flag : flags) {
            if (countFlagOccurrences(arguments, flag) > 1) {
                throw new OzException(String.format(DUPLICATE_FLAG_MESSAGE_FORMAT, flag));
            }
        }

        int onIndex = arguments.indexOf("/on");
        int startIndex = arguments.indexOf("/start");
        int endIndex = arguments.indexOf("/end");
        int everyIndex = arguments.indexOf("/every");
        int untilIndex = arguments.indexOf("/until");
        if (onIndex == -1 || startIndex == -1 || endIndex == -1 || everyIndex == -1) {
            throw new OzException(RECURRING_EVENT_USAGE_MESSAGE);
        }

        boolean areRequiredFlagsOrdered = onIndex < startIndex
                && startIndex < endIndex
                && endIndex < everyIndex;
        boolean isUntilFlagOrdered = untilIndex == -1 || everyIndex < untilIndex;
        if (!areRequiredFlagsOrdered || !isUntilFlagOrdered) {
            throw new OzException(RECURRING_PARAMETER_ORDER_MESSAGE
                    + RECURRING_EVENT_USAGE_MESSAGE);
        }
    }

    /**
     * Parses and validates a positive recurrence interval.
     *
     * @param argument Raw recurrence interval.
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
     * Counts appearances of a parameter flag as a distinct argument word.
     *
     * @param input Command arguments to inspect.
     * @param flag Parameter flag including its leading slash.
     * @return Number of distinct appearances of the flag.
     */
    private static int countFlagOccurrences(String input, String flag) {
        Matcher matcher = Pattern.compile(
                "(?<=\\s|^)" + Pattern.quote(flag) + "(?=\\s|$)").matcher(input);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /**
     * Rejects task text that would corrupt the pipe-delimited storage format.
     *
     * @param input Task arguments to validate.
     * @throws OzException If the input contains the storage delimiter.
     */
    private static void validateNoStorageDelimiter(String input) throws OzException {
        if (input.contains("|")) {
            throw new OzException(RESERVED_DELIMITER_MESSAGE);
        }
    }
}
