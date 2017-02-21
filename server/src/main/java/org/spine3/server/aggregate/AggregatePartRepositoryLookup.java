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

import com.google.common.base.Optional;
import com.google.protobuf.Message;
import org.spine3.server.BoundedContext;

/**
 * A helper class for finding and checking {@code AggregatePartRepository}.
 *
 * <p>This class is used by {@link AggregateRoot} to find repositories for its parts.
 *
 * @param <I> the type of the IDs of the repository to find
 * @param <S> the type of the state of aggregate parts of managed by the target repository
 * @author Alexander Yevsyukov
 */
class AggregatePartRepositoryLookup<I, S extends Message> {

    private final BoundedContext boundedContext;
    private final Class<I> idClass;
    private final Class<S> stateClass;

    /**
     * Creates the lookup object for finding the repository that
     * manages aggregate parts with the passed state class.
     */
    static <I, S extends Message> AggregatePartRepositoryLookup<I, S> createLookup(
            BoundedContext boundedContext,
            Class<I> idClass,
            Class<S> stateClass) {
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
    <A extends AggregatePart<I, S, ?>, R extends AggregateRoot<I>> AggregatePartRepository<I, A, R> find() {
        final AggregateRepository<?, ?> repo =
                checkFound(boundedContext.getAggregateRepository(stateClass));

        checkIsAggregatePartRepository(repo);

        final AggregatePartRepository<I, A, R> result = checkIdClass(
                (AggregatePartRepository<?, ?, ?>) repo);
        return result;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType") // as this is the purpose of the method
    private AggregateRepository<?, ?> checkFound(
            Optional<? extends AggregateRepository<?, ?>> rawRepo) {
        if (!rawRepo.isPresent()) {
            final String errMsg = String.format("Unable to find repository for the state class: %s",
                                                stateClass);
            throw new IllegalStateException(errMsg);
        } else {
            return rawRepo.get();
        }
    }

    /**
     * Ensures that the passed repository is instance of {@code AggregatePartRepository}.
     *
     * <p>We check this to make sure that expectations of this {@code AggregateRoot} are supported
     * by correct configuration of the {@code BoundedContext}.
     */
    private static void checkIsAggregatePartRepository(AggregateRepository<?, ?> repo) {
        if (!(repo instanceof AggregatePartRepository)) {
            final String errMsg = String.format("The repository `%s` is not an instance of `%s`",
                                                repo, AggregatePartRepository.class);
            throw new IllegalStateException(errMsg);
        }
    }

    /**
     * Ensures the type of the IDs of the passed repository.
     */
    private <A extends AggregatePart<I, S, ?>, R extends AggregateRoot<I>> AggregatePartRepository<I, A, R> checkIdClass(
            AggregatePartRepository<?, ?, ?> repo) {
        final Class<?> repoIdClass = repo.getIdClass();
        if (!idClass.equals(repoIdClass)) {
            final String errMsg = String.format(
                    "The ID class of the aggregate part repository (%s) " +
                    "does not match the ID class of the AggregateRoot (%s)",
                    repoIdClass, idClass);
            throw new IllegalStateException(errMsg);
        }

        @SuppressWarnings("unchecked") // we checked by previous check methods and the code above.
        final AggregatePartRepository<I, A, R> result = (AggregatePartRepository<I, A, R>) repo;
        return result;
    }
}
