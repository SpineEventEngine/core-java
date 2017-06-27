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

package io.spine.time;

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import org.junit.Assert;
import org.junit.Test;

import java.text.ParseException;
import java.util.TimeZone;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static io.spine.time.Durations2.hours;
import static io.spine.time.Durations2.hoursAndMinutes;
import static io.spine.time.Time.MILLIS_PER_SECOND;
import static io.spine.time.Time.getCurrentTime;
import static io.spine.time.ZoneOffsets.adjustZero;
import static io.spine.time.ZoneOffsets.getDefault;
import static io.spine.time.ZoneOffsets.ofHoursMinutes;
import static io.spine.time.ZoneOffsets.parse;
import static org.junit.Assert.assertEquals;

public class ZoneOffsetsShould {

    @Test
    public void has_private_constructor() {
        assertHasPrivateParameterlessCtor(ZoneOffsets.class);
    }

    @Test
    public void get_current_zone_offset() {
        final TimeZone timeZone = TimeZone.getDefault();
        final ZoneOffset zoneOffset = getDefault();

        final Timestamp now = getCurrentTime();
        final long date = Timestamps.toMillis(now);
        final int offsetSeconds = timeZone.getOffset(date) / MILLIS_PER_SECOND;

        final String zoneId = timeZone.getID();
        assertEquals(zoneId, zoneOffset.getId()
                                       .getValue());

        assertEquals(offsetSeconds, zoneOffset.getAmountSeconds());
    }

    @Test
    public void create_instance_by_hours_offset() {
        final Duration twoHours = hours(2);
        Assert.assertEquals(twoHours.getSeconds(), ZoneOffsets.ofHours(2).getAmountSeconds());
    }

    @Test
    public void create_instance_by_hours_and_minutes_offset() {
        Assert.assertEquals(hoursAndMinutes(8, 45).getSeconds(),
                            ofHoursMinutes(8, 45).getAmountSeconds());

        Assert.assertEquals(hoursAndMinutes(-4, -50).getSeconds(),
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
        ZoneOffsets.ofHours(ZoneOffsets.MAX_HOURS_OFFSET + 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_more_than_11_hours_by_abs() {
        ZoneOffsets.ofHours(ZoneOffsets.MIN_HOURS_OFFSET - 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_more_than_60_minutes() {
        ofHoursMinutes(10, ZoneOffsets.MAX_MINUTES_OFFSET + 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_more_than_17_hours_and_60_minutes() {
        ofHoursMinutes(3, ZoneOffsets.MIN_MINUTES_OFFSET - 1);
    }

    @Test
    public void convert_to_string() throws ParseException {
        final ZoneOffset positive = ofHoursMinutes(5, 48);
        final ZoneOffset negative = ofHoursMinutes(-3, -36);

        Assert.assertEquals(positive, parse(ZoneOffsets.toString(positive)));
        Assert.assertEquals(negative, parse(ZoneOffsets.toString(negative)));
    }

    @Test
    public void parse_string() throws ParseException {
        Assert.assertEquals(ofHoursMinutes(4, 30), parse("+4:30"));
        Assert.assertEquals(ofHoursMinutes(4, 30), parse("+04:30"));

        Assert.assertEquals(ofHoursMinutes(-2, -45), parse("-2:45"));
        Assert.assertEquals(ofHoursMinutes(-2, -45), parse("-02:45"));
    }

    @Test(expected = ParseException.class)
    public void fail_when_sign_char_missing() throws ParseException {
        parse("x03:00");
    }

    @Test(expected = IllegalArgumentException.class)
    public void fail_when_hours_and_minutes_have_different_sign_negative_hours() {
        ofHoursMinutes(-1, 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fail_when_hours_and_minutes_have_different_sign_negative_minutes() {
        ofHoursMinutes(1, -10);
    }

    @Test
    public void adjust_zero_offset_without_zone() {
        Assert.assertEquals(ZoneOffsets.UTC, adjustZero(ZoneOffsets.ofSeconds(0)));

        final ZoneOffset offset = getDefault();
        Assert.assertEquals(offset, adjustZero(offset));

        final ZoneOffset gmtOffset = ZoneConverter.getInstance()
                                                  .convert(TimeZone.getTimeZone("GMT"));
        checkNotNull(gmtOffset);
        Assert.assertEquals(gmtOffset, adjustZero(gmtOffset));
    }
}
