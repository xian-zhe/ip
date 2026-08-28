package oz.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import oz.exception.OzException;

/**
 * Tests the parsing, formatting, and comparison functionality of {@link TaskDateTime}.
 */
public class TaskDateTimeTest {

    @Test
    public void parse_validIsoDate_success() throws OzException {
        TaskDateTime dateTime = TaskDateTime.parse("2019-10-15");
        assertEquals("Oct 15 2019", dateTime.toDisplayString());
        assertEquals("2019-10-15", dateTime.toStorageString());
        assertEquals(LocalDate.of(2019, 10, 15), dateTime.toLocalDate());
        assertEquals("Oct 15 2019", dateTime.toString());
    }

    @Test
    public void parse_validSlashDate_success() throws OzException {
        TaskDateTime dateTime = TaskDateTime.parse("2/12/2019");
        assertEquals("Dec 02 2019", dateTime.toDisplayString());
        assertEquals("2019-12-02", dateTime.toStorageString());
    }

    @Test
    public void parse_validDashDate_success() throws OzException {
        TaskDateTime dateTime = TaskDateTime.parse("15-10-2019");
        assertEquals("Oct 15 2019", dateTime.toDisplayString());
        assertEquals("2019-10-15", dateTime.toStorageString());
    }

    @Test
    public void parse_validDateTime24h_success() throws OzException {
        TaskDateTime dateTime = TaskDateTime.parse("2/12/2019 1800");
        assertEquals("Dec 02 2019, 6pm", dateTime.toDisplayString());
        assertEquals("2019-12-02 1800", dateTime.toStorageString());
    }

    @Test
    public void parse_validDateTimeWithMinutes_success() throws OzException {
        TaskDateTime dateTime = TaskDateTime.parse("2019-10-15 1830");
        assertEquals("Oct 15 2019, 6:30pm", dateTime.toDisplayString());
        assertEquals("2019-10-15 1830", dateTime.toStorageString());
    }

    @Test
    public void parse_validDateTimeWithColon_success() throws OzException {
        TaskDateTime dateTime = TaskDateTime.parse("2019-10-15 18:45");
        assertEquals("Oct 15 2019, 6:45pm", dateTime.toDisplayString());
        assertEquals("2019-10-15 1845", dateTime.toStorageString());
    }

    @Test
    public void parse_valid12HourAmPm_success() throws OzException {
        TaskDateTime dateTimeLower = TaskDateTime.parse("2019-10-15 6pm");
        assertEquals("Oct 15 2019, 6pm", dateTimeLower.toDisplayString());
        assertEquals("2019-10-15 1800", dateTimeLower.toStorageString());

        TaskDateTime dateTimeUpper = TaskDateTime.parse("2/12/2019 9:15AM");
        assertEquals("Dec 02 2019, 9:15am", dateTimeUpper.toDisplayString());
        assertEquals("2019-12-02 0915", dateTimeUpper.toStorageString());
    }

    @Test
    public void parse_isoLocalDateTime_success() throws OzException {
        TaskDateTime dateTime = TaskDateTime.parse("2019-10-15T14:30:00");
        assertEquals("Oct 15 2019, 2:30pm", dateTime.toDisplayString());
        assertEquals("2019-10-15 1430", dateTime.toStorageString());
    }

    @Test
    public void parse_invalidDateFormat_exceptionThrown() {
        OzException exception = assertThrows(OzException.class, () -> TaskDateTime.parse("not-a-valid-date"));
        assertEquals("Please provide a valid date/time (e.g., 2019-10-15 or 2/12/2019 1800).", exception.getMessage());
    }

    @Test
    public void parse_emptyOrBlankInput_exceptionThrown() {
        OzException emptyException = assertThrows(OzException.class, () -> TaskDateTime.parse(""));
        assertEquals("Date/time argument cannot be empty.", emptyException.getMessage());

        OzException blankException = assertThrows(OzException.class, () -> TaskDateTime.parse("   "));
        assertEquals("Date/time argument cannot be empty.", blankException.getMessage());

        OzException nullException = assertThrows(OzException.class, () -> TaskDateTime.parse(null));
        assertEquals("Date/time argument cannot be empty.", nullException.getMessage());
    }

    @Test
    public void isAfter_earlierAndLaterDateTimes_correctComparison() throws OzException {
        TaskDateTime earlier = TaskDateTime.parse("2026-09-01 0900");
        TaskDateTime later = TaskDateTime.parse("2026-09-01 1800");

        assertTrue(later.isAfter(earlier));
        assertFalse(earlier.isAfter(later));
        assertTrue(earlier.isBefore(later));
        assertFalse(later.isBefore(earlier));
    }
}
