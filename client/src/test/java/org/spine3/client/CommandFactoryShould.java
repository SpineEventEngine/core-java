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

package org.spine3.client;

import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.TimeUtil;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.Command;
import org.spine3.base.UserId;
import org.spine3.protobuf.Durations;
import org.spine3.protobuf.Timestamps;
import org.spine3.time.ZoneOffset;
import org.spine3.time.ZoneOffsets;

import static org.junit.Assert.*;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.client.UserUtil.*;

@SuppressWarnings("InstanceMethodNamingConvention")
public class CommandFactoryShould {

    private final UserId actor = newUserId(newUuid());
    private final ZoneOffset zoneOffset = ZoneOffsets.UTC;

    private CommandFactory commandFactory;

    @Before
    public void setUp() {
        commandFactory = CommandFactory.newInstance(actor, zoneOffset);
    }

    @Test
    public void create_instance_by_user_and_timezone() {
        assertEquals(actor, commandFactory.getActor());
        assertEquals(zoneOffset, commandFactory.getZoneOffset());
    }

    @Test
    public void support_moving_between_timezones() {
        final CommandFactory factoryInAnotherTimezone = commandFactory.switchTimezone(ZoneOffsets.ofHours(-8));
        assertNotEquals(commandFactory.getZoneOffset(), factoryInAnotherTimezone.getZoneOffset());
    }

    @Test
    public void create_new_instances_with_current_time() {
        // We are creating a range of +/- second between the call to make sure the timestamp would fit
        // into this range. The purpose of this test is to make sure it works with this precision
        // and to add coverage.
        final Timestamp beforeCall = Timestamps.secondsAgo(1);
        final Command command = commandFactory.create(StringValue.getDefaultInstance());
        final Timestamp afterCall = TimeUtil.add(TimeUtil.getCurrentTime(), Durations.ofSeconds(1));

        assertTrue(Timestamps.isBetween(command.getContext().getTimestamp(), beforeCall, afterCall));
    }
}
