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

import com.google.protobuf.Message;
import io.spine.base.EventMessage;
import io.spine.core.Event;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.type.EventEnvelope;
import io.spine.testing.server.EventReactionTest;

import java.util.List;

import static io.spine.testing.server.aggregate.AggregateMessageDispatcher.dispatchEvent;

/**
 * The implementation base for testing a single event reactor in an {@link Aggregate}.
 *
 * @param <I>
 *         ID message of the aggregate
 * @param <E>
 *         type of the event to test
 * @param <S>
 *         the aggregate state type
 * @param <A>
 *         the {@link Aggregate} type
 * @author Dmytro Dashenkov
 */
public abstract class AggregateEventReactionTest<I,
                                                 E extends EventMessage,
                                                 S extends Message,
                                                 A extends Aggregate<I, S, ?>>
        extends EventReactionTest<I, E, S, A> {

    protected AggregateEventReactionTest(I aggregateId, E eventMessage) {
        super(aggregateId, eventMessage);
    }

    @Override
    protected List<? extends Message> dispatchTo(A aggregate) {
        Event event = createEvent(message());
        EventEnvelope envelope = EventEnvelope.of(event);
        return dispatchEvent(aggregate, envelope);
    }
}
