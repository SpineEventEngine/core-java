/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.testing.server.procman;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import io.spine.core.Event;
import io.spine.core.MessageEnvelope;
import io.spine.server.procman.ProcessManager;
import io.spine.testing.server.MessageHandlerTest;
import io.spine.testing.server.expected.CommanderExpected;

import java.util.List;

/**
 * Base class for testing a commander method of a {@code ProcessManager}.
 *
 * @param <I> ID message of the process manager
 * @param <M> type of the command to test
 * @param <S> the process manager state type
 * @param <P> the {@link ProcessManager} type
 *
 * @author Alexander Yevsyukov
 */
public abstract
class PmCommandGenerationTest<I,
                              M extends Message,
                              S extends Message,
                              P extends ProcessManager<I, S, ?>,
                              E extends MessageEnvelope>
        extends MessageHandlerTest<I, M, S, P, CommanderExpected<S>> {

    protected PmCommandGenerationTest(I processManagerId, M message) {
        super(processManagerId, message);
    }

    @Override
    protected List<Event> dispatchTo(P entity) {
        E envelope = createEnvelope();
        List<Event> result = PmDispatcher.dispatch(entity, envelope);
        return result;
    }

    protected abstract E createEnvelope();

    @Override
    protected CommanderExpected<S> expectThat(P processManager) {
        InjectCommandBus.of(boundedContext())
                        .to(processManager);
        S initialState = processManager.getState();
        List<? extends Message> messages = dispatchTo(processManager);
        ImmutableList<Message> commands = interceptedCommands();
        S updatedState = processManager.getState();
        CommanderExpected<S> result =
                new CommanderExpected<>(messages, initialState, updatedState, commands);
        return result;
    }
}
