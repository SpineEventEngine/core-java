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

package io.spine.server.integration;

import io.spine.core.BoundedContextName;
import io.spine.core.Event;
import io.spine.core.EventClass;
import io.spine.protobuf.AnyPacker;
import io.spine.server.integration.route.matcher.ChannelMatcher;
import io.spine.server.integration.route.matcher.MessageTypeMatcher;
import io.spine.test.integration.event.ItgProjectCreated;
import io.spine.test.integration.event.ItgProjectStarted;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Dmitry Ganzha
 */
public class ChannelMatcherShould {

    @Test
    public void return_false_if_message_does_not_match_message_channel_by_message_type() {
        final EventClass eventClass = EventClass.of(ItgProjectStarted.class);
        final ChannelId channelId = Channels.newId(eventClass);
        final ItgProjectCreated itgProjectCreated = ItgProjectCreated.getDefaultInstance();
        final Event projectCreatedEvent = Event.newBuilder()
                                               .setMessage(AnyPacker.pack(itgProjectCreated))
                                               .build();
        final BoundedContextName boundedContextName = BoundedContextName.getDefaultInstance();
        final ExternalMessage notSuitableMessage = ExternalMessages.of(projectCreatedEvent,
                                                                       boundedContextName);
        final ChannelMatcher action = new MessageTypeMatcher();
        final MessageMatched result = action.match(channelId, notSuitableMessage);
        assertFalse(result.getMatched());
        assertFalse(result.getDescription().isEmpty());
    }

    @Test
    public void return_true_if_message_matches_message_channel_by_message_type() {
        final EventClass eventClass = EventClass.of(ItgProjectCreated.class);
        final ChannelId channelId = Channels.newId(eventClass);
        final ItgProjectCreated itgProjectCreated = ItgProjectCreated.getDefaultInstance();
        final Event projectCreatedEvent = Event.newBuilder()
                                               .setMessage(AnyPacker.pack(itgProjectCreated))
                                               .build();
        final BoundedContextName boundedContextName = BoundedContextName.getDefaultInstance();
        final ExternalMessage suitableMessage = ExternalMessages.of(projectCreatedEvent,
                                                                    boundedContextName);
        final ChannelMatcher action = new MessageTypeMatcher();
        final MessageMatched result = action.match(channelId, suitableMessage);
        assertTrue(result.getMatched());
    }
}
