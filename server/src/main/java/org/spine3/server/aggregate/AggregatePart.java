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

package org.spine3.server.aggregate;

import com.google.protobuf.Message;

/**
 * A part of a larger aggregate.
 *
 * <p>Some business logic objects may be big enough.
 * If not all parts of such a business object need to be preserved at the same
 * time as business logic invariants, such an object can be split into several parts.
 *
 * <p>Each such part would:
 * <ul>
 *     <li>be a class derived from {@code AggregatePart}
 *     <li>have the same aggregate ID as other parts belonging to the same business object
 *     <li>have own state defined as a Protobuf message
 *     <li>managed by a separate repository class derived from {@link AggregateRepository}
 * </ul>
 *
 * <p>In order to access parts of the aggregate {@link AggregateRoot} should be used.
 *
 * <p>If your business logic cannot be split into parts that can be modified separately,
 * consider extending {@link Aggregate} instead of several {@code AggregatePart}s.
 *
 * @param <I> the type for IDs of this class of aggregates
 * @param <S> the type of the state held by the aggregate part
 * @param <B> the type of the aggregate part state builder
 * @author Alexander Yevsyukov
 * @see Aggregate
 */
public abstract class AggregatePart<I, S extends Message, B extends Message.Builder>
                      extends Aggregate<I, S, B> {

    private final AggregateRoot<I> root;

    /**
     * {@inheritDoc}
     */
    protected AggregatePart(I id, AggregateRoot<I> root) {
        super(id);
        this.root = root;
    }

    /**
     * Obtains a state of another {@code AggregatePart} by its class.
     *
     * @param partStateClass the class of the state of the part
     * @param <P>            the type of the part state
     * @return the state of the part or a default state if the state was not found
     * @throws IllegalStateException if a repository was not found,
     *                               or the ID type of the part state does not match
     *                               the ID type of the {@code root}
     */
    protected <P extends Message> P getPartState(
            Class<P> partStateClass) {
        final P partState = root.getPartState(partStateClass);
        return partState;
    }
}
