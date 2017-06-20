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
package io.spine.server.stand;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.StringValue;
import io.netty.util.internal.ConcurrentSet;
import io.spine.Identifier;
import io.spine.base.CommandContext;
import io.spine.base.Version;
import io.spine.client.TestActorRequestFactory;
import io.spine.envelope.CommandEnvelope;
import io.spine.envelope.EventEnvelope;
import io.spine.server.BoundedContext;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.entity.AbstractVersionableEntity;
import io.spine.server.entity.EntityStateEnvelope;
import io.spine.server.entity.VersionableEntity;
import io.spine.server.projection.ProjectionRepository;
import io.spine.server.storage.StorageFactory;
import io.spine.test.projection.ProjectId;
import io.spine.time.Time;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;

import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.base.Versions.newVersion;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Alex Tymchenko
 * @author Dmytro Dashenkov
 */
@Ignore //TODO:2017-05-03:alexander.yevsyukov: Enable back when Stand becomes a Bus.
public class StandPostShould {

    private final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(StandPostShould.class);

    // **** Positive scenarios (unit) ****

    @SuppressWarnings({"OverlyComplexAnonymousInnerClass", "ConstantConditions"})
    @Test
    public void deliver_updates() {
        final AggregateRepository<ProjectId, Given.StandTestAggregate> repository =
                Given.aggregateRepo();
        final ProjectId entityId = ProjectId.newBuilder()
                                            .setId("PRJ-001")
                                            .build();
        final Given.StandTestAggregate entity = repository.create(entityId);
        final StringValue state = entity.getState();
        final Version version = entity.getVersion();

        final Stand innerStand = Stand.newBuilder().build();
        final Stand stand = spy(innerStand);

        stand.post(entity, requestFactory.createCommandContext());

        final ArgumentMatcher<EntityStateEnvelope<?, ?>> argumentMatcher =
                new ArgumentMatcher<EntityStateEnvelope<?, ?>>() {
                    @Override
                    public boolean matches(EntityStateEnvelope<?, ?> argument) {
                        final boolean entityIdMatches = argument.getEntityId()
                                                                .equals(entityId);
                        final boolean versionMatches = version.equals(argument.getEntityVersion()
                                                                              .orNull());
                        final boolean stateMatches = argument.getMessage()
                                                             .equals(state);
                        return entityIdMatches &&
                                versionMatches &&
                                stateMatches;
                    }
                };
        verify(stand).update(ArgumentMatchers.argThat(argumentMatcher));
    }

    @SuppressWarnings("MagicNumber")
    @Test
    public void use_delivery_from_builder() {
        final StandUpdateDelivery delivery = spy(new StandUpdateDelivery() {
            @Override
            protected boolean shouldPostponeDelivery(EntityStateEnvelope deliverable,
                                                     Stand consumer) {
                return false;
            }
        });
        final Stand.Builder builder = Stand.newBuilder()
                                           .setDelivery(delivery);

        final Stand stand = builder.build();
        Assert.assertNotNull(stand);

        final Object id = Identifier.newUuid();
        final StringValue state = StringValue.getDefaultInstance();

        final VersionableEntity entity = mock(AbstractVersionableEntity.class);
        when(entity.getState()).thenReturn(state);
        when(entity.getId()).thenReturn(id);
        when(entity.getVersion()).thenReturn(newVersion(17, Time.getCurrentTime()));

        final CommandContext context = requestFactory.createCommandContext();
        stand.post(entity, context);

        final EntityStateEnvelope envelope = EntityStateEnvelope.of(entity,
                                                                    context.getActorContext()
                                                                           .getTenantId());
        verify(delivery).deliverNow(eq(envelope), eq(Stand.class));
    }

    // **** Integration scenarios (<source> -> StandFunnel -> Mock Stand) ****

    @Test
    public void deliver_updates_from_projection_repository() {
        checkUpdatesDelivery(false, projectionRepositoryDispatch());
    }

    @Test
    public void deliver_updates_from_aggregate_repository() {
        checkUpdatesDelivery(false, aggregateRepositoryDispatch());
    }

    @Test
    public void deliver_updates_from_several_repositories_in_single_thread() {
        checkUpdatesDelivery(false, getSeveralRepositoryDispatchCalls());
    }

    @Test
    public void deliver_updates_from_several_repositories_in_multiple_threads() {
        checkUpdatesDelivery(true, getSeveralRepositoryDispatchCalls());
    }

    private static BoundedContextAction[] getSeveralRepositoryDispatchCalls() {
        final BoundedContextAction[] result = new BoundedContextAction[Given.SEVERAL];

        for (int i = 0; i < result.length; i++) {
            result[i] = (i % 2 == 0)
                    ? aggregateRepositoryDispatch()
                    : projectionRepositoryDispatch();
        }

        return result;
    }

