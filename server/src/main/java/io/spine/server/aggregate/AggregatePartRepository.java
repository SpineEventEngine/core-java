/*
 * Copyright 2019, TeamDev. All rights reserved.
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

import io.spine.annotation.Internal;
import io.spine.server.aggregate.model.AggregatePartClass;

import static io.spine.server.aggregate.model.AggregatePartClass.asAggregatePartClass;

/**
 * Common abstract base for repositories that manage {@code AggregatePart}s.
 *
 * @param <I>
 *         the type of part identifiers
 * @param <A>
 *         the type of aggregate parts
 * @param <R>
 *         the type of the aggregate root associated with the type of parts
 */
public abstract class AggregatePartRepository<I,
                                              A extends AggregatePart<I, ?, ?, R>,
                                              R extends AggregateRoot<I>>
                      extends AggregateRepository<I, A> {

    /**
     * Creates a new instance.
     */
    protected AggregatePartRepository() {
        super();
    }

    @Override
    public void onRegistered() {
        super.onRegistered();
        boundedContext().aggregateRootDirectory()
                        .register(this);
    }

    @Override
    public A create(I id) {
        AggregateRoot<I> root = createAggregateRoot(id);
        A result = createAggregatePart(root);
        return result;
    }

    @Internal
    @Override
    protected final AggregatePartClass<A> toModelClass(Class<A> cls) {
        return asAggregatePartClass(cls);
    }

    AggregatePartClass<A> aggregatePartClass() {
        return (AggregatePartClass<A>) entityModelClass();
    }

    //TODO:2017-06-06:alexander.yevsyukov: Cache aggregate roots shared among part repositories
    private AggregateRoot<I> createAggregateRoot(I id) {
        AggregateRoot<I> result = aggregatePartClass().createRoot(boundedContext(), id);
        return result;
    }

    private A createAggregatePart(AggregateRoot<I> root) {
        return aggregatePartClass().create(root);
    }
}
