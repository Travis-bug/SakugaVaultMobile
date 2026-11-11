package ca.gbc.comp3074.sakugavault.ui;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** Unit tests: the "5m" / "3h" / "2d" stamps on comments and history. */
public class UiTimeAgoTest {

    private static String ago(long minutes) {
        return OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(minutes).toString();
    }

    @Test
    public void emptyOrBadInputGivesEmptyString() {
        assertEquals("", Ui.timeAgo(null));
        assertEquals("", Ui.timeAgo(""));
        assertEquals("", Ui.timeAgo("not a date"));
    }

    @Test
    public void underAMinuteIsNow() {
        assertEquals("now", Ui.timeAgo(ago(0)));
        // A date in the future (clock skew) is also "now", never negative.
        assertEquals("now", Ui.timeAgo(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(5).toString()));
    }

    @Test
    public void minutesHoursDays() {
        assertEquals("5m", Ui.timeAgo(ago(5)));
        assertEquals("59m", Ui.timeAgo(ago(59)));
        assertEquals("1h", Ui.timeAgo(ago(60)));
        assertEquals("23h", Ui.timeAgo(ago(23 * 60 + 30)));
        assertEquals("1d", Ui.timeAgo(ago(24 * 60)));
        assertEquals("29d", Ui.timeAgo(ago(29 * 24 * 60)));
    }

    @Test
    public void olderThanAMonthShowsTheDate() {
        assertEquals("2026-01-15", Ui.timeAgo("2026-01-15T10:30:00Z"));
    }
}
