package oz.task;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.util.Locale;

import oz.exception.OzException;

/**
 * Represents a parsed date or date-time in the Oz chatbot.
 * Encapsulates java.time objects for formatting and persistence.
 */
public class TaskDateTime {
    /** Date format shared by task descriptions and date-filtered list headings. */
    public static final DateTimeFormatter DISPLAY_DATE_FORMAT =
            DateTimeFormatter.ofPattern("MMM dd yyyy", Locale.ENGLISH);

    /** Error shown when a date does not exist on the calendar. */
    private static final String NON_EXISTENT_DATE_MESSAGE =
            "That date does not exist on the calendar (e.g., February 30).";

    /** Error shown when a required date-time value is missing. */
    private static final String EMPTY_DATE_TIME_MESSAGE = "Date/time argument cannot be empty.";

    /** Error shown when a date-time value has no supported format. */
    private static final String INVALID_DATE_TIME_MESSAGE =
            "Please provide a valid date/time (e.g., 2019-10-15 or 2/12/2019 1800).";

    /** Error shown when a required date value is missing. */
    private static final String EMPTY_DATE_MESSAGE = "Date argument cannot be empty.";

    /** Error shown when a date value has no supported format. */
    private static final String INVALID_DATE_MESSAGE =
            "Please provide a valid date (e.g., 2019-10-15 or 2/12/2019).";

    /** Error shown when a required time value is missing. */
    private static final String EMPTY_TIME_MESSAGE = "Time argument cannot be empty.";

    /** Error shown when a time value has no supported format. */
    private static final String INVALID_TIME_MESSAGE =
            "Please provide a valid time (e.g., 1400, 14:00, or 2pm).";

    /** Time format omitting minutes for times on the hour. */
    private static final DateTimeFormatter DISPLAY_HOUR_FORMAT =
            DateTimeFormatter.ofPattern("ha", Locale.ENGLISH);

    /** Time format including minutes for times between whole hours. */
    private static final DateTimeFormatter DISPLAY_TIME_FORMAT =
            DateTimeFormatter.ofPattern("h:mma", Locale.ENGLISH);

    /** Formatter for saving date-only values to storage. */
    private static final DateTimeFormatter STORAGE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd", Locale.ENGLISH);

    /** Formatter for saving date-time values to storage. */
    private static final DateTimeFormatter STORAGE_DATETIME_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HHmm", Locale.ENGLISH);

    /** Supported input formatters for strings containing both date and time. */
    private static final DateTimeFormatter[] INPUT_DATETIME_FORMATTERS = new DateTimeFormatter[] {
        createFormatter("uuuu-MM-dd HHmm"),
        createFormatter("d/M/uuuu HHmm"),
        createFormatter("uuuu/M/d HHmm"),
        createFormatter("d-M-uuuu HHmm"),
        createFormatter("uuuu-MM-dd HH:mm"),
        createFormatter("d/M/uuuu HH:mm"),
        createFormatter("uuuu/M/d HH:mm"),
        createFormatter("d-M-uuuu HH:mm"),
        createFormatter("d/M/uuuu h:mma"),
        createFormatter("d/M/uuuu ha"),
        createFormatter("uuuu-MM-dd h:mma"),
        createFormatter("uuuu-MM-dd ha"),
        DateTimeFormatter.ISO_LOCAL_DATE_TIME.withResolverStyle(ResolverStyle.STRICT)
    };

    /** Supported input formatters for strings containing only a date. */
    private static final DateTimeFormatter[] INPUT_DATE_FORMATTERS = new DateTimeFormatter[] {
        createFormatter("uuuu-MM-dd"),
        createFormatter("d/M/uuuu"),
        createFormatter("uuuu/M/d"),
        createFormatter("d-M-uuuu"),
        DateTimeFormatter.ISO_LOCAL_DATE.withResolverStyle(ResolverStyle.STRICT)
    };

    /** Supported input formatters for strings containing only a time. */
    private static final DateTimeFormatter[] INPUT_TIME_FORMATTERS = new DateTimeFormatter[] {
        createFormatter("HHmm"),
        createFormatter("H:mm"),
        createFormatter("h:mma"),
        createFormatter("ha")
    };

    /** Parsed date-time object. */
    private final LocalDateTime dateTime;

    /** Whether the user specified a time component. */
    private final boolean hasTime;

    /**
     * Constructs a TaskDateTime with a LocalDateTime instance and time flag.
     *
     * @param dateTime The parsed LocalDateTime.
     * @param hasTime Whether the time component was explicitly provided.
     */
    private TaskDateTime(LocalDateTime dateTime, boolean hasTime) {
        assert dateTime != null : "The date-time value must be non-null";
        assert hasTime || dateTime.toLocalTime().equals(LocalTime.MIDNIGHT)
                  : "A date-only value must be stored at midnight";
        this.dateTime = dateTime;
        this.hasTime = hasTime;
    }

    /**
     * Creates a date-only value, using the start of the day for comparisons.
     *
     * @param date Date with no explicitly specified time.
     * @return Date-only task value.
     */
    public static TaskDateTime fromDate(LocalDate date) {
        assert date != null : "The date value must be non-null";
        return new TaskDateTime(date.atStartOfDay(), false);
    }

    /**
     * Creates a value with an explicitly specified time, including midnight.
     *
     * @param dateTime Date and time to retain for display, storage, and comparisons.
     * @return Task value with an explicit time component.
     */
    public static TaskDateTime fromDateTime(LocalDateTime dateTime) {
        assert dateTime != null : "The date-time value must be non-null";
        return new TaskDateTime(dateTime, true);
    }

