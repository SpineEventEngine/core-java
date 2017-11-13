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

import com.google.common.testing.NullPointerTester;
import io.spine.core.Event;
import io.spine.core.EventClass;
import io.spine.type.TypeUrl;
import org.junit.Test;

import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Dmitry Ganzha
 */
public class ChannelsShould {

    @Test
    public void have_private_constructor() {
        assertHasPrivateParameterlessCtor(Channels.class);
    }

    @Test
    public void construct_channel_id_correctly_for_dead_message_channel() {
        final String deadMessageChannelName = "test";
        final ChannelId channelId = Channels.newDeadMessageId(deadMessageChannelName);
        assertTrue(channelId.getMessageTypeUrl()
                            .isEmpty());
        assertFalse(channelId.getDeadMessage()
                             .isEmpty());
    }

    @Test
    public void construct_channel_id_correctly_for_message_class() {
        final EventClass eventClass = EventClass.of(Event.class);
        final ChannelId channelId = Channels.newId(eventClass);

        final String expectedMessageTypeUrl = TypeUrl.of(eventClass.value())
                                                     .value();
        final String actualMessageTypeUrl = channelId.getMessageTypeUrl();

        assertEquals(expectedMessageTypeUrl, actualMessageTypeUrl);
    }

    @Test
    public void return_same_channel_id_for_same_message_class() {
        final EventClass eventClass = EventClass.of(Event.class);
        final ChannelId firstChannelId = Channels.newId(eventClass);
        final ChannelId secondChannelId = Channels.newId(eventClass);
        assertEquals(firstChannelId, secondChannelId);
    }

    @Test
    public void pass_the_null_tolerance_check() {
        new NullPointerTester().testAllPublicStaticMethods(Channels.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_empty_dead_channel_name() {
        Channels.newDeadMessageId("");
    }
}
