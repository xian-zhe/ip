import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Represents a parsed date or date-time in the Oz chatbot.
 * Encapsulates java.time objects for formatting and persistence.
 */
public class TaskDateTime {
    private static final DateTimeFormatter DISPLAY_DATE_FORMAT =
            DateTimeFormatter.ofPattern("MMM dd yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DISPLAY_DATETIME_FORMAT =
            DateTimeFormatter.ofPattern("MMM dd yyyy, h:mma", Locale.ENGLISH);
    private static final DateTimeFormatter STORAGE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);
    private static final DateTimeFormatter STORAGE_DATETIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HHmm", Locale.ENGLISH);

    private static final DateTimeFormatter[] INPUT_DATETIME_FORMATTERS = new DateTimeFormatter[] {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HHmm", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d/M/yyyy HHmm", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d/M/yyyy HH:mm", Locale.ENGLISH),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    };

    private static final DateTimeFormatter[] INPUT_DATE_FORMATTERS = new DateTimeFormatter[] {
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d/M/yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d-M-yyyy", Locale.ENGLISH),
            DateTimeFormatter.ISO_LOCAL_DATE
    };

    private final LocalDateTime dateTime;
    private final boolean hasTime;

    /**
     * Constructs a TaskDateTime with a LocalDateTime instance and time flag.
     *
     * @param dateTime The parsed LocalDateTime.
     * @param hasTime Whether the time component was explicitly provided.
     */
    public TaskDateTime(LocalDateTime dateTime, boolean hasTime) {
        this.dateTime = dateTime;
        this.hasTime = hasTime;
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
                return new TaskDateTime(parsedDateTime, true);
            } catch (DateTimeParseException ignored) {
                // Continue trying other formats
            }
        }

        for (DateTimeFormatter formatter : INPUT_DATE_FORMATTERS) {
            try {
                LocalDate parsedDate = LocalDate.parse(trimmed, formatter);
                return new TaskDateTime(parsedDate.atStartOfDay(), false);
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
        if (this.hasTime) {
            return this.dateTime.format(DISPLAY_DATETIME_FORMAT);
        }
        return this.dateTime.format(DISPLAY_DATE_FORMAT);
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

    @Override
    public String toString() {
        return toDisplayString();
    }
}
