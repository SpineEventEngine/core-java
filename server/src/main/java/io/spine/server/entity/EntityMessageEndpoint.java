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

package io.spine.server.entity;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.core.ActorMessageEnvelope;
import io.spine.core.TenantId;
import io.spine.server.tenant.TenantAwareFunction0;

import java.util.List;
import java.util.Set;

/**
 * Abstract base for endpoints handling messages sent to entities.
 * 
 * @param <I> the type of entity IDs
 * @param <E> the type of entities
 * @param <M> the type of message envelopes
 * @param <R> the type of the dispatch result, can be {@code <I>} for unicast dispatching, or
 *            {@code Set<I>} for multicast
 * @author Alexander Yevsyukov
 */
@Internal
public abstract class EntityMessageEndpoint<I,
                                            E extends Entity<I, ?>,
                                            M extends ActorMessageEnvelope<?, ?>,
                                            R> {

    /** The repository which created this endpoint. */
    private final Repository<I, E> repository;

    /** The message which needs to handled. */
    private final M envelope;

    protected EntityMessageEndpoint(Repository<I, E> repository, M envelope) {
        this.repository = repository;
        this.envelope = envelope;
    }

    /**
     * Handles the message processing.
     *
     * @return the result of the message processing
     */
    public final R handle() {
        final TenantId tenantId = envelope().getTenantId();
        final TenantAwareFunction0<R> operation = createOperation(tenantId);
        final R result = operation.execute();
        return result;
    }

    /**
     * Obtains IDs of aggregates to which the endpoint delivers the message.
     */
    protected abstract R getTargets();

    /**
     * Creates a tenant-aware operation based on the message this endpoint processes.
     *
     * @param tenantId the ID of the tenant in which context to perform the operation
     */
    private TenantAwareFunction0<R> createOperation(TenantId tenantId) {
        return new Operation(tenantId);
    }

    /**
     * Allows derived classes to handle empty list of uncommitted events returned by
     * the aggregate in response to the message.
     */
    protected abstract void onEmptyResult(E aggregate, M envelope);

    /**
     * Dispatches the message to the entity with the passed ID, providing transactional work
     * and storage of the entity.
     *
     * @param entityId the ID of the entity for which to dispatch the message
     */
    protected abstract void dispatchToOne(I entityId);

    /**
     * Invokes entity-specific method for dispatching the message.
     */
    protected abstract List<? extends Message> dispatchEnvelope(E entity, M envelope);

    /**
     * Processes the exception thrown during dispatching the message.
     */
    protected abstract void onError(M envelope, RuntimeException exception);

    /**
     * Dispatches the message to multiple aggregates.
     *
     * @param targets the set of aggregate IDs to which dispatch the message
     * @return the set of aggregate IDs to which the message was successfully dispatched
     */
    private Set<I> dispatchToMany(Set<I> targets) {
        final ImmutableSet.Builder<I> result = ImmutableSet.builder();
        for (I id : targets) {
            try {
                dispatchToOne(id);
                result.add(id);
            } catch (RuntimeException exception) {
                onError(envelope(), exception);
                // Do not rethrow to allow others to handle.
                // The error is already logged.
            }
        }
        return result.build();
    }

    /**
     * {@linkplain #getTargets() Selects} one or more message targets and
     * {@linkplain #dispatchToOne(I) dispatches} the message to them.
     */
    @SuppressWarnings("unchecked")
    protected final R dispatch() {
        final R targets = getTargets();
        if (targets instanceof Set) {
            final Set<I> handlingAggregates = (Set<I>) targets;
            return (R)(dispatchToMany(handlingAggregates));
        }
        try {
            dispatchToOne((I)targets);
        } catch (RuntimeException exception) {
            onError(envelope(), exception);
        }
        return targets;
    }

    /**
     * Obtains the envelope of the message processed by this endpoint.
     */
    protected final M envelope() {
        return envelope;
    }

    /**
     * Obtains the parent repository of this endpoint.
     */
    protected Repository<I, E> repository() {
        return repository;
    }

    /**
     * The operation executed under the tenant context in which the message was created.
     */
    private class Operation extends TenantAwareFunction0<R> {

        private Operation(TenantId tenantId) {
            super(tenantId);
        }

        @Override
        public R apply() {
            return dispatch();
        }
    }
}
