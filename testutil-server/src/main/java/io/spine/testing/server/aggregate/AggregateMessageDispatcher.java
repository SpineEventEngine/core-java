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
package io.spine.testing.server.aggregate;

import com.google.common.annotations.VisibleForTesting;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;
import io.spine.core.CommandEnvelope;
import io.spine.core.EventEnvelope;
import io.spine.core.MessageEnvelope;
import io.spine.core.RejectionEnvelope;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateTransaction;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A test utility to dispatch commands to an {@code Aggregate} in test purposes.
 *
 * @author Alex Tymchenko
 */
@VisibleForTesting
public class AggregateMessageDispatcher {

    /** Prevents instantiation of this utility class. */
    private AggregateMessageDispatcher() {
    }

    /**
     * Dispatches the {@linkplain CommandEnvelope command envelope} and applies the resulting events
     * to the given {@code Aggregate}.
     *
     * @return the list of event messages.
     */
    @CanIgnoreReturnValue
    public static List<? extends Message> dispatchCommand(Aggregate<?, ?, ?> aggregate,
                                                          CommandEnvelope envelope) {
        checkNotNull(envelope);
        List<? extends Message> eventMessages = aggregate.dispatchCommand(envelope);
        apply(aggregate, eventMessages, envelope);
        return eventMessages;
    }

    /**
     * Dispatches the {@linkplain EventEnvelope event envelope} and applies the resulting events
     * to the given {@code Aggregate}.
     *
     * @return the list of event messages.
     */
    @CanIgnoreReturnValue
    public static List<? extends Message> dispatchEvent(Aggregate<?, ?, ?> aggregate,
                                                        EventEnvelope envelope) {
        checkNotNull(envelope);
        List<? extends Message> eventMessages = aggregate.reactOn(envelope);
        apply(aggregate, eventMessages, envelope);
        return eventMessages;
    }

    /**
     * Dispatches the {@linkplain RejectionEnvelope rejection envelope} and applies the
     * resulting events to the given {@code Aggregate}.
     *
     * @return the list of event messages.
     */
    @CanIgnoreReturnValue
    public static List<? extends Message> dispatchRejection(Aggregate<?, ?, ?> aggregate,
                                                            RejectionEnvelope envelope) {
        checkNotNull(envelope);

        List<? extends Message> eventMessages = aggregate.reactOn(envelope);
        apply(aggregate, eventMessages, envelope);
        return eventMessages;
    }

    private static void apply(Aggregate<?, ?, ?> aggregate,
                              List<? extends Message> eventMessages,
                              MessageEnvelope envelope) {
        AggregateTransaction tx = AggregateTransaction.start(aggregate);
        aggregate.apply(eventMessages, envelope);
        tx.commit();
    }
}
