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

package org.spine3.time;

import org.junit.Test;

import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.spine3.protobuf.Timestamps2.MINUTES_PER_HOUR;
import static org.spine3.protobuf.Timestamps2.SECONDS_PER_MINUTE;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;
import static org.spine3.time.ZoneOffsets.getOffsetInSeconds;
import static org.spine3.time.ZoneOffsets.toZoneOffset;

public class ZoneOffsetsShould {

    private static final int MIN_HOURS_OFFSET = -11;
    private static final int MAX_HOURS_OFFSET = 14;
    private static final int MIN_MINUTES_OFFSET = 0;
    private static final int MAX_MINUTES_OFFSET = 60;
    public static final TimeZone timeZone = TimeZone.getDefault();

    @Test
    public void has_private_constructor() {
        assertHasPrivateParameterlessCtor(ZoneOffsets.class);
    }

    @Test
    public void create_default_instance_according_to_place() {
        final int currentOffset = getOffsetInSeconds(timeZone);
        final String zoneId = timeZone.getID();
        assertEquals(currentOffset, toZoneOffset(timeZone).getAmountSeconds());
        assertEquals(zoneId, toZoneOffset(timeZone).getId());
    }

    @Test
    public void create_instance_by_hours_offset() {
        final int secondsInTwoHours = secondsInHours(2);
        assertEquals(secondsInTwoHours, ZoneOffsets.ofHours(2)
                                                   .getAmountSeconds());
    }

    @Test
    public void create_instance_by_hours_and_minutes_offset() {
        final int secondsIn8Hours45Minutes = secondsInHoursAndMinutes(8, 45);
        final int secondsInEuclaOffset = ZoneOffsets.ofHoursMinutes(8, 45)
                                                    .getAmountSeconds();
        assertEquals(secondsIn8Hours45Minutes, secondsInEuclaOffset);
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_more_than_14_hours() {
        ZoneOffsets.ofHours(MAX_HOURS_OFFSET + 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_more_than_11_hours_by_abs() {
        ZoneOffsets.ofHours(MIN_HOURS_OFFSET - 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_more_than_60_minutes() {
        ZoneOffsets.ofHoursMinutes(10, MAX_MINUTES_OFFSET + 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_more_than_17_hours_and_60_minutes() {
        ZoneOffsets.ofHoursMinutes(3, MIN_MINUTES_OFFSET - 1);
    }

    private static int secondsInHours(int hours) {
        return SECONDS_PER_MINUTE * MINUTES_PER_HOUR * hours;
    }

    private static int secondsInHoursAndMinutes(int hours, int minutes) {
        return SECONDS_PER_MINUTE * MINUTES_PER_HOUR * hours + SECONDS_PER_MINUTE * minutes;
    }
}
