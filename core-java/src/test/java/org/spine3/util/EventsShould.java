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

import org.junit.Test;
import org.spine3.base.CommandId;
import org.spine3.base.EventId;
import org.spine3.base.UserId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * @author Mikhail Melnik
 */
@SuppressWarnings("InstanceMethodNamingConvention")
public class EventsShould {

    @Test
    public void generate_event_id() {
        UserId userId = Users.createId("events_test");
        CommandId commandId = Commands.generateId(userId);

        EventId result = Events.generateId(commandId);

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
}
