/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.testing.server.blackbox;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import io.spine.client.Query;
import io.spine.client.QueryFactory;
import io.spine.client.QueryResponse;
import io.spine.core.ActorContext;
import io.spine.core.TenantId;
import io.spine.grpc.MemoizingObserver;
import io.spine.server.QueryService;
import org.junit.jupiter.api.Assertions;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.protobuf.AnyPacker.unpackFunc;
import static io.spine.testing.client.TestActorRequestFactory.newInstance;

/**
 * Verifies the states of entities currently present in a bounded context.
 */
@VisibleForTesting
public abstract class VerifyState {

    private final Query query;
    private final ImmutableCollection<? extends Message> expectedResult;

    VerifyState(Query query, ImmutableCollection<? extends Message> result) {
        this.query = query;
        expectedResult = result;
    }

    /**
     * Verifies the entity states.
     *
     * @param queryService
     *         the query service to obtain entity states from
     * @param tenantId
     *         the tenant ID of queried storage
     */
    public final void verify(QueryService queryService, TenantId tenantId) {
        MemoizingObserver<QueryResponse> observer = memoizingObserver();
        Query queryForTenant = queryFor(tenantId);
        queryService.read(queryForTenant, observer);
        Assertions.assertTrue(observer.isCompleted());
        QueryResponse response = observer.firstResponse();
        ImmutableList<Message> actualEntities = response.getMessagesList()
                                                        .stream()
                                                        .map(unpackFunc())
                                                        .collect(toImmutableList());
        compare(expectedResult, actualEntities);
    }

    /** Obtains the {@link #query} with the specified {@code TenantId}. */
    private Query queryFor(TenantId tenantId) {
        ActorContext contextWithTenant = query.getContext()
                                              .toBuilder()
                                              .setTenantId(tenantId)
                                              .build();
        return query.toBuilder()
                    .setContext(contextWithTenant)
                    .build();
    }

    /**
     * Compares the expected and the actual entity states.
     */
    protected abstract void compare(ImmutableCollection<? extends Message> expected,
                                    ImmutableCollection<? extends Message> actual);

    /**
     * Obtains a verifier which checks that the system contains exactly the passed entity states.
     *
     * @param entityType
     *         the type of the entity to query
     * @param expected
     *         the expected entity states
     * @param <T>
     *         the type of the entity state
     * @return new instance of {@code VerifyState}
     */
    public static <T extends Message> VerifyState exactly(Class<T> entityType,
                                                          Iterable<T> expected) {
        QueryFactory queries = newInstance(VerifyState.class).query();
        return new VerifyState(queries.all(entityType), copyOf(expected)) {
            @Override
            protected void compare(ImmutableCollection<? extends Message> expected,
                                   ImmutableCollection<? extends Message> actual) {
                assertThat(actual).containsExactlyElementsIn(expected);
            }
        };
    }
}
