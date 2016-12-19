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

import com.google.protobuf.Timestamp;
import org.spine3.time.MonthOfYear;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utility class for working with field changes.
 *
 * @author Alexander Aleksandrov
 */
@SuppressWarnings("OverlyCoupledClass") /* ... because we want one utility class for all the TimeChanges classes. */
public class Changes {

    private static final String PREVIOUS_VALUE = "previousValue";
    private static final String NEW_VALUE = "newValue";
    private static final String ERR_CANNOT_BE_EQUAL = "newValue cannot be equal to previousValue";
    private static final String ERR_NEW_VALUE_CANNOT_BE_EMPTY = "newValue cannot be empty";

    private Changes() {
    }

    /**
     * Creates {@link IntervalChange} object for the passed previous and new field values of time interval.
     *
     * <p>Passed values cannot be equal.
     */
    public static IntervalChange ofInterval(Timestamp previousStartValue, Timestamp newStartValue,
                                            Timestamp previousEndValue, Timestamp newEndValue) {
        checkNotNull(previousStartValue, PREVIOUS_VALUE);
        checkNotNull(newStartValue, NEW_VALUE);
        checkNotNull(previousEndValue, PREVIOUS_VALUE);
        checkNotNull(newEndValue, NEW_VALUE);
        checkArgument(!newStartValue.equals(previousStartValue) && !newEndValue.equals(previousEndValue), ERR_CANNOT_BE_EQUAL);

        final IntervalChange result = IntervalChange.newBuilder()
                                                    .setPreviousStartValue(previousStartValue)
                                                    .setPreviousEndValue(previousEndValue)
                                                    .setNewStartValue(newStartValue)
                                                    .setNewEndValue(newEndValue)
                                                    .build();
        return result;
    }

    /**
     * Creates {@link LocalDateChange} object for the passed previous and new field values of local date.
     *
     * <p>Passed values cannot be equal.
     */
    @SuppressWarnings("MethodWithTooManyParameters")
    public static LocalDateChange ofLocalDate(int previousYearValue, MonthOfYear previousMonthValue,
                                              int previousDayValue, int newYearValue,
                                              MonthOfYear newMonthValue, int newDayValue) {
        checkNotNull(previousMonthValue, PREVIOUS_VALUE);
        checkNotNull(newMonthValue, NEW_VALUE);
        checkArgument(Integer.compare(newYearValue, previousYearValue) != 0
                              && Integer.compare(newDayValue, previousDayValue) != 0
                              && newMonthValue != previousMonthValue, ERR_CANNOT_BE_EQUAL);

        final LocalDateChange result = LocalDateChange.newBuilder()
                                                      .setPreviousYearValue(previousYearValue)
                                                      .setPreviousMonthValue(previousMonthValue)
                                                      .setPreviousDayValue(previousDayValue)
                                                      .setNewYearValue(newYearValue)
                                                      .setNewMonthValue(newMonthValue)
                                                      .setNewDayValue(newDayValue)
                                                      .build();
        return result;
    }

    /**
     * Creates {@link LocalTimeChange} object for the passed previous and new field values of local time.
     *
     * <p>Passed values cannot be equal.
     */
    @SuppressWarnings({"MethodWithTooManyParameters", "MethodWithMoreThanThreeNegations"})
    public static LocalTimeChange ofLocalTime(int previousHoursValue, int previousMinutesValue,
                                              int previousSecondsValue, int previousMillisValue,
                                              long previousNanosValue, int newHoursValue,
                                              int newMinutesValue, int newSecondsValue,
                                              int newMillisValue, long newNanosValue) {

        //noinspection OverlyComplexBooleanExpression
        checkArgument(Integer.compare(newHoursValue, previousHoursValue) != 0
                              && Integer.compare(newMinutesValue, previousMinutesValue) != 0
                              && Integer.compare(newSecondsValue, previousSecondsValue) != 0
                              && Integer.compare(newMillisValue, previousMillisValue) != 0
                              && Long.compare(newNanosValue, previousNanosValue) != 0, ERR_CANNOT_BE_EQUAL);

        final LocalTimeChange result = LocalTimeChange.newBuilder()
                                                      .setPreviousHoursValue(previousHoursValue)
                                                      .setPreviousMinutesValue(previousMinutesValue)
                                                      .setPreviousSecondsValue(previousSecondsValue)
                                                      .setPreviousMillisValue(previousMillisValue)
                                                      .setPreviousNanosValue(previousNanosValue)
                                                      .setNewHoursValue(newHoursValue)
                                                      .setNewMinutesValue(newMinutesValue)
                                                      .setNewSecondsValue(newSecondsValue)
                                                      .setNewMillisValue(newMillisValue)
                                                      .setNewNanosValue(newNanosValue)
                                                      .build();
        return result;
    }
}
