/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

import io.spine.annotation.SPI;
import io.spine.core.ActorMessageEnvelope;
import io.spine.server.delivery.EndpointDelivery;

/**
 * A strategy on delivering the messages to the instances of a certain process manager type.
 *
 * @param <I> the ID type of process manager, to which messages are being delivered
 * @param <P> the type of process manager
 * @param <M> the type of message envelope, which is used for message delivery
 * @author Alex Tymchenko
 */
@SPI
public abstract class PmEndpointDelivery<I,
                                         P extends ProcessManager<I, ?, ?>,
                                         M extends ActorMessageEnvelope<?, ?, ?>>
        extends EndpointDelivery<I, P, M> {

    protected PmEndpointDelivery(ProcessManagerRepository<I, P, ?> repository) {
        super(repository, repository.processManagerClass());
    }

    @Override
    protected abstract PmEndpoint<I, P, M, ?> getEndpoint(M messageEnvelope);

    @Override
    protected ProcessManagerRepository<I, P, ?> repository() {
        return (ProcessManagerRepository<I, P, ?>) super.repository();
    }

    @Override
    protected void passToEndpoint(I id, M envelopeMessage) {
        getEndpoint(envelopeMessage).deliverNowTo(id);
    }
}
