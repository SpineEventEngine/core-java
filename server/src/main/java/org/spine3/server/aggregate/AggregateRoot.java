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

import com.google.common.collect.Maps;
import com.google.protobuf.Message;
import org.spine3.server.BoundedContext;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A root object for a larger aggregate.
 *
 * @param <I> the type for IDs of this class of aggregates
 * @author Alexander Yevsyukov
 */
public class AggregateRoot<I> {

    /** The bounded context to which the aggregate belongs. */
    private final BoundedContext boundedContext;

    /** The map from a part class to a repository which manages corresponding aggregate part*/
    private final Map<Class<? extends Message>, AggregatePartRepository<I, ?>> partsAccess = Maps.newHashMap();

    /** The aggregate ID. */
    private final I id;

    /**
     * Creates an new instance.
     *
     * @param boundedContext the bounded context to which the aggregate belongs
     * @param id the ID of the aggregate
     */
    protected AggregateRoot(BoundedContext boundedContext, I id) {
        checkNotNull(boundedContext);
        checkNotNull(id);
        this.boundedContext = boundedContext;
        this.id = checkNotNull(id);
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
    protected BoundedContext getBoundedContext() {
        return boundedContext;
    }

    /**
     * Obtains a part state by its class.
     *
     * @param partClass the class of the part
     * @param <S> the type of the part state
     * @return the state of the part or a default state if the state was not found
     */
    protected <S extends Message> S getPart(Class<S> partClass) {
        final AggregatePartRepository<I, ?> repo = getRepository(partClass);
        //noinspection TestOnlyProblems
        final AggregatePart<I, ?, ?> aggregatePart = repo.loadOrCreate(getId());
        @SuppressWarnings("unchecked") // We ensure type when we cache the repository.
        final S partState = (S) aggregatePart.getState();
        return partState;
    }

    /**
     * Obtains a repository for the passed state class.
     *
     * @throws IllegalStateException if a repository was not found
     *                               or the repository does not match expectations of this {@code AggregateRoot}
     */
    @SuppressWarnings("MethodWithMoreThanThreeNegations") // OK as all false branches are exits from the method.
    private <S extends Message> AggregatePartRepository<I, ?> getRepository(Class<S> stateClass) {
        final AggregatePartRepository<I, ?> cached = partsAccess.get(stateClass);

        if (cached != null) {
            return cached;
        }

        // We don't have cached instance. Obtain from the `BoundedContext` and cache.
        final AggregatePartRepositoryLookup<I, S> lookup = new AggregatePartRepositoryLookup<>(getBoundedContext(), stateClass);
        final AggregatePartRepository<I, ?> repo = lookup.find();

        partsAccess.put(stateClass, repo);

        return repo;
    }
}
