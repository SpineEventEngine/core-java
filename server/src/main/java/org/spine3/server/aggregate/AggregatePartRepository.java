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

import org.spine3.server.BoundedContext;

import java.lang.reflect.Constructor;

import static org.spine3.server.aggregate.Aggregates.createAggregatePartEntity;
import static org.spine3.server.aggregate.Aggregates.createAggregateRoot;

/**
 * Common abstract base for repositories that manage {@code AggregatePart}s.
 *
 * @author Alexander Yevsyukov
 */
public abstract class AggregatePartRepository
        <I, A extends AggregatePart<I, ?, ?>, R extends AggregateRoot<I>>
        extends AggregateRepository<I, A> {

    /**
     * The class of the {@code AggregateRoot}, which instance will be injected
     * into the {@code AggregatePart} instance during initialization.
     */
    private final Class<R> rootClass;

    /**
     * {@inheritDoc}
     */
    protected AggregatePartRepository(BoundedContext boundedContext, Class<R> rootClass) {
        super(boundedContext);
        this.rootClass = rootClass;
    }

    /**
     * {@inheritDoc}
     */
    @Override // to expose this method in the same package.
    protected Class<I> getIdClass() {
        return super.getIdClass();
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod") // The call of the super method is not needed.
    @Override
    public A create(I id) {
        final Constructor<A> entityConstructor = getEntityConstructor();
        final AggregateRoot<I> root = createAggregateRoot(id, getBoundedContext(), rootClass);
        final A result = createAggregatePartEntity(entityConstructor, id, root);
        return result;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod") // The call of the super method is not needed.
    @Override
    protected Constructor<A> getEntityConstructor() {
        final Constructor<A> result =
                Aggregates.getAggregatePartConstructor(getEntityClass(), rootClass, getIdClass());
        return result;
    }
}
