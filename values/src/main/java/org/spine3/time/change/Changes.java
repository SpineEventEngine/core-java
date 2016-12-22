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

import org.spine3.change.Changes.ErrorMessage;
import org.spine3.time.Interval;
import org.spine3.time.LocalDate;
import org.spine3.time.LocalTime;
import org.spine3.time.OffsetDate;
import org.spine3.time.OffsetDateTime;
import org.spine3.time.OffsetTime;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utility class for working with field changes.
 *
 * @author Alexander Aleksandrov
 */
public class Changes {

    public interface ArgumentName {
        String YEARS_TO_ADD = "yearsToAdd";
        String MONTHS_TO_ADD = "monthsToAdd";
        String DAYS_TO_ADD = "daysToAdd";
        String HOURS_TO_ADD = "hoursToAdd";
        String MINUTES_TO_ADD = "minutesToAdd";
        String SECONDS_TO_ADD = "secondsToAdd";
        String MILLIS_TO_ADD = "millisToAdd";
        String NANOS_TO_ADD = "nanosToAdd";
        String YEARS_TO_SUBTRACT = "yearsToSubtract";
        String MONTHS_TO_SUBTRACT = "monthsToSubtract";
        String DAYS_TO_SUBTRACT = "daysToSubtract";
        String HOURS_TO_SUBTRACT = "hoursToSubtract";
        String MINUTES_TO_SUBTRACT = "minutesToSubtract";
        String SECONDS_TO_SUBTRACT = "secondsToSubtract";
        String MILLIS_TO_SUBTRACT = "millisToSubtract";
        String NANOS_TO_SUBTRACT = "nanosToSubtract";
        String HOURS = "hours";
        String MINUTES = "minutes";
        String SECONDS = "seconds";
        String MILLIS = "millis";
        String NANOS = "nanos";
    }

    private Changes() {
    }

    /**
     * Creates {@link IntervalChange} object for the passed previous and new field values of time interval.
     *
     * <p>Passed values cannot be equal.
     */
    public static IntervalChange of(Interval previousValue, Interval newValue) {
        checkNotNull(previousValue, ErrorMessage.PREVIOUS_VALUE);
        checkNotNull(newValue, ErrorMessage.NEW_VALUE);
        checkArgument(!newValue.equals(previousValue), ErrorMessage.VALUES_CANNOT_BE_EQUAL);

        final IntervalChange result = IntervalChange.newBuilder()
                                                    .setPreviousValue(previousValue)
                                                    .setNewValue(newValue)
                                                    .build();
        return result;
    }

    /**
     * Creates {@link LocalDateChange} object for the passed previous and new field values of local date.
     *
     * <p>Passed values cannot be equal.
     */
    public static LocalDateChange of(LocalDate previousValue, LocalDate newValue) {
        checkNotNull(previousValue, ErrorMessage.PREVIOUS_VALUE);
        checkNotNull(newValue, ErrorMessage.NEW_VALUE);
        checkArgument(!newValue.equals(previousValue), ErrorMessage.VALUES_CANNOT_BE_EQUAL);

        final LocalDateChange result = LocalDateChange.newBuilder()
                                                      .setPreviousValue(previousValue)
                                                      .setNewValue(newValue)
                                                      .build();
        return result;
    }

    /**
     * Creates {@link LocalTimeChange} object for the passed previous and new field values of local time.
     *
     * <p>Passed values cannot be equal.
     */
    public static LocalTimeChange of(LocalTime previousValue, LocalTime newValue) {
        checkNotNull(previousValue, ErrorMessage.PREVIOUS_VALUE);
        checkNotNull(newValue, ErrorMessage.NEW_VALUE);
        checkArgument(!newValue.equals(previousValue), ErrorMessage.VALUES_CANNOT_BE_EQUAL);

        final LocalTimeChange result = LocalTimeChange.newBuilder()
                                                      .setPreviousValue(previousValue)
                                                      .setNewValue(newValue)
                                                      .build();
        return result;
    }

    /**
     * Creates {@link OffsetTimeChange} object for the passed previous and new field values of offset time.
     *
     * <p>Passed values cannot be equal.
     */
    public static OffsetTimeChange of(OffsetTime previousValue, OffsetTime newValue) {
        checkNotNull(previousValue, ErrorMessage.PREVIOUS_VALUE);
        checkNotNull(newValue, ErrorMessage.NEW_VALUE);
        checkArgument(!newValue.equals(previousValue), ErrorMessage.VALUES_CANNOT_BE_EQUAL);

        final OffsetTimeChange result = OffsetTimeChange.newBuilder()
                                                        .setPreviousValue(previousValue)
                                                        .setNewValue(newValue)
                                                        .build();
        return result;
    }

    /**
     * Creates {@link OffsetDateChange} object for the passed previous and new field values of offset time.
     *
     * <p>Passed values cannot be equal.
     */
    public static OffsetDateChange of(OffsetDate previousValue, OffsetDate newValue) {
        checkNotNull(previousValue, ErrorMessage.PREVIOUS_VALUE);
        checkNotNull(newValue, ErrorMessage.NEW_VALUE);
        checkArgument(!newValue.equals(previousValue), ErrorMessage.VALUES_CANNOT_BE_EQUAL);

        final OffsetDateChange result = OffsetDateChange.newBuilder()
                                                        .setPreviousValue(previousValue)
                                                        .setNewValue(newValue)
                                                        .build();
        return result;
    }

    /**
     * Creates {@link OffsetDateTimeChange} object for the passed previous and new field values of offset time.
     *
     * <p>Passed values cannot be equal.
     */
    public static OffsetDateTimeChange of(OffsetDateTime previousValue, OffsetDateTime newValue) {
        checkNotNull(previousValue, ErrorMessage.PREVIOUS_VALUE);
        checkNotNull(newValue, ErrorMessage.NEW_VALUE);
        checkArgument(!newValue.equals(previousValue), ErrorMessage.VALUES_CANNOT_BE_EQUAL);

        final OffsetDateTimeChange result = OffsetDateTimeChange.newBuilder()
                                                        .setPreviousValue(previousValue)
                                                        .setNewValue(newValue)
                                                        .build();
        return result;
    }
}
