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

package io.spine.server.command;

import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.base.CommandContext;
import io.spine.base.CommandId;
import io.spine.base.Commands;
import io.spine.client.ActorRequestFactory;
import io.spine.test.TestActorRequestFactory;
import io.spine.test.Tests;
import io.spine.test.command.event.MandatoryFieldEvent;
import io.spine.validate.ValidationException;
import org.junit.Before;
import org.junit.Test;

import static io.spine.test.Tests.newUuidValue;

/**
 * @author Alexander Yevsyukov
 */
public class EventFactoryShould {

    private final ActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(getClass());

    private Message producerId;
    private CommandContext commandContext;
    private CommandId commandId;

    @Before
    public void setUp() {
        commandContext = requestFactory.command()
                                       .create(Empty.getDefaultInstance())
                                       .getContext();
        producerId = newUuidValue();
        commandId = Commands.generateId();
    }

    @Test(expected = NullPointerException.class)
    public void require_producer_id_in_builder() {
        EventFactory.newBuilder()
                    .setCommandContext(commandContext)
                    .setCommandId(commandId)
                    .build();
    }

    @Test(expected = NullPointerException.class)
    public void require_non_null_command_context_in_builder() {
        EventFactory.newBuilder()
                    .setProducerId(producerId)
                    .setCommandId(commandId)
                    .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void require_non_default_command_context_in_builder() {
        EventFactory.newBuilder()
                    .setProducerId(producerId)
                    .setCommandId(commandId)
                    .setCommandContext(CommandContext.getDefaultInstance())
                    .build();
    }

    @Test(expected = NullPointerException.class)
    public void require_non_null_command_id() {
        EventFactory.newBuilder().setCommandId(Tests.<CommandId>nullRef());
    }
    
    @Test(expected = NullPointerException.class)
    public void require_set_command_id() {
        EventFactory.newBuilder()
                    .setProducerId(producerId)
                    .setCommandContext(commandContext)
                    .build();
    }

    @Test(expected = ValidationException.class)
    public void validate_event_messages_before_creation() {
        final EventFactory factory = EventFactory.newBuilder()
                                               .setCommandContext(commandContext)
                                               .setCommandId(commandId)
                                               .setProducerId(producerId)
                                               .build();
        factory.createEvent(MandatoryFieldEvent.getDefaultInstance(), null);
    }
}
