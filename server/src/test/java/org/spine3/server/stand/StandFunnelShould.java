/*
 *
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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
 *
 */
package org.spine3.server.stand;

import com.google.protobuf.Any;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.spine3.server.BoundedContext;
import org.spine3.server.aggregate.AggregateRepository;
import org.spine3.server.projection.ProjectionRepository;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.testdata.TestStandFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.mockito.ArgumentMatchers.any;
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

    /**
     * - deliver mock updates to the stand (invoke proper methods with particular arguments) - test the delivery only.
     */

    @Test
    public void initialize_properly_with_stand_only() {
        final Stand stand = TestStandFactory.create();
        final StandFunnel.Builder builder = StandFunnel.newBuilder()
                                                       .setStand(stand);
        final StandFunnel standFunnel = builder.build();
        Assert.assertNotNull(standFunnel);
    }

    @Test
    public void initialize_properly_with_various_builder_options() {
        final Stand stand = TestStandFactory.create();
        final Executor executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return Thread.currentThread();
            }
        });

        final StandFunnel blockingFunnel = StandFunnel.newBuilder()
                                                      .setStand(stand)
                                                      .setExecutor(executor)
                                                      .build();
        Assert.assertNotNull(blockingFunnel);

        final StandFunnel funnelForBusyStand = StandFunnel.newBuilder()
                                                          .setStand(stand)
                                                          .setExecutor(Executors.newSingleThreadExecutor())
                                                          .build();
        Assert.assertNotNull(funnelForBusyStand);


        final StandFunnel emptyExecutorFunnel = StandFunnel.newBuilder()
                                                           .setStand(TestStandFactory.create())
                                                           .setExecutor(new Executor() {
                                                               @Override
                                                               public void execute(Runnable neverCalled) { }
                                                           })
                                                           .build();
        Assert.assertNotNull(emptyExecutorFunnel);
    }

    @Test
    public void deliver_mock_updates_to_stand() {
        final Stand stand = spy(TestStandFactory.create());

        final StandFunnel funnel = StandFunnel.newBuilder()
                                              .setStand(stand)
                                              .build();

        funnel.post(id, state);

        verify(stand).update(id, state);
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
        standFunnel.post(someId, someState);

        verify(executor).execute(any(Runnable.class));
    }


    // **** Negative scenarios (unit) ****

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test(expected = NullPointerException.class)
    public void fail_to_initialize_with_null_stand() {
        @SuppressWarnings("ConstantConditions")
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

    /**
     * - Deliver updates from projection repo on update;
     * - deliver updates from aggregate repo on update;
     * - deliver the updates from several projection and aggregate repositories.
     */


    @SuppressWarnings("MethodWithMultipleLoops")
    @Test
    public void deliver_updates_from_projection_repository() {
        deliverUpdates(false, projectionRepositoryDispatch());
    }

    @Test
    public void deliver_updates_form_aggregate_repository() {
        deliverUpdates(false, aggregateRepositoryDispatch());
    }

    @Test
    public void deliver_updates_from_several_repositories_in_single_thread() {
        deliverUpdates(false, getSeveralRepositoryDispatchCalls());
    }

    @Test
    public void deliver_updates_from_several_repositories_in_multiple_threads() {
        deliverUpdates(true, getSeveralRepositoryDispatchCalls());
    }

    private static BoundedContextAction[] getSeveralRepositoryDispatchCalls() {
        final BoundedContextAction[] result = new BoundedContextAction[Given.SEVERAL];
        final Random random = new SecureRandom();

        for (int i = 0; i < result.length; i++) {
            result[i] = random.nextBoolean() ? aggregateRepositoryDispatch() : projectionRepositoryDispatch();
        }

        return result;
    }

    private static void deliverUpdates(boolean isMultiThread, BoundedContextAction... dispatchActions) {
        checkNotNull(dispatchActions);

        final Stand stand = mock(Stand.class);
        final BoundedContext boundedContext = spy(Given.boundedContext(stand,
                                                                       isMultiThread ?
                                                                       Given.THREADS_COUNT_IN_POOL_EXECUTOR : 0));

        for (BoundedContextAction dispatchAction : dispatchActions) {
            dispatchAction.perform(boundedContext);
        }


        // Was called ONCE
        verify(boundedContext, times(dispatchActions.length)).getStandFunnel();
        verify(stand, times(dispatchActions.length)).update(ArgumentMatchers.any(), any(Any.class));
    }

    private static BoundedContextAction aggregateRepositoryDispatch() {
        return new BoundedContextAction() {
            @Override
            public void perform(BoundedContext context) {
                // Init repository
                final AggregateRepository<?, ?> repository = Given.aggregateRepo(context);

                repository.initStorage(InMemoryStorageFactory.getInstance());

                try {
                    repository.dispatch(Given.validCommand());
                } catch (IllegalStateException e) {
                    // Handle null event dispatch after command handling.
                    Assert.assertTrue(e.getMessage().contains("No record found for command ID: EMPTY"));
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
                repository.setOnline();

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
        final int threadExecutionMaxAwaitSeconds = 2;

        final Map<String, Object> threadInvocationRegistry = new ConcurrentHashMap<>(threadsCount);

        final Stand stand = mock(Stand.class);
        doNothing().when(stand).update(ArgumentMatchers.any(), any(Any.class));

        final StandFunnel standFunnel = StandFunnel.newBuilder()
                                                   .setStand(stand)
                                                   .build();

        final ExecutorService processes = Executors.newFixedThreadPool(threadsCount);


        final Runnable task = new Runnable() {
            @Override
            public void run() {
                final String threadName = Thread.currentThread().getName();
                Assert.assertFalse(threadInvocationRegistry.containsKey(threadName));

                standFunnel.post(new Object(), Any.getDefaultInstance());

                threadInvocationRegistry.put(threadName, new Object());
            }
        };

        for (int i = 0; i < threadsCount; i++) {
            processes.execute(task);
        }

        processes.awaitTermination(threadExecutionMaxAwaitSeconds, TimeUnit.SECONDS);

        Assert.assertEquals(threadInvocationRegistry.size(), threadsCount);

    }

    private interface BoundedContextAction {
        void perform(BoundedContext context);
    }

}
