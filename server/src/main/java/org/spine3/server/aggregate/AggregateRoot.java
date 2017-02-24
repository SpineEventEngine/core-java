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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.protobuf.Message;
import org.spine3.server.BoundedContext;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.server.aggregate.AggregatePartRepositoryLookup.createLookup;

/**
 * A root object for a larger aggregate.
 *
 * @param <I> the type for IDs of this class of aggregates
 * @author Alexander Yevsyukov
 */
public class AggregateRoot<I> {

    /** The map from a part class to a repository which manages corresponding aggregate part */
    private static final Map<Class<? extends Message>,
                             AggregatePartRepository<?, ?, ?>> partsAccess =
                                                               Maps.newConcurrentMap();

    /** The bounded context to which the aggregate belongs. */
    private final BoundedContext boundedContext;

    /** The aggregate ID. */
    private final I id;

    /**
     * Creates an new instance.
     *
     * @param boundedContext the bounded context to which the aggregate belongs
     * @param id             the ID of the aggregate
     */
    protected AggregateRoot(BoundedContext boundedContext, I id) {
        checkNotNull(boundedContext);
        checkNotNull(id);
        this.boundedContext = boundedContext;
        this.id = id;
    }

    /**
     * Obtains the aggregate ID.
     */
    public I getId() {
        return this.id;
    }

    /**
     * Obtains the {@code BoundedContext} to which the aggregate belongs.
     */
    private BoundedContext getBoundedContext() {
        return boundedContext;
    }

    /**
     * Obtains a part state by its class.
     *
     * @param partStateClass the class of the state of the part
     * @param <S>            the type of the part state
     * @return the state of the part or a default state if the state was not found
     * @throws IllegalStateException if a repository was not found,
     *                               or the ID type of the part state does not match
     *                               the ID type of this {@code AggregateRoot}
     */
    protected <S extends Message, A extends AggregatePart<I, S, ?, ?>> S
    getPartState(Class<S> partStateClass) {
        final AggregatePartRepository<I, A, ?> repo = getRepository(partStateClass);
        final AggregatePart<I, S, ?, ?> aggregatePart = repo.loadOrCreate(getId());
        final S partState = aggregatePart.getState();
        return partState;
    }

    /**
     * Obtains a repository for the passed state class.
     *
     * @throws IllegalStateException if a repository was not found,
     *                               or the repository ID type does not match
     *                               the ID type of this {@code AggregateRoot}
     */
    private <S extends Message, A extends AggregatePart<I, S, ?, ?>>
    AggregatePartRepository<I, A, ?> getRepository(Class<S> stateClass) {
        @SuppressWarnings("unchecked") // We ensure ID type when adding to the map.
        final AggregatePartRepository<I, A, ?> cached =
                (AggregatePartRepository<I, A, ?>) partsAccess.get(stateClass);

        if (cached != null) {
            return cached;
        }

        // We don't have a cached instance. Obtain from the `BoundedContext` and cache.
        final AggregatePartRepository<I, A, ?> repo = lookup(stateClass);

        partsAccess.put(stateClass, repo);

        return repo;
    }

    @VisibleForTesting
    <S extends Message, A extends AggregatePart<I, S, ?, ?>>
    AggregatePartRepository<I, A, ?> lookup(Class<S> stateClass) {
        @SuppressWarnings("unchecked") // The type is ensured by getId() result.
        final Class<I> idClass = (Class<I>) getId().getClass();
        final AggregatePartRepositoryLookup<I, S> lookup = createLookup(getBoundedContext(),
                                                                        idClass,
                                                                        stateClass);
        final AggregatePartRepository<I, A, ?> result = lookup.find();
        return result;
    }
}
