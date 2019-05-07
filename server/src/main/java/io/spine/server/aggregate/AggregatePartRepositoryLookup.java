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
import io.spine.server.entity.Repository;

import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * A helper class for finding and checking {@code AggregatePartRepository}.
 *
 * <p>This class is used by {@link AggregateRoot} to find repositories for its parts.
 *
 * @param <I>
 *         the type of the IDs of the repository to find
 * @param <S>
 *         the type of the state of aggregate parts of managed by the target repository
 */
final class AggregatePartRepositoryLookup<I, S extends Message> {

    private final BoundedContext boundedContext;
    private final Class<I> idClass;
    private final Class<S> stateClass;

    /**
     * Creates the lookup object for finding the repository that
     * manages aggregate parts with the passed state class.
     */
    static <I, S extends Message> AggregatePartRepositoryLookup<I, S>
    createLookup(BoundedContext boundedContext, Class<I> idClass, Class<S> stateClass) {
        return new AggregatePartRepositoryLookup<>(boundedContext, idClass, stateClass);
    }

    private AggregatePartRepositoryLookup(BoundedContext boundedContext,
                                          Class<I> idClass,
                                          Class<S> stateClass) {
        this.boundedContext = boundedContext;
        this.idClass = idClass;
        this.stateClass = stateClass;
    }

    /**
     * Find the repository in the bounded context.
     *
     * @throws IllegalStateException if the repository was not found, or
     *                               if not of the expected type, or
     *                               IDs are not of the expected type
     */
    <A extends AggregatePart<I, S, ?, ?>> AggregatePartRepository<I, A, ?> find() {
        Repository genericRepo = boundedContext
                .findRepository(stateClass)
                .orElseThrow(() -> newIllegalStateException(
                        "Unable to find a repository for aggregate part `%s`",
                        stateClass.getName()
                ));
        checkIsAggregatePartRepository(genericRepo);
        AggregatePartRepository<?, ?, ?> genericPartRepo =
                (AggregatePartRepository<?, ?, ?>) genericRepo;
        AggregatePartRepository<I, A, ?> repository = checkIdClass(genericPartRepo);
        return repository;
    }

    /**
     * Ensures that the passed repository is an instance of {@code AggregatePartRepository}.
     *
     * <p>We check this to make sure that expectations of this {@code AggregateRoot} are supported
     * by correct configuration of the {@code BoundedContext}.
     */
    private static void checkIsAggregatePartRepository(Repository repo) {
        if (!(repo instanceof AggregatePartRepository)) {
            throw newIllegalStateException("The repository `%s` is not an instance of `%s`",
                                           repo,
                                           AggregatePartRepository.class.getName());
        }
    }

    /**
     * Ensures the type of the IDs of the passed repository.
     */
    private <A extends AggregatePart<I, S, ?, ?>>
    AggregatePartRepository<I, A, ?> checkIdClass(AggregatePartRepository<?, ?, ?> repo) {
        Class<?> repoIdClass = repo.idClass();
        if (!idClass.equals(repoIdClass)) {
            throw newIllegalStateException("The ID class of the aggregate part repository (%s) " +
                                           "does not match the ID class of the AggregateRoot (%s)",
                                           repoIdClass.getName(),
                                           idClass.getName());
        }

        @SuppressWarnings("unchecked") // we checked by previous check methods and the code above.
        AggregatePartRepository<I, A, ?> result = (AggregatePartRepository<I, A, ?>) repo;
        return result;
    }
}
