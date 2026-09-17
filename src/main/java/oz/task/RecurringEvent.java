package oz.task;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.TreeSet;
import java.util.stream.Collectors;

import oz.exception.OzException;

/**
 * Represents a same-day event that repeats at a fixed weekly interval.
 */
public class RecurringEvent extends Task {
    /** Task type code used in storage and display output. */
    public static final String TYPE_CODE = "R";

    /** Placeholder status required by the shared storage record structure. */
    public static final String STORAGE_SERIES_STATUS = STORAGE_NOT_DONE;

    /** Value representing an omitted optional storage field. */
    public static final String STORAGE_NONE = "-";

    /** Number of days in one week. */
    private static final int DAYS_PER_WEEK = 7;

    /** Start of the first occurrence. */
    private final TaskDateTime firstStartDateTime;

    /** End of the first occurrence. */
    private final TaskDateTime firstEndDateTime;

    /** Number of weeks between occurrence start dates. */
    private final int weekInterval;

    /** Last permitted occurrence start date, or null for an indefinite series. */
    private final LocalDate untilDate;

    /** Start dates of occurrences marked as completed. */
    private final TreeSet<LocalDate> completedOccurrenceDates = new TreeSet<>();

    /**
     * Constructs a weekly recurring same-day event.
     *
     * @param description Description of the recurring event.
     * @param firstStartDateTime Start of the first occurrence.
     * @param firstEndDateTime End of the first occurrence.
     * @param weekInterval Positive number of weeks between occurrences.
     * @param untilDate Inclusive final occurrence start date, or null for no end date.
     * @throws OzException If the event interval or recurrence rule is invalid.
     */
    public RecurringEvent(String description, TaskDateTime firstStartDateTime,
            TaskDateTime firstEndDateTime, int weekInterval, LocalDate untilDate) throws OzException {
        super(description);
        assert firstStartDateTime != null : "A recurring event must have a start date-time";
        assert firstEndDateTime != null : "A recurring event must have an end date-time";

        if (firstStartDateTime.isAfter(firstEndDateTime)) {
            throw new OzException("The start date/time (/from) cannot be after the end date/time (/to).");
        }
        if (!firstStartDateTime.toLocalDate().equals(firstEndDateTime.toLocalDate())) {
            throw new OzException("A recurring event must start and end on the same date.");
        }
        if (weekInterval <= 0) {
            throw new OzException("The recurrence interval must be a positive whole number.");
        }
        if (untilDate != null && untilDate.isBefore(firstStartDateTime.toLocalDate())) {
            throw new OzException("The recurrence end date (/until) cannot be before the first occurrence.");
        }

        this.firstStartDateTime = firstStartDateTime;
        this.firstEndDateTime = firstEndDateTime;
        this.weekInterval = weekInterval;
        this.untilDate = untilDate;
    }

    /**
     * Checks whether an occurrence starts on the supplied date.
     *
     * @param date Date to test.
     * @return True if the recurrence produces an occurrence on the date.
     */
    @Override
    public boolean occursOn(LocalDate date) {
        if (date == null || date.isBefore(this.firstStartDateTime.toLocalDate())) {
            return false;
        }
        if (this.untilDate != null && date.isAfter(this.untilDate)) {
            return false;
        }

        long daysFromFirstOccurrence = ChronoUnit.DAYS.between(
                this.firstStartDateTime.toLocalDate(), date);
        long recurrenceIntervalDays = (long) this.weekInterval * DAYS_PER_WEEK;
        return daysFromFirstOccurrence % recurrenceIntervalDays == 0;
    }

    /**
     * Marks the occurrence starting on the specified date as completed.
     *
     * @param date Start date of the occurrence to mark.
     * @throws OzException If no occurrence starts on the date or it is already marked.
     */
    public void markOccurrence(LocalDate date) throws OzException {
        validateOccurrenceDate(date);
        if (!this.completedOccurrenceDates.add(date)) {
            throw new OzException("The occurrence on " + formatDate(date) + " is already marked as done.");
        }
    }

    /**
     * Marks the occurrence starting on the specified date as incomplete.
     *
     * @param date Start date of the occurrence to unmark.
     * @throws OzException If no occurrence starts on the date or it is not marked.
     */
    public void unmarkOccurrence(LocalDate date) throws OzException {
        validateOccurrenceDate(date);
        if (!this.completedOccurrenceDates.remove(date)) {
            throw new OzException("The occurrence on " + formatDate(date) + " is not marked as done.");
        }
    }

    /**
     * Formats the occurrence starting on the supplied date.
     *
     * @param date Start date of the occurrence.
     * @return User-facing occurrence description.
     * @throws OzException If no occurrence starts on the date.
     */
    public String toOccurrenceString(LocalDate date) throws OzException {
        validateOccurrenceDate(date);
        long dayOffset = ChronoUnit.DAYS.between(this.firstStartDateTime.toLocalDate(), date);
        TaskDateTime occurrenceStart = this.firstStartDateTime.plusDays(dayOffset);
        TaskDateTime occurrenceEnd = this.firstEndDateTime.plusDays(dayOffset);
        String statusIcon = this.completedOccurrenceDates.contains(date) ? "X" : " ";
        return String.format("[" + TYPE_CODE + "][%s] %s (from: %s to: %s)",
                statusIcon, this.description,
                occurrenceStart.toDisplayString(), occurrenceEnd.toDisplayString());
    }

    /**
     * Converts the recurring event into its storage representation.
     *
     * @return Pipe-delimited recurring event record.
     */
    @Override
    public String toFileFormat() {
        String storedUntilDate = this.untilDate == null
                ? STORAGE_NONE
                : this.untilDate.toString();
        String storedCompletedDates = this.completedOccurrenceDates.isEmpty()
                ? STORAGE_NONE
                : this.completedOccurrenceDates.stream()
                        .map(LocalDate::toString)
                        .collect(Collectors.joining(","));
        return String.format(TYPE_CODE + " | %s | %s | %s | %s | %d | %s | %s",
                STORAGE_SERIES_STATUS, this.description,
                this.firstStartDateTime.toStorageString(), this.firstEndDateTime.toStorageString(),
                this.weekInterval, storedUntilDate, storedCompletedDates);
    }

    @Override
    public String toString() {
        String recurrence = "every " + this.weekInterval + " "
                + (this.weekInterval == 1 ? "week" : "weeks");
        if (this.untilDate != null) {
            recurrence += " until " + formatDate(this.untilDate);
        }
        return String.format("[" + TYPE_CODE + "] %s (from: %s to: %s; repeats: %s)",
                this.description, this.firstStartDateTime.toDisplayString(),
                this.firstEndDateTime.toDisplayString(), recurrence);
    }

    /**
     * Ensures that the supplied date identifies an occurrence in this series.
     *
     * @param date Date to validate.
     * @throws OzException If the date is null or not an occurrence start date.
     */
    private void validateOccurrenceDate(LocalDate date) throws OzException {
        if (!occursOn(date)) {
            String displayedDate = date == null ? "the supplied date" : formatDate(date);
            throw new OzException("The recurring task does not have an occurrence on "
                    + displayedDate + ".");
        }
    }

    /**
     * Formats a date consistently with other Oz task dates.
     *
     * @param date Date to format.
     * @return User-facing date string.
     */
    private static String formatDate(LocalDate date) {
        return date.format(TaskDateTime.DISPLAY_DATE_FORMAT);
    }
}
