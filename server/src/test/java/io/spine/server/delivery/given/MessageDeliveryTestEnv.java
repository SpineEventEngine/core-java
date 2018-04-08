/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.protobuf.Message;
import io.spine.server.BoundedContext;
import io.spine.server.delivery.Shardable;
import io.spine.server.entity.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * An abstract base for environments, which are created to ease the message delivery testing.
 *
 * @author Alex Tymchenko
 */
public class MessageDeliveryTestEnv {

    public abstract static class ParallelDispatcher<I extends Message, M extends Message> {
        private final int threadCount;
        private final int messageCount;
        private final int dispatchWaitTime;

        public ParallelDispatcher(int threadCount, int messageCount, int dispatchWaitTime) {
            this.threadCount = threadCount;
            this.messageCount = messageCount;
            this.dispatchWaitTime = dispatchWaitTime;
        }

        protected abstract EntityStats<I> getStats();

        protected abstract M newMessage();

        protected abstract void postToBus(BoundedContext context, M message);

        public void dispatchMessagesTo(Repository<I, ?> repository) throws Exception {
            final BoundedContext boundedContext = BoundedContext.newBuilder()
                                                                .build();
            boundedContext.register(repository);

            final int numberOfShards = ((Shardable) repository).getShardingStrategy()
                                                               .getNumberOfShards();

            assertTrue(getStats().getThreadToId()
                                 .isEmpty());

            final ExecutorService executorService = newFixedThreadPool(threadCount);
            final ImmutableList.Builder<Callable<Object>> builder = ImmutableList.builder();

            for (int i = 0; i < messageCount; i++) {
                final M message = newMessage();

                builder.add(new Callable<Object>() {
                    @Override
                    public Object call() {
                        postToBus(boundedContext, message);
                        return 0;
                    }
                });
            }

            final List<Callable<Object>> commandPostingJobs = builder.build();
            executorService.invokeAll(commandPostingJobs);

            Thread.sleep(dispatchWaitTime);

            verifyStats(messageCount, numberOfShards);

            repository.close();
            boundedContext.close();
        }

        private void verifyStats(int totalMessages, int numberOfShards) {
            final Map<Long, Collection<I>> whoProcessedWhat = getStats()
                    .getThreadToId()
                    .asMap();
            final Collection<I> actualIds = newHashSet(getStats()
                                                               .getThreadToId()
                                                               .values());
            final Set<Long> actualThreads = whoProcessedWhat.keySet();

            assertEquals(numberOfShards, actualThreads.size());
            assertEquals(totalMessages, actualIds.size());
        }
    }

    public static class EntityStats<I extends Message> {

        private final Multimap<Long, I> threadToId =
                Multimaps.synchronizedMultimap(HashMultimap.<Long, I>create());

        public void recordCallingThread(I id) {
            final long currentThreadId = Thread.currentThread()
                                               .getId();
            threadToId.put(currentThreadId, id);
        }

        public Multimap<Long, I> getThreadToId() {
            return Multimaps.unmodifiableMultimap(threadToId);
        }

        public void clear() {
            threadToId.clear();
        }
    }
}
