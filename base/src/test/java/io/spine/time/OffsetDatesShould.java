/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.spine.time;

import com.google.common.testing.NullPointerTester;
import org.junit.Before;
import org.junit.Test;

import java.text.ParseException;
import java.util.Calendar;

import static io.spine.test.TestValues.random;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static io.spine.time.Calendars.at;
import static io.spine.time.Calendars.getDay;
import static io.spine.time.Calendars.getMonth;
import static io.spine.time.Calendars.getYear;
import static io.spine.time.OffsetDates.addDays;
import static io.spine.time.OffsetDates.addMonths;
import static io.spine.time.OffsetDates.addYears;
import static io.spine.time.OffsetDates.subtractDays;
import static io.spine.time.OffsetDates.subtractMonths;
import static io.spine.time.OffsetDates.subtractYears;
import static org.junit.Assert.assertEquals;

/**
 * @author Alexander Aleksandrov
 * @author Alexander Yevsyukov
 */
public class OffsetDatesShould extends AbstractZonedTimeTest {

    private static final int YEAR = 2017;
    private static final MonthOfYear MONTH = MonthOfYear.OCTOBER;
    private static final int DAY = 28;
    private static final int DAYS_DELTA = 7;
    private static final int MONTH_DELTA = 6;

    private LocalDate gmtToday;
    private OffsetDate today;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        gmtToday = LocalDates.of(YEAR, MONTH, DAY);
        today = OffsetDates.of(gmtToday, zoneOffset);
    }

    @Test
    public void have_private_constructor() {
        assertHasPrivateParameterlessCtor(OffsetDates.class);
    }

    @Test
    public void obtain_current_date() {
        final OffsetDate today = OffsetDates.now(zoneOffset);
        final Calendar cal = at(zoneOffset);

        final LocalDate date = today.getDate();

        assertEquals(getYear(cal), date.getYear());
        assertEquals(getMonth(cal), date.getMonthValue());
        assertEquals(getDay(cal), date.getDay());
        assertEquals(zoneOffset, today.getOffset());
    }

    @Test
    public void create_instance_by_local_date_and_offset() {
        final OffsetDate todayAtZone = OffsetDates.of(gmtToday, zoneOffset);

        /* We cannot have `assertEquals(gmtToday, todayAtZone.getDate())` because
           it would break at the day end at the time zone represented by `zoneOffset`. */

        assertEquals(zoneOffset, todayAtZone.getOffset());
    }

    /*
     * Math with date
     */

    @Test
    public void add_years() {
        final int yearsDelta = random(1, 2017);
        final OffsetDate offsetDate = OffsetDates.of(gmtToday, zoneOffset);
        final OffsetDate offsetDatePlusYears = addYears(offsetDate, yearsDelta);
        final LocalDate date = offsetDatePlusYears.getDate();
        LocalDates.checkDate(date);

        assertEquals(YEAR + yearsDelta, date.getYear());
        assertEquals(zoneOffset, offsetDate.getOffset());
    }

    @Test
    public void subtract_years() {
        final int yearsDelta = random(10, 2016);
        final OffsetDate offsetDate = OffsetDates.of(gmtToday, zoneOffset);
        final OffsetDate offsetDateMinusYears = subtractYears(offsetDate, yearsDelta);
        final LocalDate date = offsetDateMinusYears.getDate();
        LocalDates.checkDate(date);

        assertEquals(YEAR - yearsDelta, date.getYear());
        assertEquals(zoneOffset, offsetDate.getOffset());
    }

    @Test
    public void add_month() {
        final int monthDelta = random(1, 12);
        final OffsetDate value = OffsetDates.of(gmtToday, zoneOffset);
        final OffsetDate offsetDatePlusMonths = addMonths(value, monthDelta);
        int expectedMonth = (gmtToday.getMonth().getNumber() + monthDelta) % 12;
        if(expectedMonth<=0) {
            expectedMonth = 12 - expectedMonth;
        }

        final LocalDate date = offsetDatePlusMonths.getDate();
        LocalDates.checkDate(date);

        assertEquals(expectedMonth, date.getMonth().getNumber());
        assertEquals(zoneOffset, value.getOffset());
    }

    @Test
    public void subtract_month() {
        final OffsetDate offsetDate = OffsetDates.of(gmtToday, zoneOffset);
        final OffsetDate offsetDateMinusMonths = subtractMonths(offsetDate, MONTH_DELTA);

        final LocalDate date = offsetDateMinusMonths.getDate();
        LocalDates.checkDate(date);

        assertEquals(4, date.getMonth().getNumber());
        assertEquals(zoneOffset, offsetDate.getOffset());
    }

    @Test
    public void add_days() {
        final OffsetDate offsetDate = OffsetDates.of(gmtToday, zoneOffset);
        final OffsetDate offsetDatePlusMonths = addDays(offsetDate, DAYS_DELTA);

        final LocalDate date = offsetDatePlusMonths.getDate();
        LocalDates.checkDate(date);

        assertEquals(4, date.getDay());
        assertEquals(zoneOffset, offsetDate.getOffset());
    }

    @Test
    public void subtract_days() {
        final OffsetDate offsetDate = OffsetDates.of(gmtToday, zoneOffset);
        final OffsetDate offsetDateMinusDays = subtractDays(offsetDate, DAYS_DELTA);

        final LocalDate date = offsetDateMinusDays.getDate();
        LocalDates.checkDate(date);

        assertEquals(gmtToday.getDay() - DAYS_DELTA, date.getDay());
        assertEquals(zoneOffset, offsetDate.getOffset());
    }

    /*
     * Arguments check
     */

    @Test
    public void pass_null_tolerance_check() {
        final ZoneOffset zoneOffset = ZoneOffsets.getDefault();
        new NullPointerTester()
                .setDefault(ZoneOffset.class, zoneOffset)
                .setDefault(LocalDate.class, LocalDates.now())
                .setDefault(OffsetDate.class, OffsetDates.now(ZoneOffsets.UTC))
                .testAllPublicStaticMethods(OffsetDates.class);
    }

    /*
     * Illegal args. check for math with years.
     */

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_years_to_add() {
        addYears(today, -5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_years_to_subtract() {
        subtractYears(today, -6);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_zero_years_to_add() {
        addYears(today, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_zero_years_to_subtract() {
        subtractYears(today, 0);
    }

    /*
     * Illegal args. check for math with months.
     */

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_months_to_add() {
        addMonths(today, -7);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_months_to_subtract() {
        subtractMonths(today, -8);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_zero_months_to_add() {
        addMonths(today, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_zero_months_to_subtract() {
        subtractMonths(today, 0);
    }

    /*
     * Illegal args. check for math with days.
     */

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_days_to_subtract() {
        subtractDays(today, -27);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_days_to_add() {
        addDays(today, -25);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_zero_days_to_subtract() {
        subtractDays(today, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_zero_days_to_add() {
        addDays(today, 0);
    }

    /*
     * Stringification
     */

    @Override
    protected void assertConversionAt(ZoneOffset zoneOffset) throws ParseException {
        final OffsetDate today = OffsetDates.now(zoneOffset);
        final String str = OffsetDates.toString(today);
        final OffsetDate parsed = OffsetDates.parse(str);
        assertEquals(today, parsed);
    }
}
