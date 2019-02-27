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

package io.spine.testing.server.aggregate;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;
import io.spine.base.EventMessage;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.type.EventEnvelope;
import io.spine.testing.server.MessageHandlerTest;
import io.spine.testing.server.TestEventFactory;
import io.spine.testing.server.expected.EventApplierExpected;

import java.util.List;

import static io.spine.testing.server.aggregate.AggregateMessageDispatcher.importEvent;
import static java.util.Collections.emptyList;

/**
 * The implementation base for testing a single event import in an {@link Aggregate}.
 *
 * @param <I>
 *         ID message of the aggregate
 * @param <E>
 *         type of the event to test
 * @param <S>
 *         the aggregate state type
 * @param <A>
 *         the {@link Aggregate} type
 * @see io.spine.server.aggregate.Apply#allowImport()
 */
public abstract class AggregateEventImportTest<I,
                                               E extends EventMessage,
                                               S extends Message,
                                               A extends Aggregate<I, S, ?>>
        extends MessageHandlerTest<I, E, S, A, EventApplierExpected<S>> {

    private final TestEventFactory eventFactory = TestEventFactory.newInstance(getClass());

    protected AggregateEventImportTest(I aggregateId, E eventMessage) {
        super(aggregateId, eventMessage);
    }

    @CanIgnoreReturnValue
    @Override
    protected List<? extends Message> dispatchTo(A aggregate) {
        Event event = createEvent(message());
        EventEnvelope envelope = EventEnvelope.of(event);
        importEvent(aggregate, envelope);
        return emptyList();
    }

    @Override
    protected EventApplierExpected<S> expectThat(A entity) {
        S initialState = entity.state();
        dispatchTo(entity);
        return new EventApplierExpected<>(initialState, entity.state());
    }

    /**
     * Creates {@link Event} from the given message and supplies it with {@link EventContext}.
     *
     * @param message
     *         an event message
     * @return a new {@link Event}
     */
    protected final Event createEvent(E message) {
        Event event = eventFactory.createEvent(message);
        EventContext context = event.getContext()
                                    .toBuilder()
                                    .setExternal(externalMessage())
                                    .build();
        return event.toBuilder()
                    .setContext(context)
                    .build();
    }

    /**
     * Indicates if the tested event is external to the {@link io.spine.server.BoundedContext}
     * in which the rejection is being processed.
     *
     * @return {@code true} if the event is external, {@code false} otherwise
     */
    protected boolean externalMessage() {
        return false;
    }
}
