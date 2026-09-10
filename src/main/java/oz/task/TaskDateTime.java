package oz.task;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
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

    /** Time format omitting minutes for times on the hour. */
    private static final DateTimeFormatter DISPLAY_HOUR_FORMAT =
            DateTimeFormatter.ofPattern("ha", Locale.ENGLISH);

    /** Time format including minutes for times between whole hours. */
    private static final DateTimeFormatter DISPLAY_TIME_FORMAT =
            DateTimeFormatter.ofPattern("h:mma", Locale.ENGLISH);

    /** Formatter for saving date-only values to storage. */
    private static final DateTimeFormatter STORAGE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);

    /** Formatter for saving date-time values to storage. */
    private static final DateTimeFormatter STORAGE_DATETIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HHmm", Locale.ENGLISH);

    /** Supported input formatters for strings containing both date and time. */
    private static final DateTimeFormatter[] INPUT_DATETIME_FORMATTERS = new DateTimeFormatter[] {
        createFormatter("yyyy-MM-dd HHmm"),
        createFormatter("d/M/yyyy HHmm"),
        createFormatter("yyyy/M/d HHmm"),
        createFormatter("d-M-yyyy HHmm"),
        createFormatter("yyyy-MM-dd HH:mm"),
        createFormatter("d/M/yyyy HH:mm"),
        createFormatter("yyyy/M/d HH:mm"),
        createFormatter("d-M-yyyy HH:mm"),
        createFormatter("d/M/yyyy h:mma"),
        createFormatter("d/M/yyyy ha"),
        createFormatter("yyyy-MM-dd h:mma"),
        createFormatter("yyyy-MM-dd ha"),
        DateTimeFormatter.ISO_LOCAL_DATE_TIME
    };

    /** Supported input formatters for strings containing only a date. */
    private static final DateTimeFormatter[] INPUT_DATE_FORMATTERS = new DateTimeFormatter[] {
        createFormatter("yyyy-MM-dd"),
        createFormatter("d/M/yyyy"),
        createFormatter("yyyy/M/d"),
        createFormatter("d-M-yyyy"),
        DateTimeFormatter.ISO_LOCAL_DATE
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
        return new TaskDateTime(date.atStartOfDay(), false);
    }

    /**
     * Creates a value with an explicitly specified time, including midnight.
     *
     * @param dateTime Date and time to retain for display, storage, and comparisons.
     * @return Task value with an explicit time component.
     */
    public static TaskDateTime fromDateTime(LocalDateTime dateTime) {
        return new TaskDateTime(dateTime, true);
    }

    /**
     * Constructs a case-insensitive DateTimeFormatter from the given pattern.
     *
     * @param pattern Date/time format pattern string.
     * @return Case-insensitive DateTimeFormatter instance.
     */
    private static DateTimeFormatter createFormatter(String pattern) {
        return new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern(pattern)
                .toFormatter(Locale.ENGLISH);
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
            throw new OzException("Date/time argument cannot be empty.");
        }

        String trimmed = input.trim();

        for (DateTimeFormatter formatter : INPUT_DATETIME_FORMATTERS) {
            try {
                LocalDateTime parsedDateTime = LocalDateTime.parse(trimmed, formatter);
                return fromDateTime(parsedDateTime);
            } catch (DateTimeParseException ignored) {
                // Continue trying other formats
            }
        }

        for (DateTimeFormatter formatter : INPUT_DATE_FORMATTERS) {
            try {
                LocalDate parsedDate = LocalDate.parse(trimmed, formatter);
                return fromDate(parsedDate);
            } catch (DateTimeParseException ignored) {
                // Continue trying other formats
            }
        }

        throw new OzException("Please provide a valid date/time (e.g., 2019-10-15 or 2/12/2019 1800).");
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
