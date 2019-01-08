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
package io.spine.server.stand;

import com.google.common.util.concurrent.MoreExecutors;
import io.netty.util.internal.ConcurrentSet;
import io.spine.base.Identifier;
import io.spine.core.CommandEnvelope;
import io.spine.core.EventEnvelope;
import io.spine.core.Version;
import io.spine.server.BoundedContext;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.entity.EntityStateEnvelope;
import io.spine.server.projection.ProjectionRepository;
import io.spine.server.stand.given.Given;
import io.spine.server.stand.given.Given.StandTestAggregate;
import io.spine.server.storage.StorageFactory;
import io.spine.server.test.shared.EmptyAggregate;
import io.spine.test.projection.ProjectId;
import io.spine.testing.client.TestActorRequestFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Disabled //TODO:2017-05-03:alexander.yevsyukov: Enable back when Stand becomes a Bus.
@DisplayName("Stand `post` should")
class StandPostTest {

    private final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(StandPostTest.class);

    // **** Positive scenarios (unit) ****

    private static BoundedContextAction[] getSeveralRepositoryDispatchCalls() {
        BoundedContextAction[] result = new BoundedContextAction[Given.SEVERAL];

        for (int i = 0; i < result.length; i++) {
            result[i] = ((i % 2) == 0)
                        ? StandPostTest::aggregateRepositoryDispatch
                        : StandPostTest::projectionRepositoryDispatch;
        }

        return result;
    }

    // **** Integration scenarios (<source> -> StandFunnel -> Mock Stand) ****

    private static void checkUpdatesDelivery(boolean isConcurrent,
                                             BoundedContextAction... dispatchActions) {
        checkNotNull(dispatchActions);

        Executor executor = isConcurrent
                            ? Executors.newFixedThreadPool(
                Given.THREADS_COUNT_IN_POOL_EXECUTOR)
                            : MoreExecutors.directExecutor();

        BoundedContext boundedContext =
                BoundedContext.newBuilder()
                              .setStand(Stand.newBuilder())
                              .build();

        Stand stand = boundedContext.getStand();

        for (BoundedContextAction dispatchAction : dispatchActions) {
            dispatchAction.perform(boundedContext);
        }

        if (isConcurrent) {
            try {
                ((ExecutorService) executor).awaitTermination(Given.SEVERAL, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
        }

        verify(stand, times(dispatchActions.length)).update(any(EntityStateEnvelope.class));
    }

    private static StorageFactory storageFactory(boolean multitenant) {
        BoundedContext bc = BoundedContext
                .newBuilder()
                .setMultitenant(multitenant)
                .build();
        return bc.getStorageFactory();
    }

    /**
     * Creates a repository and dispatches a command to it.
     */
    @SuppressWarnings("CheckReturnValue") // can ignore the dispatch() result
    private static void aggregateRepositoryDispatch(BoundedContext context) {
        // Init repository
        AggregateRepository<?, ?> repository = Given.aggregateRepo();
        repository.initStorage(storageFactory(context.isMultitenant()));

        try {
            // Mock aggregate and mock stand are not able to handle events
            // returned after command handling.
            // This causes IllegalStateException to be thrown.
            // Note that this is not the end of a test case,
            // so we can't just "expect=IllegalStateException".
            CommandEnvelope cmd = CommandEnvelope.of(Given.validCommand());
            repository.dispatch(cmd);
        } catch (IllegalStateException e) {
            // Handle null event dispatching after the command is handled.

            // Check if this error is caused by returning null or empty list after
            // command processing.
            // Proceed crash if it's not.
            if (!e.getMessage()
                  .contains("No record found for command ID: EMPTY")) {
                throw e;
            }
        }
    }

    @SuppressWarnings("CheckReturnValue") // can ignore the dispatch() result
    private static void projectionRepositoryDispatch(BoundedContext context) {
        ProjectionRepository repository = Given.projectionRepo();
        repository.initStorage(storageFactory(context.isMultitenant()));

        // Dispatch an update from projection repo
        repository.dispatch(EventEnvelope.of(Given.validEvent()));
    }

    @Test
    @DisplayName("deliver updates")
    void deliverUpdates() {
        AggregateRepository<ProjectId, StandTestAggregate> repository = Given.aggregateRepo();
        ProjectId entityId = ProjectId
                .newBuilder()
                .setId("PRJ-001")
                .build();
        StandTestAggregate entity = repository.create(entityId);
        EmptyAggregate state = entity.getState();
        Version version = entity.getVersion();

        Stand innerStand = Stand.newBuilder()
                                .build();
        Stand stand = spy(innerStand);

        stand.post(requestFactory.createCommandContext()
                                 .getActorContext()
                                 .getTenantId(), entity);

        ArgumentMatcher<EntityStateEnvelope<?, ?>> argumentMatcher =
                argument -> {
                    boolean entityIdMatches = argument.getEntityId()
                                                      .equals(entityId);
                    boolean versionMatches = version.equals(argument.getEntityVersion()
                                                                    .orElse(null));
                    boolean stateMatches = argument.getMessage()
                                                   .equals(state);
                    return entityIdMatches
                            && versionMatches
                            && stateMatches;
                };
        verify(stand).update(argThat(argumentMatcher));
    }

    @Test
    @DisplayName("deliver updates from projection repository")
    void deliverFromProjectionRepo() {
        checkUpdatesDelivery(false, StandPostTest::projectionRepositoryDispatch);
    }

    @Test
    @DisplayName("deliver updates from aggregate repository")
    void deliverFromAggregateRepo() {
        checkUpdatesDelivery(false, StandPostTest::aggregateRepositoryDispatch);
    }

    @Test
    @DisplayName("deliver updates from several repositories in single thread")
    void deliverFromSeveralRepos() {
        checkUpdatesDelivery(false, getSeveralRepositoryDispatchCalls());
    }

    @Test
    @DisplayName("deliver updates from several repositories in multiple threads")
    void deliverFromConcurrentRepos() {
        checkUpdatesDelivery(true, getSeveralRepositoryDispatchCalls());
    }

    @Test
    @DisplayName("deliver updates through several threads")
    void deliverThroughSeveralThreads() throws InterruptedException {
        int threadsCount = Given.THREADS_COUNT_IN_POOL_EXECUTOR;

        Set<String> threadInvocationRegistry = new ConcurrentSet<>();

        Stand stand = Stand.newBuilder()
                           .build();

        ExecutorService executor = Executors.newFixedThreadPool(threadsCount);

        Runnable task = () -> {
            String threadName = Thread.currentThread()
                                      .getName();
            assertFalse(threadInvocationRegistry.contains(threadName));
            ProjectId entityId = ProjectId
                    .newBuilder()
                    .setId(Identifier.newUuid())
                    .build();
            StandTestAggregate entity = Given.aggregateRepo()
                                             .create(entityId);
            stand.post(requestFactory.createCommandContext()
                                     .getActorContext()
                                     .getTenantId(), entity);

            threadInvocationRegistry.add(threadName);
        };

        for (int i = 0; i < threadsCount; i++) {
            executor.execute(task);
        }

        executor.awaitTermination(Given.AWAIT_SECONDS, TimeUnit.SECONDS);

        assertEquals(threadInvocationRegistry.size(), threadsCount);
    }

    private interface BoundedContextAction {

        void perform(BoundedContext context);
    }
}
