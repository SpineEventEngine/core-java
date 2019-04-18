/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server.type;

import io.spine.base.CommandMessage;
import io.spine.base.Identifier;
import io.spine.client.ActorRequestFactory;
import io.spine.core.Command;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.client.command.TestCommandMessage;
import io.spine.type.TypeName;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("CommandEnvelope should")
class CommandEnvelopeTest extends MessageEnvelopeTest<Command, CommandEnvelope, CommandClass> {

    private final TestActorRequestFactory requestFactory =
            new TestActorRequestFactory(CommandEnvelopeTest.class);

    @Override
    protected Command outerObject() {
        return requestFactory.generateCommand();
    }

    @Override
    protected CommandEnvelope toEnvelope(Command obj) {
        return CommandEnvelope.of(obj);
    }

    @Override
    protected CommandClass getMessageClass(Command obj) {
        return CommandClass.of(obj);
    }

    @Test
    @DisplayName("obtain command context")
    void getCommandContext() {
        Command command = outerObject();
        CommandEnvelope envelope = toEnvelope(command);
        assertThat(envelope.context())
                .isEqualTo(command.context());
        assertThat(envelope.context())
                .isSameAs(envelope.context());
    }

    @Test
    @DisplayName("obtain actor context")
    void getActorContext() {
        Command command = outerObject();
        CommandEnvelope envelope = toEnvelope(command);

        assertThat(envelope.actorContext())
                .isEqualTo(command.context()
                                  .getActorContext());
    }

    @Test
    @DisplayName("obtain type of given command")
    void getCommandType() {
        Command command = requestFactory.generateCommand();

        TypeName typeName = CommandEnvelope.of(command)
                                           .messageTypeName();
        assertThat(typeName)
                .isEqualTo(TypeName.of(TestCommandMessage.class));
    }

    @Test
    @DisplayName("obtain type url of given command")
    void getCommandTypeUrl() {
        ActorRequestFactory factory =
                new TestActorRequestFactory(CommandEnvelopeTest.class);
        CommandMessage message = TestCommandMessage
                .newBuilder()
                .setId(Identifier.newUuid())
                .build();
        Command command = factory.command()
                                 .create(message);

        TypeUrl typeUrl = CommandEnvelope.of(command)
                                         .messageTypeName()
                                         .toUrl();

        assertEquals(TypeUrl.of(TestCommandMessage.class), typeUrl);
    }
}
