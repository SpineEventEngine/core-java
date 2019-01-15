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

package io.spine.testing.server.procman;

import com.google.protobuf.Message;
import io.spine.base.CommandMessage;
import io.spine.core.CommandEnvelope;
import io.spine.core.Event;
import io.spine.core.Events;
import io.spine.server.procman.ProcessManager;
import io.spine.testing.server.CommandHandlerTest;
import io.spine.testing.server.expected.CommandHandlerExpected;

import java.util.List;

import static io.spine.testing.server.procman.PmDispatcher.dispatch;

/**
 * The implementation base for testing a single command handling in a {@link ProcessManager}.
 *
 * @param <I> ID message of the process manager
 * @param <C> type of the command to test
 * @param <S> the process manager state type
 * @param <P> the {@link ProcessManager} type
 */
public abstract class PmCommandTest<I,
                                    C extends CommandMessage,
                                    S extends Message,
                                    P extends ProcessManager<I, S, ?>>
        extends CommandHandlerTest<I, C, S, P> {

    protected PmCommandTest(I processManagerId, C commandMessage) {
        super(processManagerId, commandMessage);
    }

    @Override
    protected List<? extends Message> dispatchTo(P processManager) {
        CommandEnvelope command = createCommand();
        List<Event> events = dispatch(processManager, command);
        return Events.toMessages(events);
    }

    @Override
    public CommandHandlerExpected<S> expectThat(P processManager) {
        InjectCommandBus.of(boundedContext())
                        .to(processManager);
        return super.expectThat(processManager);
    }
}
