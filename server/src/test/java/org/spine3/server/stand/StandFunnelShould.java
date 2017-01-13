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
package org.spine3.server.stand;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.Any;
import io.netty.util.internal.ConcurrentSet;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.spine3.server.BoundedContext;
import org.spine3.server.aggregate.AggregateRepository;
import org.spine3.server.projection.ProjectionRepository;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.testdata.TestStandFactory;

import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Alex Tymchenko
 * @author Dmytro Dashenkov
 */
public class StandFunnelShould {

    // **** Positive scenarios (unit) ****

    @Test
    public void initialize_properly_with_stand_only() {
        final Stand stand = TestStandFactory.create();
        final StandFunnel.Builder builder = StandFunnel.newBuilder()
                                                       .setStand(stand);
        final StandFunnel standFunnel = builder.build();
        Assert.assertNotNull(standFunnel);
    }

    @Test
    public void initialize_properly_with_all_builder_options() {
        final Stand stand = TestStandFactory.create();
        final Executor executor = Executors.newSingleThreadExecutor();

        final StandFunnel funnel = StandFunnel.newBuilder()
                                              .setStand(stand)
                                              .setExecutor(executor)
                                              .build();
        Assert.assertNotNull(funnel);
    }

    @Test
    public void initialize_properly_with_no_executor() {
        final Stand stand = TestStandFactory.create();

        final StandFunnel funnelForBusyStand = StandFunnel.newBuilder()
                                                          .setStand(stand)
                                                          .build();
        Assert.assertNotNull(funnelForBusyStand);
    }

    @Test
    public void deliver_mock_updates_to_stand() {
        final Object id = new Object();
        final Any state = Any.getDefaultInstance();
        final int version = 8;  // random number

        final Stand stand = mock(Stand.class);
        doNothing().when(stand).update(id, state, version);

        final StandFunnel funnel = StandFunnel.newBuilder()
                                              .setStand(stand)
                                              .build();

        funnel.post(id, state, version);

        verify(stand).update(id, state, version);
    }

    @Test
    public void use_executor_from_builder() {
        final Stand stand = spy(TestStandFactory.create());
        final Executor executor = spy(new Executor() {
            @Override
            public void execute(Runnable command) {

            }
        });
        final StandFunnel.Builder builder = StandFunnel.newBuilder()
                                                       .setStand(stand)
                                                       .setExecutor(executor);

        final StandFunnel standFunnel = builder.build();
        Assert.assertNotNull(standFunnel);

        final Any someState = Any.getDefaultInstance();
        final Object someId = new Object();
        final int someVersion = 17;
        standFunnel.post(someId, someState, someVersion);

        verify(executor).execute(any(Runnable.class));
    }

    // **** Negative scenarios (unit) ****

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test(expected = NullPointerException.class)
    public void fail_to_initialize_with_improper_stand() {
        @SuppressWarnings("ConstantConditions") // null is marked as improper with this warning
        final StandFunnel.Builder builder = StandFunnel.newBuilder().setStand(null);

        builder.build();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test(expected = IllegalStateException.class)
    public void fail_to_initialize_from_empty_builder() {
        final StandFunnel.Builder builder = StandFunnel.newBuilder();
        builder.build();
    }

    // **** Integration scenarios (<source> -> StandFunnel -> Mock Stand) ****

    @Test
    public void deliver_updates_from_projection_repository() {
        checkUpdatesDelivery(false, projectionRepositoryDispatch());
    }

    @Test
    public void deliver_updates_form_aggregate_repository() {
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
            result[i] = (i % 2 == 0) ? aggregateRepositoryDispatch() : projectionRepositoryDispatch();
        }

        return result;
    }

    private static void checkUpdatesDelivery(boolean isConcurrent, BoundedContextAction... dispatchActions) {
        checkNotNull(dispatchActions);

        final Stand stand = mock(Stand.class);
        final Executor executor = isConcurrent ?
                                  Executors.newFixedThreadPool(Given.THREADS_COUNT_IN_POOL_EXECUTOR) :
                                  MoreExecutors.directExecutor();

        final BoundedContext boundedContext = spy(Given.boundedContext(stand, executor));

        for (BoundedContextAction dispatchAction : dispatchActions) {
            dispatchAction.perform(boundedContext);
        }

        // Was called as many times as there are dispatch actions.
        verify(boundedContext, times(dispatchActions.length)).getStandFunnel();

        if (isConcurrent) {
            try {
                ((ExecutorService) executor).awaitTermination(Given.SEVERAL, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
        }

        verify(stand, times(dispatchActions.length)).update(ArgumentMatchers.any(), any(Any.class), anyInt());
    }

    private static BoundedContextAction aggregateRepositoryDispatch() {
        return new BoundedContextAction() {
            @Override
            public void perform(BoundedContext context) {
                // Init repository
                final AggregateRepository<?, ?> repository = Given.aggregateRepo(context);

                repository.initStorage(InMemoryStorageFactory.getInstance());

                try {
                    // Mock aggregate and mock stand are not able to handle events returned after command handling.
                    // This causes IllegalStateException to be thrown.
                    // Note that this is not the end of a test case, so we can't just "expect=IllegalStateException"
                    repository.dispatch(Given.validCommand());
                } catch (IllegalStateException e) {
                    // Handle null event dispatching after the command is handled.

                    // Check if this error is caused by returning nuu or empty list after command processing.
                    // Proceed crash if it's not
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
                repository.initStorage(InMemoryStorageFactory.getInstance());

                // Dispatch an update from projection repo
                repository.dispatch(Given.validEvent());
            }
        };
    }

    @SuppressWarnings("MethodWithMultipleLoops")
    @Test
    public void deliver_updates_through_several_threads() throws InterruptedException {
        final int threadsCount = Given.THREADS_COUNT_IN_POOL_EXECUTOR;
        @SuppressWarnings("LocalVariableNamingConvention") // Too long variable name
        final int threadExecutionMaxAwaitSeconds = Given.AWAIT_SECONDS;

        final Set<String> threadInvocationRegistry = new ConcurrentSet<>();

        final Stand stand = mock(Stand.class);
        doNothing().when(stand).update(ArgumentMatchers.any(), any(Any.class), anyInt());

        final StandFunnel standFunnel = StandFunnel.newBuilder()
                                                   .setStand(stand)
                                                   .build();

        final ExecutorService executor = Executors.newFixedThreadPool(threadsCount);

        final Runnable task = new Runnable() {
            @Override
            public void run() {
                final String threadName = Thread.currentThread().getName();
                Assert.assertFalse(threadInvocationRegistry.contains(threadName));

                final int entityVersion = 1;
                standFunnel.post(new Object(), Any.getDefaultInstance(), entityVersion);

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

}
