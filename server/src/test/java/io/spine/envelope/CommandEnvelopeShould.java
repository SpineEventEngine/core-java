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

package io.spine.envelope;

import io.spine.client.TestActorRequestFactory;
import io.spine.core.Command;
import io.spine.core.CommandClass;
import io.spine.core.CommandEnvelope;
import io.spine.core.MessageEnvelopeShould;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * @author Alexander Yevsyukov
 */
public class CommandEnvelopeShould
        extends MessageEnvelopeShould<Command, CommandEnvelope, CommandClass> {

    private final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(CommandEnvelopeShould.class);

    @Test
    public void obtain_command_context() {
        final Command command = outerObject();
        final CommandEnvelope envelope = toEnvelope(command);
        assertEquals(command.getContext(), envelope.getCommandContext());
        assertSame(envelope.getCommandContext(), envelope.getMessageContext());
    }

    @Test
    public void obtain_actor_context() {
        final Command command = outerObject();
        final CommandEnvelope envelope = toEnvelope(command);

        assertEquals(command.getContext()
                            .getActorContext(), envelope.getActorContext());
    }

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
}