    private static void checkUpdatesDelivery(boolean isConcurrent,
                                             BoundedContextAction... dispatchActions) {
        checkNotNull(dispatchActions);

        final Executor executor = isConcurrent
                ? Executors.newFixedThreadPool(Given.THREADS_COUNT_IN_POOL_EXECUTOR)
                : MoreExecutors.directExecutor();
        final StandUpdateDelivery delivery = spy(new SpyableStandUpdateDelivery(executor));

        final BoundedContext boundedContext =
                BoundedContext.newBuilder()
                              .setStand(Stand.newBuilder()
                                             .setDelivery(delivery))
                              .build();

        final Stand stand = boundedContext.getStand();

        for (BoundedContextAction dispatchAction : dispatchActions) {
            dispatchAction.perform(boundedContext);
        }

        // Was called as many times as there are dispatch actions.
        verify(delivery, times(dispatchActions.length)).deliver(any(EntityStateEnvelope.class));

        if (isConcurrent) {
            try {
                ((ExecutorService) executor).awaitTermination(Given.SEVERAL, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
        }

        verify(stand, times(dispatchActions.length)).update(any(EntityStateEnvelope.class));
    }

    private static BoundedContextAction aggregateRepositoryDispatch() {
        return new BoundedContextAction() {
            @Override
            public void perform(BoundedContext context) {
                // Init repository
                final AggregateRepository<?, ?> repository = Given.aggregateRepo(context);

                repository.initStorage(storageFactory(context.isMultitenant()));

                try {
                    // Mock aggregate and mock stand are not able to handle events
                    // returned after command handling.
                    // This causes IllegalStateException to be thrown.
                    // Note that this is not the end of a test case,
                    // so we can't just "expect=IllegalStateException".
                    final CommandEnvelope cmd = CommandEnvelope.of(Given.validCommand());
                    repository.dispatch(cmd);
                } catch (IllegalStateException e) {
                    // Handle null event dispatching after the command is handled.

                    // Check if this error is caused by returning nuu or empty list after
                    // command processing.
                    // Proceed crash if it's not.
                    if (!e.getMessage()
                          .contains("No record found for command ID: EMPTY")) {
                        throw e;
                    }
                }
            }
        };
    }

    private static BoundedContextAction projectionRepositoryDispatch() {
        return new BoundedContextAction() {
            @Override
            public void perform(BoundedContext context) {
                // Init repository
                final ProjectionRepository repository = Given.projectionRepo(context);
                repository.initStorage(storageFactory(context.isMultitenant()));

                // Dispatch an update from projection repo
                repository.dispatch(EventEnvelope.of(Given.validEvent()));
            }
        };
    }

    private static StorageFactory storageFactory(boolean multitenant) {
        final BoundedContext bc = BoundedContext.newBuilder()
                                                .setMultitenant(multitenant)
                                                .build();
        return bc.getStorageFactory();
    }

    @SuppressWarnings("MethodWithMultipleLoops")
    @Test
    public void deliver_updates_through_several_threads() throws InterruptedException {
        final int threadsCount = Given.THREADS_COUNT_IN_POOL_EXECUTOR;
        @SuppressWarnings("LocalVariableNamingConvention") // Too long variable name
        final int threadExecutionMaxAwaitSeconds = Given.AWAIT_SECONDS;

        final Set<String> threadInvocationRegistry = new ConcurrentSet<>();

        final Stand stand = Stand.newBuilder()
                                 .build();

        final ExecutorService executor = Executors.newFixedThreadPool(threadsCount);

        final Runnable task = new Runnable() {
            @Override
            public void run() {
                final String threadName = Thread.currentThread()
                                                .getName();
                Assert.assertFalse(threadInvocationRegistry.contains(threadName));
                final ProjectId enitityId = ProjectId.newBuilder()
                                                     .setId(Identifier.newUuid())
                                                     .build();
                final Given.StandTestAggregate entity = Given.aggregateRepo()
                                                             .create(enitityId);
                stand.post(entity, requestFactory.createCommandContext());

                threadInvocationRegistry.add(threadName);
            }
        };

        for (int i = 0; i < threadsCount; i++) {
            executor.execute(task);
        }

        executor.awaitTermination(threadExecutionMaxAwaitSeconds, TimeUnit.SECONDS);

        Assert.assertEquals(threadInvocationRegistry.size(), threadsCount);

    }

    private interface BoundedContextAction {
        void perform(BoundedContext context);
    }

    /**
     * A custom {@code StandUpdateDelivery}, which is suitable for
     * {@linkplain org.mockito.Mockito#spy(Object) spying}.
     */
    private static class SpyableStandUpdateDelivery extends StandUpdateDelivery {

        public SpyableStandUpdateDelivery(Executor delegate) {
            super(delegate);
        }

        @Override
        protected boolean shouldPostponeDelivery(EntityStateEnvelope deliverable, Stand consumer) {
            return false;
        }
    }
}
