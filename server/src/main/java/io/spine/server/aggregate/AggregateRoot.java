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

import com.google.protobuf.Message;
import io.spine.server.BoundedContext;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * A root object for a larger aggregate.
 *
 * @param <I> the type for IDs of this class of aggregates
 */
public class AggregateRoot<I> {

    /** The {@code BoundedContext} to which the aggregate belongs. */
    private final BoundedContext boundedContext;

    /** The aggregate ID. */
    private final I id;

    /**
     * Creates a new instance.
     *
     * @param boundedContext the bounded context to which the aggregate belongs
     * @param id             the ID of the aggregate
     */
    protected AggregateRoot(BoundedContext boundedContext, I id) {
        this.boundedContext = checkNotNull(boundedContext);
        this.id = checkNotNull(id);
    }

    /**
     * Obtains the aggregate ID.
     */
    public I id() {
        return this.id;
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
    protected <S extends Message, A extends AggregatePart<I, S, ?, ?>>
    S partState(Class<S> partStateClass) {
        AggregatePartRepository<I, A, ?> repo = repositoryOf(partStateClass);
        AggregatePart<I, S, ?, ?> aggregatePart = repo.loadOrCreate(id());
        S partState = aggregatePart.state();
        return partState;
    }

    /**
     * Obtains a repository for the passed state class.
     *
     * @throws IllegalStateException if a repository was not found,
     *                               or the repository ID type does not match
     *                               the ID type of this {@code AggregateRoot}
     */
    @SuppressWarnings("unchecked") // We ensure ID type when adding to the map.
    private <S extends Message, A extends AggregatePart<I, S, ?, ?>>
    AggregatePartRepository<I, A, ?> repositoryOf(Class<S> stateClass) {
        Class<? extends AggregateRoot<?>> thisType = (Class<? extends AggregateRoot<?>>) getClass();
        Optional<? extends AggregatePartRepository<?, ?, ?>> partRepository = boundedContext
                .aggregateRootDirectory()
                .findPart(thisType, stateClass);
        AggregatePartRepository<?, ?, ?> repository = partRepository.orElseThrow(
                () -> newIllegalStateException("Could not find a repository for aggregate part %s",
                                               stateClass.getName())
        );
        AggregatePartRepository<I, A, ?> result = (AggregatePartRepository<I, A, ?>) repository;
        return result;
    }
}
