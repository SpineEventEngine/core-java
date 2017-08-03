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
package io.spine.server.aggregate;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Message;
import io.spine.core.CommandEnvelope;
import io.spine.core.EventEnvelope;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A test utility to dispatch commands to an {@code Aggregate} in test purposes.
 *
 * @author Alex Tymchenko
 */
@VisibleForTesting
public class AggregateMessageDispatcher {

    private AggregateMessageDispatcher() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Dispatches the {@linkplain CommandEnvelope command envelope} and applies the resulting events
     * to the given {@code Aggregate}.
     *
     * @return the list of {@code Event} messages.
     */
    public static List<? extends Message> dispatchCommand(Aggregate<?, ?, ?> aggregate,
                                                          CommandEnvelope envelope) {
        checkNotNull(aggregate);
        checkNotNull(envelope);

        final List<? extends Message> eventMessages = aggregate.dispatchCommand(envelope);

        final AggregateTransaction tx = AggregateTransaction.start(aggregate);
        aggregate.apply(eventMessages, envelope);
        tx.commit();

        return eventMessages;
    }

    /**
     * Dispatches the {@linkplain EventEnvelope event envelope} and applies the resulting events
     * to the given {@code Aggregate}.
     *
     * @return the list of {@code Event} messages.
     */
    public static List<? extends Message> dispatchEvent(Aggregate<?, ?, ?> aggregate,
                                                        EventEnvelope envelope) {
        checkNotNull(aggregate);
        checkNotNull(envelope);

        final List<? extends Message> eventMessages = aggregate.reactOn(envelope);

        final AggregateTransaction tx = AggregateTransaction.start(aggregate);
        aggregate.apply(eventMessages, envelope);
        tx.commit();

        return eventMessages;
    }
}
