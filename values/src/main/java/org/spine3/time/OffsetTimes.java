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
package org.spine3.time;

import java.util.Calendar;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.change.Changes.ErrorMessage;
import static org.spine3.time.Calendars.createTime;
import static org.spine3.time.Calendars.createTimeWithZoneOffset;
import static org.spine3.time.Calendars.getHours;
import static org.spine3.time.Calendars.getMillis;
import static org.spine3.time.Calendars.getMinutes;
import static org.spine3.time.Calendars.getSeconds;
import static org.spine3.time.change.Changes.ArgumentName;
import static org.spine3.validate.Validate.checkPositive;

/**
 * Routines for working with {@link OffsetTime}.
 *
 * @author Alexander Aleksandrov
 */
public class OffsetTimes {

    private OffsetTimes() {
    }

    /**
     * Obtains offset time using {@code ZoneOffset}.
     */
    public static OffsetTime now(ZoneOffset zoneOffset) {
        checkNotNull(zoneOffset, ErrorMessage.ZONE_OFFSET);
        final Calendar cal = createTimeWithZoneOffset(zoneOffset);
        final LocalTime localTime = LocalTimes.of(getHours(cal), getMinutes(cal), getSeconds(cal), getMillis(cal));
        final OffsetTime result = OffsetTime.newBuilder()
                                            .setTime(localTime)
                                            .setOffset(zoneOffset)
                                            .build();
        return result;
    }

    /**
     * Obtains offset time using {@code LocalTime} and {@code ZoneOffset}.
     */
    public static OffsetTime of(LocalTime localTime, ZoneOffset zoneOffset) {
        checkNotNull(localTime, ErrorMessage.LOCAL_TIME);
        checkNotNull(zoneOffset, ErrorMessage.ZONE_OFFSET);
        final OffsetTime result = OffsetTime.newBuilder()
                                            .setTime(localTime)
                                            .setOffset(zoneOffset)
                                            .build();
        return result;
    }

    /**
     * Obtains a copy of this offset time with the specified number of hours added.
     */
    public static OffsetTime plusHours(OffsetTime offsetTime, int hoursToAdd) {
        checkNotNull(offsetTime, ErrorMessage.OFFSET_TIME);
        checkPositive(hoursToAdd, ArgumentName.HOURS_TO_ADD);
        return changeHours(offsetTime, hoursToAdd);
    }

    /**
     * Obtains a copy of this offset time with the specified number of minutes added.
     */
    public static OffsetTime plusMinutes(OffsetTime offsetTime, int minutesToAdd) {
        checkNotNull(offsetTime, ErrorMessage.OFFSET_TIME);
        checkPositive(minutesToAdd, ArgumentName.MINUTES_TO_ADD);
        return changeMinutes(offsetTime, minutesToAdd);
    }

    /**
     * Obtains a copy of this offset time with the specified number of seconds added.
     */
    public static OffsetTime plusSeconds(OffsetTime offsetTime, int secondsToAdd) {
        checkNotNull(offsetTime, ErrorMessage.OFFSET_TIME);
        checkPositive(secondsToAdd, ArgumentName.SECONDS_TO_ADD);
        return changeSeconds(offsetTime, secondsToAdd);
    }

    /**
     * Obtains a copy of this offset time with the specified number of milliseconds added.
     */
    public static OffsetTime plusMillis(OffsetTime offsetTime, int millisToAdd) {
        checkNotNull(offsetTime, ErrorMessage.OFFSET_TIME);
        checkPositive(millisToAdd, ArgumentName.MILLIS_TO_ADD);
        return changeMillis(offsetTime, millisToAdd);
    }

    /**
     * Obtains a copy of this offset time with the specified number of hours subtracted.
     */
    public static OffsetTime minusHours(OffsetTime offsetTime, int hoursToSubtract) {
        checkNotNull(offsetTime, ErrorMessage.OFFSET_TIME);
        checkPositive(hoursToSubtract, ArgumentName.HOURS_TO_SUBTRACT);
        return changeHours(offsetTime, -hoursToSubtract);
    }

    /**
     * Obtains a copy of this offset time with the specified number of minutes subtracted.
     */
    public static OffsetTime minusMinutes(OffsetTime offsetTime, int minutesToSubtract) {
        checkNotNull(offsetTime, ErrorMessage.OFFSET_TIME);
        checkPositive(minutesToSubtract, ArgumentName.MINUTES_TO_SUBTRACT);
        return changeMinutes(offsetTime, -minutesToSubtract);
    }

    /**
     * Obtains a copy of this offset time with the specified number of seconds subtracted.
     */
    public static OffsetTime minusSeconds(OffsetTime offsetTime, int secondsToSubtract) {
        checkNotNull(offsetTime, ErrorMessage.OFFSET_TIME);
        checkPositive(secondsToSubtract, ArgumentName.SECONDS_TO_SUBTRACT);
        return changeSeconds(offsetTime, -secondsToSubtract);
    }

