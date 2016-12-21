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

import com.google.protobuf.Timestamp;
import org.junit.Test;
import org.spine3.protobuf.Timestamps;

import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.spine3.test.Tests.hasPrivateUtilityConstructor;

@SuppressWarnings("InstanceMethodNamingConvention")

public class OffsetDateTimesShould {

    @Test
    public void have_private_constructor() {
        assertTrue(hasPrivateUtilityConstructor(OffsetDateTimes.class));
    }

    @Test
    public void obtain_current_OffsetDateTime_using_ZoneOffset() {
        final int expectedSeconds = 3*Timestamps.SECONDS_PER_HOUR;
        final ZoneOffset inKiev = ZoneOffsets.ofHours(3);
        final OffsetDateTime today = OffsetDateTimes.now(inKiev);

        final Calendar calendar = Calendar.getInstance();

        assertEquals(calendar.get(Calendar.YEAR), today.getDate().getYear());
        assertEquals(calendar.get(Calendar.MONTH) + 1, today.getDate().getMonthValue());
        assertEquals(calendar.get(Calendar.DAY_OF_MONTH), today.getDate().getDay());

        final Timestamp time = Timestamps.getCurrentTime();
        calendar.setTimeInMillis(time.getSeconds() / 1000);

        assertEquals(calendar.get(Calendar.HOUR), today.getTime().getHours());
        assertEquals(calendar.get(Calendar.MINUTE), today.getTime().getMinutes());
        assertEquals(calendar.get(Calendar.SECOND), today.getTime().getSeconds());
        assertEquals(expectedSeconds, today.getOffset().getAmountSeconds());
        /* We cannot check milliseconds and nanos due to time gap between object creation */
    }

    @Test
    public void obtain_current_OffsetDateTime_using_OffsetDate_OffsetTime_ZoneOffset() {
        final int expectedSeconds = 5* Timestamps.SECONDS_PER_HOUR + 30*Timestamps.SECONDS_PER_MINUTE;
        final ZoneOffset inDelhi = ZoneOffsets.ofHoursMinutes(5, 30);
        final LocalDate today = LocalDates.now();
        final LocalTime now = LocalTimes.now();
        final OffsetDateTime todayInDelhi = OffsetDateTimes.of(today, now, inDelhi);

        final Calendar calendar = Calendar.getInstance();

        assertEquals(calendar.get(Calendar.YEAR), todayInDelhi.getDate().getYear());
        assertEquals(calendar.get(Calendar.MONTH) + 1, todayInDelhi.getDate().getMonthValue());
        assertEquals(calendar.get(Calendar.DAY_OF_MONTH), todayInDelhi.getDate().getDay());

        final Timestamp time = Timestamps.getCurrentTime();
        calendar.setTimeInMillis(time.getSeconds() / 1000);

        assertEquals(calendar.get(Calendar.HOUR), todayInDelhi.getTime().getHours());
        assertEquals(calendar.get(Calendar.MINUTE), todayInDelhi.getTime().getMinutes());
        assertEquals(calendar.get(Calendar.SECOND), todayInDelhi.getTime().getSeconds());
        assertEquals(expectedSeconds, todayInDelhi.getOffset().getAmountSeconds());
        /* We cannot check milliseconds and nanos due to time gap between object creation */
    }

}
