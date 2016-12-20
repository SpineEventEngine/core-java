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
public class OffsetTimesShould {

    @Test
    public void have_private_constructor() {
        assertTrue(hasPrivateUtilityConstructor(OffsetTimes.class));
    }

    @Test
    public void obtain_current_OffsetTime_using_ZoneOffset() {
        final int expectedSeconds = 3*Timestamps.SECONDS_PER_HOUR;
        final ZoneOffset inKiev = ZoneOffsets.ofHours(3);
        final OffsetTime now = OffsetTimes.now(inKiev);

        final Timestamp time = Timestamps.getCurrentTime();
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time.getSeconds() / 1000);

        assertEquals(calendar.get(Calendar.HOUR), now.getTime().getHours());
        assertEquals(calendar.get(Calendar.MINUTE), now.getTime().getMinutes());
        assertEquals(calendar.get(Calendar.SECOND), now.getTime().getSeconds());
        assertEquals(expectedSeconds, now.getOffset().getAmountSeconds());
        /* We cannot check milliseconds and nanos due to time gap between object creation */
    }

    @Test
    public void obtain_current_OffsetTime_using_LocalTime_and_ZoneOffset() {
        final int expectedSeconds = 5*Timestamps.SECONDS_PER_HOUR + 30*Timestamps.SECONDS_PER_MINUTE;
        final ZoneOffset inDelhi = ZoneOffsets.ofHoursMinutes(5, 30);
        final LocalTime inOneHour = LocalTimes.plusHours(1);
        final OffsetTime inOneHourInDelhi = OffsetTimes.of(inOneHour, inDelhi);

        final Timestamp time = Timestamps.getCurrentTime();
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time.getSeconds() / 1000);
        calendar.add(Calendar.HOUR, 1);


        assertEquals(calendar.get(Calendar.HOUR), inOneHourInDelhi.getTime().getHours());
        assertEquals(calendar.get(Calendar.MINUTE), inOneHourInDelhi.getTime().getMinutes());
        assertEquals(calendar.get(Calendar.SECOND), inOneHourInDelhi.getTime().getSeconds());
        assertEquals(expectedSeconds, inOneHourInDelhi.getOffset().getAmountSeconds());
         /* We cannot check milliseconds and nanos due to time gap between object creation */
    }
    

}
