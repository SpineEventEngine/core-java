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
import org.spine3.testdata.TestStandFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
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
        final Object id = new Object();
        final Any state = Any.getDefaultInstance();

        final Stand stand = mock(Stand.class);
        doNothing().when(stand).update(id, state);

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
    public void fail_to_initialize_with_inproper_stand() {
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
    public void deliver_updates_through_several_threads() throws InterruptedException {
        final int threadsCount = 10;

        final Map<String, Object> threadInvakationRegistry = new ConcurrentHashMap<>(threadsCount);

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
                Assert.assertFalse(threadInvakationRegistry.containsKey(threadName));

                standFunnel.post(new Object(), Any.getDefaultInstance());

                threadInvakationRegistry.put(threadName, new Object());
            }
        };

        for (int i = 0; i < threadsCount; i++) {
            processes.execute(task);
        }

        processes.awaitTermination(10, TimeUnit.SECONDS);

        Assert.assertEquals(threadInvakationRegistry.size(), threadsCount);

    }

}