    /**
     * Obtains a copy of this offset time with the specified number of milliseconds subtracted.
     */
    public static OffsetTime minusMillis(OffsetTime offsetTime, int millisToSubtract) {
        checkNotNull(offsetTime, ErrorMessage.OFFSET_TIME);
        checkPositive(millisToSubtract, ArgumentName.MILLIS_TO_SUBTRACT);
        return changeMillis(offsetTime, -millisToSubtract);
    }

    /**
     * Obtains offset time changed on specified amount of hours.
     *
     * @param offsetTime  offset time that will be changed
     * @param hoursDelta a number of hours that needs to be added or subtracted that can be either positive or negative
     * @return copy of this offset time with new hours value
     */
    private static OffsetTime changeHours(OffsetTime offsetTime, int hoursDelta) {
        final Calendar cal = createTime(offsetTime.getTime().getHours(), offsetTime.getTime().getMinutes(),
                                        offsetTime.getTime().getSeconds(), offsetTime.getTime().getMillis());
        cal.add(Calendar.HOUR, hoursDelta);
        final LocalTime localTime = LocalTimes.of(getHours(cal), getMinutes(cal),
                                                  getSeconds(cal), getMillis(cal), offsetTime.getTime().getNanos());
        final ZoneOffset zoneOffset = offsetTime.getOffset();

        final OffsetTime result = OffsetTime.newBuilder()
                                            .setTime(localTime)
                                            .setOffset(zoneOffset)
                                            .build();
        return result;
    }

    /**
     * Obtains offset time changed on specified amount of minutes.
     *
     * @param offsetTime    offset time that will be changed
     * @param minutesDelta a number of minutes that needs to be added or subtracted that can be either positive or negative
     * @return copy of this offset time with new minutes value
     */
    private static OffsetTime changeMinutes(OffsetTime offsetTime, int minutesDelta) {
        final Calendar cal = createTime(offsetTime.getTime().getHours(), offsetTime.getTime().getMinutes(),
                                        offsetTime.getTime().getSeconds(), offsetTime.getTime().getMillis());
        cal.add(Calendar.MINUTE, minutesDelta);
        final LocalTime localTime = LocalTimes.of(getHours(cal), getMinutes(cal),
                                                  getSeconds(cal), getMillis(cal), offsetTime.getTime().getNanos());
        final ZoneOffset zoneOffset = offsetTime.getOffset();

        final OffsetTime result = OffsetTime.newBuilder()
                                            .setTime(localTime)
                                            .setOffset(zoneOffset)
                                            .build();
        return result;
    }

    /**
     * Obtains offset time changed on specified amount of seconds.
     *
     * @param offsetTime    offset time that will be changed
     * @param secondsDelta a number of seconds that needs to be added or subtracted that can be either positive or negative
     * @return copy of this offset time with new seconds value
     */
    private static OffsetTime changeSeconds(OffsetTime offsetTime, int secondsDelta) {
        final Calendar cal = createTime(offsetTime.getTime().getHours(), offsetTime.getTime().getMinutes(),
                                        offsetTime.getTime().getSeconds(), offsetTime.getTime().getMillis());
        cal.add(Calendar.SECOND, secondsDelta);
        final LocalTime localTime = LocalTimes.of(getHours(cal), getMinutes(cal),
                                                  getSeconds(cal), getMillis(cal), offsetTime.getTime().getNanos());
        final ZoneOffset zoneOffset = offsetTime.getOffset();

        final OffsetTime result = OffsetTime.newBuilder()
                                            .setTime(localTime)
                                            .setOffset(zoneOffset)
                                            .build();
        return result;
    }

    /**
     * Obtains offset time changed on specified amount of milliseconds.
     *
     * @param offsetTime   offset time that will be changed
     * @param millisDelta a number of milliseconds that needs to be added or subtracted that can be either positive or negative
     * @return copy of this offset time with new milliseconds value
     */
    private static OffsetTime changeMillis(OffsetTime offsetTime, int millisDelta) {
        final Calendar cal = createTime(offsetTime.getTime().getHours(), offsetTime.getTime().getMinutes(),
                                        offsetTime.getTime().getSeconds(), offsetTime.getTime().getMillis());
        cal.add(Calendar.MILLISECOND, millisDelta);
        final LocalTime localTime = LocalTimes.of(getHours(cal), getMinutes(cal),
                                                  getSeconds(cal), getMillis(cal), offsetTime.getTime().getNanos());
        final ZoneOffset zoneOffset = offsetTime.getOffset();

        final OffsetTime result = OffsetTime.newBuilder()
                                            .setTime(localTime)
                                            .setOffset(zoneOffset)
                                            .build();
        return result;
    }

}
