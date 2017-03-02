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
package org.spine3.time.change;

import org.spine3.time.Interval;
import org.spine3.time.LocalDate;
import org.spine3.time.LocalTime;
import org.spine3.time.OffsetDate;
import org.spine3.time.OffsetDateTime;
import org.spine3.time.OffsetTime;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.change.Preconditions.checkNotEqual;

/**
 * Utility class for working with field changes.
 *
 * @author Alexander Aleksandrov
 */
public class Changes {

    private Changes() {
    }

    /**
     * Creates {@link IntervalChange} object for the passed previous and new field values of
     * time interval.
     *
     * <p>Passed values cannot be equal.
     */
    public static IntervalChange of(Interval previousValue, Interval newValue) {
        checkNotNull(previousValue);
        checkNotNull(newValue);
        checkNotEqual(previousValue, newValue);

        final IntervalChange result = IntervalChange.newBuilder()
                                                    .setPreviousValue(previousValue)
                                                    .setNewValue(newValue)
                                                    .build();
        return result;
    }

    /**
     * Creates {@link LocalDateChange} object for the passed previous and new field values of
     * local date.
     *
     * <p>Passed values cannot be equal.
     */
    public static LocalDateChange of(LocalDate previousValue, LocalDate newValue) {
        checkNotNull(previousValue);
        checkNotNull(newValue);
        checkNotEqual(previousValue, newValue);

        final LocalDateChange result = LocalDateChange.newBuilder()
                                                      .setPreviousValue(previousValue)
                                                      .setNewValue(newValue)
                                                      .build();
        return result;
    }

    /**
     * Creates {@link LocalTimeChange} object for the passed previous and new field values of
     * local time.
     *
     * <p>Passed values cannot be equal.
     */
    public static LocalTimeChange of(LocalTime previousValue, LocalTime newValue) {
        checkNotNull(previousValue);
        checkNotNull(newValue);
        checkNotEqual(previousValue, newValue);

        final LocalTimeChange result = LocalTimeChange.newBuilder()
                                                      .setPreviousValue(previousValue)
                                                      .setNewValue(newValue)
                                                      .build();
        return result;
    }

    /**
     * Creates {@link OffsetTimeChange} object for the passed previous and new field values of
     * offset time.
     *
     * <p>Passed values cannot be equal.
     */
    public static OffsetTimeChange of(OffsetTime previousValue, OffsetTime newValue) {
        checkNotNull(previousValue);
        checkNotNull(newValue);
        checkNotEqual(previousValue, newValue);

        final OffsetTimeChange result = OffsetTimeChange.newBuilder()
                                                        .setPreviousValue(previousValue)
                                                        .setNewValue(newValue)
                                                        .build();
        return result;
    }

    /**
     * Creates {@link OffsetDateChange} object for the passed previous and new field values of
     * offset time.
     *
     * <p>Passed values cannot be equal.
     */
    public static OffsetDateChange of(OffsetDate previousValue, OffsetDate newValue) {
        checkNotNull(previousValue);
        checkNotNull(newValue);
        checkNotEqual(previousValue, newValue);

        final OffsetDateChange result = OffsetDateChange.newBuilder()
                                                        .setPreviousValue(previousValue)
                                                        .setNewValue(newValue)
                                                        .build();
        return result;
    }

    /**
     * Creates {@link OffsetDateTimeChange} object for the passed previous and new field values of
     * offset time.
     *
     * <p>Passed values cannot be equal.
     */
    public static OffsetDateTimeChange of(OffsetDateTime previousValue, OffsetDateTime newValue) {
        checkNotNull(previousValue);
        checkNotNull(newValue);
        checkNotEqual(previousValue, newValue);

        final OffsetDateTimeChange result = OffsetDateTimeChange.newBuilder()
                                                                .setPreviousValue(previousValue)
                                                                .setNewValue(newValue)
                                                                .build();
        return result;
    }
}
