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

package io.spine.server.procman;

import com.google.protobuf.Message;
import io.spine.core.Event;
import io.spine.protobuf.AnyPacker;
import io.spine.server.CommandHandlerTest;
import io.spine.server.expected.CommandHandlerExpected;

import java.util.List;

import static io.spine.core.CommandEnvelope.of;
import static io.spine.server.procman.CommandBusInjection.inject;
import static io.spine.server.procman.ProcessManagerDispatcher.dispatch;
import static java.util.stream.Collectors.toList;

/**
 * The implementation base for testing a single command handling in a {@link ProcessManager}.
 *
 * @param <I> ID message of the process manager
 * @param <C> type of the command to test
 * @param <S> the process manager state type
 * @param <P> the {@link ProcessManager} type
 * @author Vladyslav Lubenskyi
 */
public abstract class ProcessManagerCommandTest<I,
                                                C extends Message,
                                                S extends Message,
                                                P extends ProcessManager<I, S, ?>>
        extends CommandHandlerTest<I, C, S, P> {

    @Override
    protected List<? extends Message> dispatchTo(P entity) {
        List<Event> events = dispatch(entity, of(createCommand(message())));
        return events.stream()
                     .map(ProcessManagerCommandTest::eventToMessage)
                     .collect(toList());
    }

    private static Message eventToMessage(Event event) {
        return AnyPacker.unpack(event.getMessage());
    }

    @Override
    protected CommandHandlerExpected<S> expectThat(P entity) {
        inject(entity, boundedContext().getCommandBus());
        return super.expectThat(entity);
    }
}
