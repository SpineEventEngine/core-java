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

package io.spine.testing.server.blackbox.verify.state;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import io.spine.client.Query;
import io.spine.client.QueryFactory;
import io.spine.client.QueryResponse;
import io.spine.core.TenantId;
import io.spine.grpc.MemoizingObserver;
import io.spine.server.QueryService;
import org.junit.jupiter.api.Assertions;

import java.util.function.Function;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.protobuf.AnyPacker.unpackFunc;
import static io.spine.testing.client.TestActorRequestFactory.newInstance;
import static java.util.Collections.singletonList;

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
     */
    public final void verify(QueryService queryService) {
        MemoizingObserver<QueryResponse> observer = memoizingObserver();
        queryService.read(query, observer);
        Assertions.assertTrue(observer.isCompleted());
        QueryResponse response = observer.firstResponse();
        ImmutableList<Message> actualEntities = response.getMessagesList()
                                                        .stream()
                                                        .map(unpackFunc())
                                                        .collect(toImmutableList());
        compare(expectedResult, actualEntities);
    }

    /**
     * Compares the expected and the actual entity states.
     */
    protected abstract void compare(ImmutableCollection<? extends Message> expected,
                                    ImmutableCollection<? extends Message> actual);

    /**
     * Obtains provider of an entity states verifier.
     *
     * <p>The verifier checks that the system contains exactly the passed entity states.
     *
     * <p>Use the method to verify entities within the
     * {@linkplain io.spine.testing.server.blackbox.MultitenantBlackBoxContext#tenantId tenant} of a {@link io.spine.testing.server.blackbox.MultitenantBlackBoxContext}.
     *
     * @param entityType
     *         the type of the entity to query
     * @param expected
     *         the expected entity states
     * @return provider of {@link #exactly(TenantId, Class, Iterable)}
     */
    public static <T extends Message> VerifyStateByTenant exactly(Class<T> entityType,
                                                                  Iterable<T> expected) {
        return tenantId -> exactly(tenantId, entityType, expected);
    }

    /**
     * The shortcut of {@link #exactly(Class, Iterable)} to verify that
     * only a single entity is present in the storage and its state matches the expected.
     */
    public static <T extends Message> VerifyStateByTenant exactlyOne(T expected) {
        @SuppressWarnings("unchecked" /* The cast is totally safe. */)
        Class<T> messageClass = (Class<T>) expected.getClass();
        return exactly(messageClass, singletonList(expected));
    }

    /**
     * Obtains a verifier which checks that the system contains exactly the passed entity states.
     *
     * @param <T>
     *         the type of the entity state
     * @param tenantId
     *         the tenant ID of queried storage
     * @param entityType
     *         the type of the entity to query
     * @param expected
     *         the expected entity states
     * @return new instance of {@code VerifyState}
     */
    public static <T extends Message> VerifyState exactly(TenantId tenantId,
                                                          Class<T> entityType,
                                                          Iterable<T> expected) {
        QueryFactory queries = newInstance(VerifyState.class, tenantId).query();
        return new VerifyState(queries.all(entityType), copyOf(expected)) {
            @Override
            protected void compare(ImmutableCollection<? extends Message> expected,
                                   ImmutableCollection<? extends Message> actual) {
                assertThat(actual).containsExactlyElementsIn(expected);
            }
        };
    }

    /**
     * Provides a {@link VerifyState} based on a {@link TenantId}.
     *
     * <p>Use the interface when a tenant ID for {@link VerifyState} should be specified
     * by a {@link io.spine.testing.server.blackbox.MultitenantBlackBoxContext}.
     *
     * <p>If a user wants to specify a tenant ID on its own,
     * {@link VerifyState} should be used directly.
     */
    public interface VerifyStateByTenant extends Function<TenantId, VerifyState> {
    }
}
