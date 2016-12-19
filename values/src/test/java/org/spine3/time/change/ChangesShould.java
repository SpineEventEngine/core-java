/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.time.change;

import org.junit.Test;
import com.google.protobuf.Timestamp;
import org.spine3.protobuf.Timestamps;
import org.spine3.time.MonthOfYear;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.spine3.test.Tests.hasPrivateUtilityConstructor;

@SuppressWarnings({"ConstantConditions" /* We pass `null` to some of the methods to check handling of preconditions */,
        "ResultOfMethodCallIgnored" /* ...when methods throw exceptions */,
        "ClassWithTooManyMethods", "OverlyCoupledClass" /* we test many data types and utility methods */})
public class ChangesShould {

    @Test
    public void have_private_constructor() {
        assertTrue(hasPrivateUtilityConstructor(Changes.class));
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_Interval_previousStartValue() {
        final Timestamp fiveMinutesAgo = Timestamps.minutesAgo(5);
        final Timestamp fourMinutesAgo = Timestamps.minutesAgo(4);
        final Timestamp now = Timestamps.getCurrentTime();
        Changes.ofInterval(null, fourMinutesAgo, now, fiveMinutesAgo);
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_Interval_newStartValue() {
        final Timestamp fiveMinutesAgo = Timestamps.minutesAgo(5);
        final Timestamp fourMinutesAgo = Timestamps.minutesAgo(4);
        final Timestamp now = Timestamps.getCurrentTime();
        Changes.ofInterval(fourMinutesAgo, null, now, fiveMinutesAgo);
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_Interval_previousEndValue() {
        final Timestamp fiveMinutesAgo = Timestamps.minutesAgo(5);
        final Timestamp fourMinutesAgo = Timestamps.minutesAgo(4);
        final Timestamp now = Timestamps.getCurrentTime();
        Changes.ofInterval(now, fourMinutesAgo, null, fiveMinutesAgo);
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_Interval_newEndValue() {
        final Timestamp fiveMinutesAgo = Timestamps.minutesAgo(5);
        final Timestamp fourMinutesAgo = Timestamps.minutesAgo(4);
        final Timestamp now = Timestamps.getCurrentTime();
        Changes.ofInterval(fourMinutesAgo, fiveMinutesAgo, now, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_equal_Interval_values() {
        final Timestamp fourMinutesAgo = Timestamps.minutesAgo(4);
        final Timestamp now = Timestamps.getCurrentTime();
        Changes.ofInterval(now, now, fourMinutesAgo, fourMinutesAgo);
    }

    @Test
    public void create_IntervalChange_instance() {
        final Timestamp fiveMinutesAgo = Timestamps.minutesAgo(5);
        final Timestamp now = Timestamps.getCurrentTime();

        final IntervalChange result = Changes.ofInterval(fiveMinutesAgo, now, fiveMinutesAgo, now);

        assertEquals(fiveMinutesAgo, result.getPreviousStartValue());
        assertEquals(now, result.getNewStartValue());
        assertEquals(fiveMinutesAgo, result.getPreviousEndValue());
        assertEquals(now, result.getNewEndValue());
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_LocalDate_previousMonthValue() {
        final MonthOfYear newMonthValue = MonthOfYear.JANUARY;
        final int previousYearValue = 1984;
        final int newYearValue = 1983;
        final int previousDayValue = 6;
        final int newDayValue = 3;

        Changes.ofLocalDate(previousYearValue, null, previousDayValue,
                            newYearValue, newMonthValue, newDayValue);
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_LocalDate_newMonthValue() {
        final MonthOfYear previousMonthValue = MonthOfYear.JANUARY;
        final int previousYearValue = 1984;
        final int newYearValue = 1983;
        final int previousDayValue = 4;
        final int newDayValue = 5;

        Changes.ofLocalDate(previousYearValue, previousMonthValue, previousDayValue,
                            newYearValue, null, newDayValue);
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_equal_LocalDate_values() {
        final MonthOfYear monthValue = MonthOfYear.JANUARY;
        final int yearValue = 1984;
        final int dayValue = 5;

        Changes.ofLocalDate(yearValue, monthValue, dayValue,
                            yearValue, monthValue, dayValue);
    }

    @Test
    public void create_LocalDateChange_instance() {
        final MonthOfYear previousMonthValue = MonthOfYear.JANUARY;
        final MonthOfYear newMonthValue = MonthOfYear.APRIL;
        final int previousYearValue = 1984;
        final int newYearValue = 1983;
        final int previousDayValue = 20;
        final int newDayValue = 31;

        final LocalDateChange result = Changes.ofLocalDate(previousYearValue, previousMonthValue, previousDayValue,
                                                           newYearValue, newMonthValue, newDayValue);

        assertTrue(Integer.compare(previousYearValue, result.getPreviousYearValue()) == 0);
        assertEquals(previousMonthValue, result.getPreviousMonthValue());
        assertTrue(Integer.compare(previousDayValue, result.getPreviousDayValue()) == 0);
        assertTrue(Integer.compare(newYearValue, result.getNewYearValue()) == 0);
        assertEquals(newMonthValue, result.getNewMonthValue());
        assertTrue(Integer.compare(newDayValue, result.getNewDayValue()) == 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_equal_LocalTime_values() {
        final int hoursValue = 12;
        final int minutesValue = 59;
        final int secondsValue = 1;
        final int millisValue = 9;
        final long nanosValue = 112L;

        Changes.ofLocalTime(hoursValue, minutesValue, secondsValue, millisValue, nanosValue,
                            hoursValue, minutesValue, secondsValue, millisValue, nanosValue);
    }

    @Test
    public void create_LocalTimeChange_instance() {
        final int previousHoursValue = 12;
        final int previousMinutesValue = 59;
        final int previousSecondsValue = 1;
        final int previousMillisValue = 9;
        final long previousNanosValue = 112L;
        final int newHoursValue = 11;
        final int newMinutesValue = 58;
        final int newSecondsValue = 2;
        final int newMillisValue = 10;
        final long newNanosValue = 111L;

        final LocalTimeChange result = Changes.ofLocalTime(previousHoursValue, previousMinutesValue,
                                                           previousSecondsValue, previousMillisValue,
                                                           previousNanosValue, newHoursValue,
                                                           newMinutesValue, newSecondsValue,
                                                           newMillisValue, newNanosValue);

        assertTrue(Integer.compare(previousHoursValue, result.getPreviousHoursValue()) == 0);
        assertTrue(Integer.compare(previousMinutesValue, result.getPreviousMinutesValue()) == 0);
        assertTrue(Integer.compare(previousSecondsValue, result.getPreviousSecondsValue()) == 0);
        assertTrue(Integer.compare(previousMillisValue, result.getPreviousMillisValue()) == 0);
        assertTrue(Long.compare(previousNanosValue, result.getPreviousNanosValue()) == 0);
        assertTrue(Integer.compare(newHoursValue, result.getNewHoursValue()) == 0);
        assertTrue(Integer.compare(newMinutesValue, result.getNewMinutesValue()) == 0);
        assertTrue(Integer.compare(newSecondsValue, result.getNewSecondsValue()) == 0);
        assertTrue(Integer.compare(newMillisValue, result.getNewMillisValue()) == 0);
        assertTrue(Long.compare(newNanosValue, result.getNewNanosValue()) == 0);
    }
}
