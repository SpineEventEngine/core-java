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
import io.spine.base.EventMessage;
import io.spine.core.Enrichment;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.Events;
import io.spine.server.procman.ProcessManager;
import io.spine.server.type.EventEnvelope;
import io.spine.testing.server.EventReactionTest;
import io.spine.testing.server.expected.EventReactorExpected;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The implementation base for testing a single event reactor in a {@link ProcessManager}.
 *
 * @param <I> ID message of the process manager
 * @param <E> type of the event to test
 * @param <S> the process manager state type
 * @param <P> the {@link ProcessManager} type
 */
public abstract class PmEventReactionTest<I,
                                          E extends EventMessage,
                                          S extends Message,
                                          P extends ProcessManager<I, S, ?>>
        extends EventReactionTest<I, E, S, P> {

    protected PmEventReactionTest(I processManagerId, E eventMessage) {
        super(processManagerId, eventMessage);
    }

    @Override
    protected List<? extends Message> dispatchTo(P entity) {
        EventEnvelope event = createEnriched();
        List<Event> events = PmDispatcher.dispatch(entity, event);
        List<? extends Message> result = Events.toMessages(events);
        return result;
    }

    private EventEnvelope createEnriched() {
        E message = message();
        checkNotNull(message);
        Event sourceEvent = createEvent(message);

        EventContext context = sourceEvent
                .context()
                .toBuilder()
                .setEnrichment(enrichment())
                .build();
        Event enrichedEvent = sourceEvent
                .toBuilder()
                .setContext(context)
                .build();
        return EventEnvelope.of(enrichedEvent);
    }

    @Override
    protected EventReactorExpected<S> expectThat(P entity) {
        InjectCommandBus.of(boundedContext())
                        .to(entity);
        return super.expectThat(entity);
    }

    /**
     * Creates an {@link Enrichment} to enrich the tested event.
     */
    protected abstract Enrichment enrichment();
}
