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
package io.spine.server.delivery.given;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import io.spine.server.BoundedContext;
import io.spine.server.delivery.Shardable;
import io.spine.server.entity.Repository;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newFixedThreadPool;

/**
 * The helper class used to dispatch numerous messages of a certain kind to the entities,
 * managed by a specific repository.
 *
 * <p>Another feature is to check that the messages were delivered to proper shards,
 * according to the repository sharding configuration.
 *
 * @param <I> the type of the entity identifiers
 * @param <M> the type of messages to dispatch (e.g. {@link io.spine.core.Command Command}).
 * @author Alex Tymchenko
 */
public abstract class ParallelDispatcher<I extends Message, M extends Message> {
    /**
     * The count of threads to use for dispatching.
     */
    private final int threadCount;

    /**
     * The count of messages to dispatch in several threads.
     */
    private final int messageCount;

    /**
     * The time to wait in the main thread before analyzing the statistics of message delivery.
     */
    private final int dispatchWaitTime;

    protected ParallelDispatcher(int threadCount, int messageCount, int dispatchWaitTime) {
        this.threadCount = threadCount;
        this.messageCount = messageCount;
        this.dispatchWaitTime = dispatchWaitTime;
    }

    /**
     * Obtains the stats for the entity kind, which is served by this dispatcher.
     */
    protected abstract ThreadStats<I> getStats();

    /**
     * Creates a new message to be used in the dispatching.
     */
    protected abstract M newMessage();

    /**
     * Posts the message to the respective bus.
     */
    protected abstract void postToBus(BoundedContext context, M message);

    /**
     * Dispatches messages in several threads and check if they are delivered according to the
     * sharding configuration for this repository.
     *
     * @param repository the repository which sharding configuration to take into account
     * @throws Exception in case the multithreading message propagation breaks
     */
    public void dispatchMessagesTo(Repository<I, ?> repository) throws Exception {
        BoundedContext boundedContext = BoundedContext.newBuilder()
                                                      .build();
        boundedContext.register(repository);

        int numberOfShards = ((Shardable) repository).getShardingStrategy()
                                                     .getNumberOfShards();
        getStats().assertIdCount(0);

        ExecutorService executorService = newFixedThreadPool(threadCount);
        ImmutableList.Builder<Callable<Object>> builder = ImmutableList.builder();

        for (int i = 0; i < messageCount; i++) {
            M message = newMessage();
            builder.add(() -> {
                postToBus(boundedContext, message);
                return 0;
            });
        }

        List<Callable<Object>> commandPostingJobs = builder.build();
        executorService.invokeAll(commandPostingJobs);

        Thread.sleep(dispatchWaitTime);
        executorService.shutdown();

        verifyStats(messageCount, numberOfShards);

        repository.close();
        boundedContext.close();
    }

    private void verifyStats(int totalMessages, int numberOfShards) {
        ThreadStats<I> stats = getStats();
        stats.assertThreadCount(numberOfShards);
        stats.assertIdCount(totalMessages);
    }
}
