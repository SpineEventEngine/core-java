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
import io.spine.core.EventEnvelope;
import io.spine.core.Events;
import io.spine.server.security.InvocationGuard;

import java.util.List;

import static io.spine.server.aggregate.AggregateCommandEndpointTestSupport.ALLOWED_CALLER_CLASS;
import static java.util.stream.Collectors.toList;

/**
 * A test-only implementation of an {@link AggregateEventReactionEndpoint}, that dispatches
 * events to an instance of {@code Aggregate} into its reactor methods and returns
 * the list of produced events.
 *
 * @param <I> the type of {@code Aggregate} identifier
 * @param <A> the type of {@code Aggregate}
 * @author Alex Tymchenko
 * @apiNote This internal class is designed to be called only from Testutil Server library.
 *          Calling it other code would result in run-time error.
 */
@Internal
@VisibleForTesting
public final class AggregateEventEndpointTestSupport<I, A extends Aggregate<I, ?, ?>>
        extends AggregateEventReactionEndpoint<I, A> {

    private
    AggregateEventEndpointTestSupport(AggregateRepository<I, A> repository, EventEnvelope event) {
        super(repository, event);
    }

    public static <I, A extends Aggregate<I, ?, ?>>
    List<? extends Message>
    dispatch(AggregateRepository<I, A> repository, A aggregate, EventEnvelope event) {
        InvocationGuard.allowOnly(ALLOWED_CALLER_CLASS);
        AggregateEventEndpointTestSupport<I, A> endpoint =
                new AggregateEventEndpointTestSupport<>(repository, event);
        List<? extends Message> result = endpoint.dispatchInTx(aggregate)
                                                 .stream()
                                                 .map(Events::getMessage)
                                                 .collect(toList());
        return result;
    }
}
