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

package io.spine.testing.server.blackbox.verify.state;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import io.spine.client.EntityStateWithVersion;
import io.spine.client.Query;
import io.spine.client.QueryFactory;
import io.spine.client.QueryResponse;
import io.spine.grpc.MemoizingObserver;
import io.spine.protobuf.AnyPacker;
import io.spine.server.BoundedContext;
import io.spine.server.QueryService;

import java.util.Collection;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the states of entities currently present in a bounded context.
 */
@VisibleForTesting
public abstract class VerifyState {

    /**
     * Queries the read-side of the provided {@code BoundedContext} and
     * {@linkplain #verify(Collection)} verifies} the results according to the pre-set expectations.
     *
     * @param boundedContext
     *         the bounded context to query
     * @param queryFactory
     *         the factory to create queries
     */
    public void verify(BoundedContext boundedContext, QueryFactory queryFactory) {
        Query query = query(queryFactory);
        MemoizingObserver<QueryResponse> observer = memoizingObserver();
        QueryService queryService = QueryService.newBuilder()
                                                .add(boundedContext)
                                                .build();
        queryService.read(query, observer);
        assertTrue(observer.isCompleted());
        QueryResponse response = observer.firstResponse();
        ImmutableList<? extends Message> entities = response.getMessagesList()
                                                            .stream()
                                                            .map(EntityStateWithVersion::getState)
                                                            .map(AnyPacker::unpack)
                                                            .collect(toImmutableList());
        verify(entities);
    }

    /**
     * Obtains the query for entities to be verified.
     */
    protected abstract Query query(QueryFactory factory);

    /**
     * Verifies actual entity states and throws an assertion error if the verification is failed.
     */
    protected abstract void verify(Collection<? extends Message> actualEntities);

    /**
     * The shortcut of {@link #exactly(Class, Iterable)} to verify that
     * only a single entity is present in the storage and its state matches the expected.
     */
    public static <T extends Message> VerifyState exactlyOne(T expected) {
        @SuppressWarnings("unchecked" /* The cast is totally safe. */)
        Class<T> messageClass = (Class<T>) expected.getClass();
        return exactly(messageClass, singletonList(expected));
    }

    /**
     * Obtains a verifier which checks that the system contains exactly the passed entity states.
     *
     * @param <T>
     *         the type of the entity state
     * @param entityType
     *         the type of the entity to query
     * @param expected
     *         the expected entity states
     * @return new instance of {@code VerifyState}
     */
    public static <T extends Message> VerifyState exactly(Class<T> entityType,
                                                          Iterable<T> expected) {
        return new AllOfTypeMatch<>(expected, entityType);
    }
}
