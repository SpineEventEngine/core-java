/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
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
        UserId userId = UserIds.create("events_test");
        CommandId commandId = Commands.generateId(userId);

        EventId result = Events.generateId(commandId);

        //noinspection DuplicateStringLiteralInspection
        assertThat(result, allOf(
                hasProperty("commandId", equalTo(commandId)),
                hasProperty("timestamp")));
    }

    @Test(expected = NullPointerException.class)
    public void fail_on_attempt_to_generate_null_event_id() {
        //noinspection ConstantConditions
        Events.generateId(null);
    }
}
