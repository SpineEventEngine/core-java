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
package io.spine.server.delivery;

import io.spine.annotation.Internal;
import io.spine.core.ActorMessageEnvelope;
import io.spine.core.TenantId;
import io.spine.server.entity.Entity;
import io.spine.server.entity.EntityMessageEndpoint;
import io.spine.server.entity.Repository;
import io.spine.server.tenant.TenantAwareOperation;

/**
 * A strategy on delivering the messages to the instances of a certain aggregate type.
 *
 * <p>Allows {@linkplain #shouldPostpone(Object, ActorMessageEnvelope) to postpone} the delivery
 * at runtime for a certain ID and message.
 *
 * <p>The postponed messages are not dispatched to the entity instances automatically. However
 * it is expected they are dispatched manually later via
 * {@linkplain #deliverNow(Object, ActorMessageEnvelope) deliverNow(ID, envelope)} method call.
 *
 * @author Alex Tymchenko
 */
@Internal
public abstract class EndpointDelivery<I,
                                       E extends Entity<I, ?>,
                                       M extends ActorMessageEnvelope<?, ?, ?>>  {

    private final Repository<I, E> repository;

    protected EndpointDelivery(Repository<I, E> repository) {
        this.repository = repository;
    }

    /**
     * Determines whether the given envelope should be automatically dispatched to the instance
     * of a specified ID.
     *
     * @param id       the ID of the entity the envelope is going to be dispatched.
     * @param envelope the envelope to be dispatched — now or later
     * @return {@code true} if the flow to be kept regular and thus the message dispatching
     * to happen immediately, {@code false} otherwise
     */
    public abstract boolean shouldPostpone(I id, M envelope);

    /**
     * Obtains an endpoint to dispatch the given envelope.
     *
     * @param messageEnvelope the envelope to obtain the endpoint for
     * @return the message endpoint
     */
    protected abstract EntityMessageEndpoint<I, E, M, ?> getEndpoint(M messageEnvelope);

    /**
     * Delivers the envelope to the entity of the given ID taking into account
     * the target tenant.
     *
     * <p>Use this method to deliver the previously postponed messages.
     *
     * @param id       an ID of an entity to deliver the envelope to
     * @param envelopeMessage an envelope to deliver
     */
    public void deliverNow(final I id, final M envelopeMessage) {
        final TenantId tenantId = envelopeMessage.getActorContext()
                                                 .getTenantId();
        final TenantAwareOperation operation = new TenantAwareOperation(tenantId) {
            @Override
            public void run() {
                passToEndpoint(id, envelopeMessage);
            }
        };

        operation.run();
    }

    /**
     * Calls the dispatching method of endpoint directly.
     *
     * @param id an ID of an entity to deliver th envelope to
     * @param envelopeMessage an envelope to delivery
     */
    protected abstract void passToEndpoint(I id, M envelopeMessage);

    protected Repository<I, E> repository() {
        return repository;
    }
}
