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

import com.google.protobuf.Duration;
import org.junit.Test;
import org.spine3.base.StringifierRegistry;
import org.spine3.protobuf.Durations2;

import java.text.ParseException;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;
import static org.spine3.time.ZoneOffsets.MAX_HOURS_OFFSET;
import static org.spine3.time.ZoneOffsets.MAX_MINUTES_OFFSET;
import static org.spine3.time.ZoneOffsets.MIN_HOURS_OFFSET;
import static org.spine3.time.ZoneOffsets.MIN_MINUTES_OFFSET;
import static org.spine3.time.ZoneOffsets.getOffsetInSeconds;
import static org.spine3.time.ZoneOffsets.ofHours;
import static org.spine3.time.ZoneOffsets.ofHoursMinutes;
import static org.spine3.time.ZoneOffsets.parse;
import static org.spine3.time.ZoneOffsets.toZoneOffset;

public class ZoneOffsetsShould {

    private static final TimeZone timeZone = TimeZone.getDefault();

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
        final Duration twoHours = Durations2.hours(2);
        assertEquals(twoHours.getSeconds(), ofHours(2).getAmountSeconds());
    }

    @Test
    public void create_instance_by_hours_and_minutes_offset() {
        assertEquals(Durations2.hoursAndMinutes(8, 45)
                               .getSeconds(),
                     ofHoursMinutes(8, 45).getAmountSeconds());

        assertEquals(Durations2.hoursAndMinutes(-4, -50).getSeconds(),
                     ofHoursMinutes(-4, -50).getAmountSeconds());
    }

    @Test(expected = IllegalArgumentException.class)
    public void require_same_sign_for_hours_and_minutes_negative_hours() {
        ofHoursMinutes(-1, 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void require_same_sign_for_hours_and_minutes_positive_hours() {
        ofHoursMinutes(1, -10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_more_than_14_hours() {
        ofHours(MAX_HOURS_OFFSET + 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_more_than_11_hours_by_abs() {
        ofHours(MIN_HOURS_OFFSET - 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_more_than_60_minutes() {
        ofHoursMinutes(10, MAX_MINUTES_OFFSET + 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_more_than_17_hours_and_60_minutes() {
        ofHoursMinutes(3, MIN_MINUTES_OFFSET - 1);
    }

    @Test
    public void convert_to_string() throws ParseException {
        final ZoneOffset positive = ofHoursMinutes(5, 48);
        final ZoneOffset negative = ofHoursMinutes(-3, -36);

        assertEquals(positive, parse(ZoneOffsets.toString(positive)));
        assertEquals(negative, parse(ZoneOffsets.toString(negative)));
    }

    @Test
    public void parse_string() throws ParseException {
        assertEquals(ofHoursMinutes(4, 30), parse("+4:30"));
        assertEquals(ofHoursMinutes(4, 30), parse("+04:30"));

        assertEquals(ofHoursMinutes(-2, -45), parse("-2:45"));
        assertEquals(ofHoursMinutes(-2, -45), parse("-02:45"));
    }

    @Test
    public void register_stringifier() {
        assertFalse(StringifierRegistry.getInstance()
                                       .get(ZoneOffset.class)
                                       .isPresent());

        ZoneOffsets.registerStringifier();

        assertEquals(ZoneOffsets.stringifier(), StringifierRegistry.getInstance()
                                                                   .get(ZoneOffset.class)
                                                                   .get());
    }
}
