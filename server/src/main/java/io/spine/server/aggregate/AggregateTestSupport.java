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

package io.spine.server.aggregate;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.core.CommandEnvelope;
import io.spine.core.EventEnvelope;
import io.spine.core.Events;
import io.spine.core.MessageEnvelope;
import io.spine.server.security.InvocationGuard;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;

/**
 * Internal utility class for assisting in aggregate tests.
 *
 * @author Alex Tymchenko
 * @author Alexander Yevsyukov
 * @apiNote This internal class is designed to be called only from Testutil Server library.
 *          Calling it other code would result in run-time error.
 */
@Internal
@VisibleForTesting
public final class AggregateTestSupport {

    private static final String ALLOWED_CALLER_CLASS =
            "io.spine.testing.server.aggregate.AggregateMessageDispatcher";

    /** Prevents instantiation of this utility class. */
    private AggregateTestSupport() {
    }

    /**
     * Dispatches a command to an instance of an {@code Aggregate}.
     *
     * @param <I> the type of {@code Aggregate} identifier
     * @param <A> the type of {@code Aggregate}
     * @return the list of produced event messages
     */
    public static <I, A extends Aggregate<I, ?, ?>>
    List<? extends Message>
    dispatchCommand(AggregateRepository<I, A> repository, A aggregate, CommandEnvelope command) {
        checkArguments(repository, aggregate, command);
        InvocationGuard.allowOnly(ALLOWED_CALLER_CLASS);
        return dispatchAndCollect(
                new AggregateCommandEndpoint<>(repository, command),
                aggregate
        );
    }

    /**
     * Dispatches an event to an instance of {@code Aggregate} into its reactor methods.
     *
     * @param <I> the type of {@code Aggregate} identifier
     * @param <A> the type of {@code Aggregate}
     * @return the list of produced event messages
     */
    public static <I, A extends Aggregate<I, ?, ?>>
    List<? extends Message>
    dispatchEvent(AggregateRepository<I, A> repository, A aggregate, EventEnvelope event) {
        checkArguments(repository, aggregate, event);
        InvocationGuard.allowOnly(ALLOWED_CALLER_CLASS);
        return dispatchAndCollect(
                new AggregateEventReactionEndpoint<>(repository, event),
                aggregate
        );
    }

    /**
     * Imports an event to an instance of {@code Aggregate} into the applier method annotated
     * as {@code allowImport = true}.
     *
     * @param <I> the type of {@code Aggregate} identifier
     * @param <A> the type of {@code Aggregate}
     */
    public static <I, A extends Aggregate<I, ?, ?>>
    void importEvent(AggregateRepository<I, A> repository, A aggregate, EventEnvelope event) {
        checkArguments(repository, aggregate, event);
        InvocationGuard.allowOnly(ALLOWED_CALLER_CLASS);
        EventImportEndpoint<I, A> endpoint = new EventImportEndpoint<>(repository, event);
        endpoint.dispatchInTx(aggregate);
    }

    private static <I, A extends Aggregate<I, ?, ?>> List<Message>
    dispatchAndCollect(AggregateEndpoint<I, A, ?> endpoint, A aggregate) {
        List<Message> result =
                endpoint.dispatchInTx(aggregate)
                        .stream()
                        .map(Events::getMessage)
                        .collect(toList());
        return result;
    }

    private static <I, A extends Aggregate<I, ?, ?>> void
    checkArguments(AggregateRepository<I, A> repository, A aggregate, MessageEnvelope envelope) {
        checkNotNull(repository);
        checkNotNull(aggregate);
        checkNotNull(envelope);
    }
}
