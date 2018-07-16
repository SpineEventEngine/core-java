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

import com.google.protobuf.Message;
import io.spine.core.Enrichment;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.EventEnvelope;
import io.spine.server.procman.ProcessManager;
import io.spine.testing.server.EventReactionTest;
import io.spine.testing.server.expected.EventHandlerExpected;

import java.util.List;

import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.testing.server.procman.CommandBusInjection.inject;
import static java.util.stream.Collectors.toList;

/**
 * The implementation base for testing a single event reactor in a {@link ProcessManager}.
 *
 * @param <I> ID message of the process manager
 * @param <E> type of the event to test
 * @param <S> the process manager state type
 * @param <P> the {@link ProcessManager} type
 * @author Vladyslav Lubenskyi
 */
public abstract class ProcessManagerEventReactionTest<I,
                                                      E extends Message,
                                                      S extends Message,
                                                      P extends ProcessManager<I, S, ?>>
        extends EventReactionTest<I, E, S, P> {

    @Override
    protected List<? extends Message> dispatchTo(P entity) {
        Event sourceEvent = createEvent(message());
        EventContext context = sourceEvent.getContext()
                                          .toBuilder()
                                          .setEnrichment(enrichment())
                                          .build();
        Event enrichedEvent = sourceEvent.toBuilder()
                                         .setContext(context)
                                         .build();
        EventEnvelope envelope = EventEnvelope.of(enrichedEvent);
        List<Event> events = ProcessManagerDispatcher.dispatch(entity, envelope);

        return events.stream()
                     .map(ProcessManagerEventReactionTest::eventToMessage)
                     .collect(toList());
    }

    private static Message eventToMessage(Event event) {
        return unpack(event.getMessage());
    }

    @Override
    protected EventHandlerExpected<S> expectThat(P entity) {
        inject(entity, boundedContext().getCommandBus());
        return super.expectThat(entity);
    }

    /**
     * Creates an {@link Enrichment} to enrich the tested event.
     */
    protected abstract Enrichment enrichment();
}