    /**
     * Constructs a strict, case-insensitive DateTimeFormatter from the given pattern.
     *
     * @param pattern Date/time format pattern string.
     * @return Strict, case-insensitive DateTimeFormatter instance.
     */
    private static DateTimeFormatter createFormatter(String pattern) {
        return new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern(pattern)
                .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
                .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
                .toFormatter(Locale.ENGLISH)
                .withResolverStyle(ResolverStyle.STRICT);
    }

    /**
     * Parses a raw date or date-time string into a TaskDateTime.
     *
     * @param input Raw date or date-time string.
     * @return A parsed TaskDateTime instance.
     * @throws OzException If the date/time string does not match any accepted format.
     */
    public static TaskDateTime parse(String input) throws OzException {
        if (input == null || input.isBlank()) {
            throw new OzException(EMPTY_DATE_TIME_MESSAGE);
        }

        String trimmed = input.trim();
        String invalidCalendarMessage = null;

        for (DateTimeFormatter formatter : INPUT_DATETIME_FORMATTERS) {
            try {
                LocalDateTime parsedDateTime = LocalDateTime.parse(trimmed, formatter);
                return fromDateTime(parsedDateTime);
            } catch (DateTimeParseException exception) {
                if (exception.getMessage() != null && exception.getMessage().contains("Invalid date")) {
                    invalidCalendarMessage = NON_EXISTENT_DATE_MESSAGE;
                }
            }
        }

        for (DateTimeFormatter formatter : INPUT_DATE_FORMATTERS) {
            try {
                LocalDate parsedDate = LocalDate.parse(trimmed, formatter);
                return fromDate(parsedDate);
            } catch (DateTimeParseException exception) {
                if (exception.getMessage() != null && exception.getMessage().contains("Invalid date")) {
                    invalidCalendarMessage = NON_EXISTENT_DATE_MESSAGE;
                }
            }
        }

        if (invalidCalendarMessage != null) {
            throw new OzException(invalidCalendarMessage);
        }

        throw new OzException(INVALID_DATE_TIME_MESSAGE);
    }

    /**
     * Parses a raw date string without accepting a time component.
     *
     * @param input Raw date string.
     * @return Parsed local date.
     * @throws OzException If the input is empty or does not match an accepted date format.
     */
    public static LocalDate parseDate(String input) throws OzException {
        if (input == null || input.isBlank()) {
            throw new OzException(EMPTY_DATE_MESSAGE);
        }

        String trimmed = input.trim();
        String invalidCalendarMessage = null;
        for (DateTimeFormatter formatter : INPUT_DATE_FORMATTERS) {
            try {
                return LocalDate.parse(trimmed, formatter);
            } catch (DateTimeParseException exception) {
                if (exception.getMessage() != null && exception.getMessage().contains("Invalid date")) {
                    invalidCalendarMessage = NON_EXISTENT_DATE_MESSAGE;
                }
            }
        }

        if (invalidCalendarMessage != null) {
            throw new OzException(invalidCalendarMessage);
        }

        throw new OzException(INVALID_DATE_MESSAGE);
    }

    /**
     * Parses a raw time string without accepting a date component.
     *
     * @param input Raw time string.
     * @return Parsed local time.
     * @throws OzException If the input is empty or does not match an accepted time format.
     */
    public static LocalTime parseTime(String input) throws OzException {
        if (input == null || input.isBlank()) {
            throw new OzException(EMPTY_TIME_MESSAGE);
        }

        String trimmed = input.trim();
        for (DateTimeFormatter formatter : INPUT_TIME_FORMATTERS) {
            try {
                return LocalTime.parse(trimmed, formatter);
            } catch (DateTimeParseException ignored) {
                // Continue trying other formats
            }
        }

        throw new OzException(INVALID_TIME_MESSAGE);
    }

    /**
     * Produces a copy shifted by the requested number of days.
     *
     * @param days Number of days to add, which may be negative.
     * @return Shifted date-time retaining whether a time was explicitly provided.
     */
    public TaskDateTime plusDays(long days) {
        return new TaskDateTime(this.dateTime.plusDays(days), this.hasTime);
    }

    /**
     * Formats the date/time for user display.
     *
     * @return Formatted string for user viewing.
     */
    public String toDisplayString() {
        if (!this.hasTime) {
            return this.dateTime.format(DISPLAY_DATE_FORMAT);
        }

        String timePart;
        if (this.dateTime.getMinute() == 0) {
            timePart = this.dateTime.format(DISPLAY_HOUR_FORMAT).toLowerCase();
        } else {
            timePart = this.dateTime.format(DISPLAY_TIME_FORMAT).toLowerCase();
        }
        return this.dateTime.format(DISPLAY_DATE_FORMAT) + ", " + timePart;
    }

    /**
     * Formats the date/time for disk storage.
     *
     * @return Formatted string for file persistence.
     */
    public String toStorageString() {
        if (this.hasTime) {
            return this.dateTime.format(STORAGE_DATETIME_FORMAT);
        }
        return this.dateTime.format(STORAGE_DATE_FORMAT);
    }

    /**
     * Checks if this date/time occurs after another date/time.
     *
     * @param other The other TaskDateTime to compare against.
     * @return True if this date/time is strictly after the other date/time.
     */
    public boolean isAfter(TaskDateTime other) {
        return this.dateTime.isAfter(other.dateTime);
    }

    /**
     * Checks if this date/time occurs before another date/time.
     *
     * @param other The other TaskDateTime to compare against.
     * @return True if this date/time is strictly before the other date/time.
     */
    public boolean isBefore(TaskDateTime other) {
        return this.dateTime.isBefore(other.dateTime);
    }

    /**
     * Extracts the LocalDate component of this date/time.
     *
     * @return The LocalDate representation.
     */
    public LocalDate toLocalDate() {
        return this.dateTime.toLocalDate();
    }

    @Override
    public String toString() {
        return toDisplayString();
    }
}
