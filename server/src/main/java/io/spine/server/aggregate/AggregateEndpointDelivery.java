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

import io.spine.annotation.SPI;
import io.spine.core.ActorMessageEnvelope;
import io.spine.server.delivery.EndpointDelivery;

/**
 * A strategy on delivering the messages to the instances of a certain aggregate type.
 *
 * @author Alex Tymchenko
 */
@SPI
public abstract class AggregateEndpointDelivery<I,
                                                A extends Aggregate<I, ?, ?>,
                                                E extends ActorMessageEnvelope<?, ?, ?>>
        extends EndpointDelivery<I, A, E> {

    AggregateEndpointDelivery(AggregateRepository<I, A> repository) {
        super(repository);
    }

    @Override
    protected abstract AggregateMessageEndpoint<I, A, E, ?> getEndpoint(E messageEnvelope);

    @Override
    protected AggregateRepository<I, A> repository() {
        return (AggregateRepository<I, A>) super.repository();
    }

    @Override
    protected void passToEndpoint(I id, E envelopeMessage) {
        getEndpoint(envelopeMessage).deliverNowTo(id);
    }
}
