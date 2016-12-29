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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.spine3.protobuf.Timestamps.MINUTES_PER_HOUR;
import static org.spine3.protobuf.Timestamps.SECONDS_PER_MINUTE;
import static org.spine3.test.Tests.hasPrivateUtilityConstructor;

@SuppressWarnings("InstanceMethodNamingConvention")
public class ZoneOffsetsShould {

    @Test
    public void has_private_constructor() {
        assertTrue(hasPrivateUtilityConstructor(ZoneOffsets.class));
    }

    @Test
    public void have_private_utility_constructor() {
        assertTrue(hasPrivateUtilityConstructor(ZoneOffsets.class));
    }

    @Test
    public void create_instance_by_hours_offset() {
        final int secondsInTwoHours = SECONDS_PER_MINUTE * MINUTES_PER_HOUR * 2;
        assertEquals(secondsInTwoHours, ZoneOffsets.ofHours(2)
                                                   .getAmountSeconds());
    }

    @Test
    public void create_instance_by_hours_and_minutes_offset() {
        final int secondsIn8Hours45Minutes = SECONDS_PER_MINUTE * MINUTES_PER_HOUR * 8
                + SECONDS_PER_MINUTE * 45;
        final int secondsInEuclaOffset = ZoneOffsets.ofHoursMinutes(8, 45)
                                                    .getAmountSeconds();
        assertEquals(secondsIn8Hours45Minutes, secondsInEuclaOffset);
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_more_than_18_hours() {
        ZoneOffsets.ofHours(15);
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_more_than_18_hours_by_abs() {
        ZoneOffsets.ofHours(-12);
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_more_than_60_minutes() {
        ZoneOffsets.ofHoursMinutes(10, 61);
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_more_than_17_hours_and_60_minutes() {
        ZoneOffsets.ofHoursMinutes(18, 30);
    }

}
