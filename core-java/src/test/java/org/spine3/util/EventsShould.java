/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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
package org.spine3.util;

import com.google.protobuf.Timestamp;
import org.junit.Test;
import org.spine3.base.CommandId;
import org.spine3.base.EventId;
import org.spine3.base.UserId;

import static com.google.protobuf.util.TimeUtil.getCurrentTime;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.spine3.util.Commands.generateId;
import static org.spine3.util.Identifiers.*;
import static org.spine3.util.Users.newUserId;

/**
 * @author Mikhail Melnik
 */
@SuppressWarnings("InstanceMethodNamingConvention")
public class EventsShould {

    @Test
    public void generate_event_id() {
        final UserId userId = newUserId("events_test");
        final CommandId commandId = Commands.generateId(userId);

        final EventId result = Events.generateId(commandId);

        //noinspection DuplicateStringLiteralInspection
        assertThat(result, allOf(
                hasProperty("commandId", equalTo(commandId)),
                hasProperty("deltaNanos")));
    }

    @Test(expected = NullPointerException.class)
    public void fail_on_attempt_to_generate_null_event_id() {
        //noinspection ConstantConditions
        Events.generateId(null);
    }

    @Test
    public void convert_to_string_event_id_message() {

        final String userIdString = "user123123";
        final Timestamp commandTime = getCurrentTime();
        final long deltaNanos = 256000;

        final CommandId commandId = generateId(userIdString, commandTime);
        final EventId id = EventId.newBuilder().setCommandId(commandId).setDeltaNanos(deltaNanos).build();

        /* TODO:2015-09-21:alexander.litus: create parse() method that would restore an object from its String representation.
           Use the restored object for equality check with the original object.
         */
        final String expected = userIdString + USER_ID_AND_TIME_DELIMITER + timestampToString(commandTime) +
                TIME_DELIMITER + String.valueOf(deltaNanos);

        final String actual = Events.idToString(id);

        assertEquals(expected, actual);
    }
}
