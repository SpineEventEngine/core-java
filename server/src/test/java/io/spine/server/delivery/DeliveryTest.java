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

package io.spine.server.delivery;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.protobuf.util.Durations;
import io.spine.base.SerializableMessage;
import io.spine.core.TenantId;
import io.spine.server.ServerEnvironment;
import io.spine.server.delivery.given.CalcAggregate;
import io.spine.server.delivery.given.CalculatorSignal;
import io.spine.server.delivery.given.DeliveryTestEnv.CalculatorRepository;
import io.spine.server.delivery.given.DeliveryTestEnv.RawMessageMemoizer;
import io.spine.server.delivery.given.DeliveryTestEnv.ShardIndexMemoizer;
import io.spine.server.delivery.given.DeliveryTestEnv.SignalMemoizer;
import io.spine.server.delivery.given.FixedShardStrategy;
import io.spine.test.delivery.AddNumber;
import io.spine.test.delivery.Calc;
import io.spine.test.delivery.NumberImported;
import io.spine.test.delivery.NumberReacted;
import io.spine.testing.server.blackbox.BlackBoxBoundedContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.google.common.collect.Streams.concat;
import static com.google.common.truth.Truth.assertThat;
import static io.spine.server.delivery.given.DeliveryTestEnv.manyTargets;
import static io.spine.server.delivery.given.DeliveryTestEnv.singleTarget;
import static io.spine.server.tenant.TenantAwareRunner.with;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@DisplayName("Delivery of messages to entities should deliver those via")
public class DeliveryTest {

    private Delivery originalDelivery;

    @BeforeEach
    public void setUp() {
        this.originalDelivery = ServerEnvironment.instance()
                                                 .delivery();
    }

    @AfterEach
    public void tearDown() {
        ServerEnvironment.instance()
                         .configureDelivery(originalDelivery);
    }

    @Test
    @DisplayName("a single shard to a single target in a multi-threaded env")
    public void singleTarget_singleShard_manyThreads() {
        changeShardCountTo(1);
        ImmutableSet<String> aTarget = singleTarget();
        new ThreadSimulator(42).runWith(aTarget);
    }

    @Test
    @DisplayName("a single shard to multiple targets in a multi-threaded env")
    public void manyTargets_singleShard_manyThreads() {
        changeShardCountTo(1);
        ImmutableSet<String> targets = manyTargets(7);
        new ThreadSimulator(10).runWith(targets);
    }

    @Test
    @DisplayName("multiple shards to a single target in a multi-threaded env")
    public void singleTarget_manyShards_manyThreads() {
        changeShardCountTo(1986);
        ImmutableSet<String> targets = singleTarget();
        new ThreadSimulator(15).runWith(targets);
    }

    @Test
    @DisplayName("multiple shards to multiple targets in a multi-threaded env")
    public void manyTargets_manyShards_manyThreads() {
        changeShardCountTo(2004);
        ImmutableSet<String> targets = manyTargets(13);
        new ThreadSimulator(19).runWith(targets);
    }

    @Test
    @DisplayName("multiple shards to a single target in a single-threaded env")
    public void singleTarget_manyShards_singleThread() {
        changeShardCountTo(12);
        ImmutableSet<String> aTarget = singleTarget();
        new ThreadSimulator(1).runWith(aTarget);
    }

    @Test
    @DisplayName("a single shard to a single target in a single-threaded env")
    public void singleTarget_singleShard_singleThread() {
        changeShardCountTo(1);
        ImmutableSet<String> aTarget = singleTarget();
        new ThreadSimulator(1).runWith(aTarget);
    }

    @Test
    @DisplayName("a single shard to mutiple targets in a single-threaded env")
    public void manyTargets_singleShard_singleThread() {
        changeShardCountTo(1);
        ImmutableSet<String> targets = manyTargets(11);
        new ThreadSimulator(1).runWith(targets);
    }

    @Test
    @DisplayName("multiple shards to multiple targets in a single-threaded env")
    public void manyTargets_manyShards_singleThread() {
        changeShardCountTo(2019);
        ImmutableSet<String> targets = manyTargets(13);
        new ThreadSimulator(1).runWith(targets);
    }

    @Test
    @DisplayName("multiple shards to multiple targets " +
            "in a multi-threaded env with the custom strategy")
    public void with_custom_strategy() {

        FixedShardStrategy strategy = new FixedShardStrategy(13);
        Delivery newDelivery = Delivery.localWithStrategyAndWindow(strategy, Durations.ZERO);
        ShardIndexMemoizer memoizer = new ShardIndexMemoizer();
        newDelivery.subscribe(memoizer);
        ServerEnvironment.instance()
                         .configureDelivery(newDelivery);

        ImmutableSet<String> targets = manyTargets(7);
        new ThreadSimulator(5, false).runWith(targets);

        ImmutableSet<ShardIndex> shards = memoizer.shards();
        assertThat(shards.size())
                .isEqualTo(1);
        assertThat(shards.iterator()
                         .next())
                .isEqualTo(strategy.nonEmptyShard());
    }

    @Test
    @DisplayName("multiple shards and " +
            "keep them as `TO_DELIVER` right after they are written to `InboxStorage`, " +
            "and mark every as `DELIVERED` after they are actually delivered.")
    @SuppressWarnings("MethodWithMultipleLoops")    // Traversing over the storage.
    public void mark_delivered() {

        FixedShardStrategy strategy = new FixedShardStrategy(3);

        // Set a very long window to keep the messages non-deleted from the `InboxStorage`.
        Delivery newDelivery = Delivery.localWithStrategyAndWindow(strategy, Durations.fromDays(1));
        RawMessageMemoizer memoizer = new RawMessageMemoizer();
        newDelivery.subscribe(memoizer);
        ServerEnvironment.instance()
                         .configureDelivery(newDelivery);

        ImmutableSet<String> targets = manyTargets(6);
        new ThreadSimulator(3, false).runWith(targets);

        // Check that each message was in `TO_DELIVER` status upon writing to the storage.
        ImmutableSet<InboxMessage> rawMessages = memoizer.messages();
        for (InboxMessage message : rawMessages) {
            assertThat(message.getStatus()).isEqualTo(InboxMessageStatus.TO_DELIVER);
        }

        ImmutableMap<ShardIndex, Page<InboxMessage>> contents = inboxContents();
        for (Page<InboxMessage> page : contents.values()) {
            ImmutableList<InboxMessage> messages = page.contents();
            for (InboxMessage message : messages) {
                assertThat(message.getStatus()).isEqualTo(InboxMessageStatus.DELIVERED);
            }
        }
    }

    private static ImmutableMap<ShardIndex, Page<InboxMessage>> inboxContents() {
        Delivery delivery = ServerEnvironment.instance()
                                             .delivery();
        InboxStorage storage = delivery.storage();
        int shardCount = delivery.shardCount();
        ImmutableMap.Builder<ShardIndex, Page<InboxMessage>> builder =
                ImmutableMap.builder();
        for (int shardIndex = 0; shardIndex < shardCount; shardIndex++) {
            ShardIndex index =
                    ShardIndex.newBuilder()
                              .setIndex(shardIndex)
                              .setOfTotal(shardCount)
                              .vBuild();
            Page<InboxMessage> page = with(TenantId.getDefaultInstance())
                    .evaluate(() -> storage.readAll(index));

            builder.put(index, page);
        }

        return builder.build();
    }

    /*
     * Test environment.
     *
     * <p>Accesses the {@linkplain Delivery Delivery API} which has been made
     * package-private and marked as visible for testing. Therefore the test environment routines
     * aren't moved to a separate {@code ...TestEnv} class. Otherwise the test-only API
     * of {@code Delivery} must have been made {@code public}, which wouldn't be
     * a good API design move.
     ******************************************************************************/

    /**
     * Posts addendum commands to instances of {@link CalcAggregate} in a selected number of threads
     * and verifies that each of the targets calculated a proper sum.
     */
    private static class ThreadSimulator {

        private final int threadCount;
        private final boolean shouldInboxBeEmpty;

        private ThreadSimulator(int threadCount) {
            this(threadCount, true);
        }

        private ThreadSimulator(int threadCount, boolean shouldInboxBeEmpty) {
            this.threadCount = threadCount;
            this.shouldInboxBeEmpty = shouldInboxBeEmpty;
        }

        /**
         * Generates some number of commands and events and delivers them to the specified
         * {@linkplain CalcAggregate target entities} via the selected number of threads.
         *
         * @param targets
         *         the identifiers of target entities
         */
        private void runWith(Set<String> targets) {
            BlackBoxBoundedContext context =
                    BlackBoxBoundedContext.singleTenant()
                                          .with(new CalculatorRepository());

            SignalMemoizer memoizer = subscribeToDelivered();

            int streamSize = targets.size() * 30;

            Iterator<String> targetsIterator = Iterators.cycle(targets);
            List<AddNumber> commands = commands(streamSize, targetsIterator);
            List<NumberImported> importEvents = eventsToImport(streamSize, targetsIterator);
            List<NumberReacted> reactEvents = eventsToReact(streamSize, targetsIterator);

            postAsync(context, commands, importEvents, reactEvents);

            Stream<CalculatorSignal> signals = concat(commands.stream(),
                                                      importEvents.stream(),
                                                      reactEvents.stream());

            Map<String, List<CalculatorSignal>> signalsPerTarget =
                    signals.collect(groupingBy(CalculatorSignal::getCalculatorId));

            ImmutableSet<SerializableMessage> receivedMessages = memoizer.messages();

            for (String calcId : signalsPerTarget.keySet()) {

                List<CalculatorSignal> targetSignals = signalsPerTarget.get(calcId);
                assertTrue(receivedMessages.containsAll(targetSignals));

                Integer sumForTarget = targetSignals.stream()
                                                    .map(CalculatorSignal::getValue)
                                                    .reduce(0, (n1, n2) -> n1 + n2);
                Calc expectedState = Calc.newBuilder()
                                         .setSum(sumForTarget)
                                         .build();
                context.assertEntity(CalcAggregate.class, calcId)
                       .hasStateThat()
                       .comparingExpectedFieldsOnly()
                       .isEqualTo(expectedState);

            }
            ensureInboxesEmpty();
        }

        private static List<NumberReacted> eventsToReact(int streamSize,
                                                         Iterator<String> targetsIterator) {
            IntStream ints = new SecureRandom().ints(streamSize, 3, 500);
            return ints.mapToObj((value) ->
                                         NumberReacted.newBuilder()
                                                      .setCalculatorId(targetsIterator.next())
                                                      .setValue(value)
                                                      .vBuild())
                       .collect(toList());
        }

        private static List<NumberImported> eventsToImport(int streamSize,
                                                           Iterator<String> targetsIterator) {
            IntStream ints = new SecureRandom().ints(streamSize, 18, 100);
            return ints.mapToObj((value) ->
                                         NumberImported.newBuilder()
                                                       .setCalculatorId(targetsIterator.next())
                                                       .setValue(value)
                                                       .vBuild())
                       .collect(toList());
        }

        private static List<AddNumber> commands(int streamSize, Iterator<String> targetsIterator) {
            IntStream ints = new SecureRandom().ints(streamSize, 42, 200);
            return ints.mapToObj((value) ->
                                         AddNumber.newBuilder()
                                                  .setCalculatorId(targetsIterator.next())
                                                  .setValue(value)
                                                  .vBuild())
                       .collect(toList());
        }

        private static SignalMemoizer subscribeToDelivered() {
            SignalMemoizer observer = new SignalMemoizer();
            ServerEnvironment.instance()
                             .delivery()
                             .subscribe(observer);
            return observer;
        }

        private void ensureInboxesEmpty() {
            if (shouldInboxBeEmpty) {
                ImmutableMap<ShardIndex, Page<InboxMessage>> shardedItems = inboxContents();

                for (ShardIndex index : shardedItems.keySet()) {
                    Page<InboxMessage> page = shardedItems.get(index);
                    assertTrue(page.contents()
                                   .isEmpty());
                    assertFalse(page.next()
                                    .isPresent());
                }
            }
        }

        private void postAsync(BlackBoxBoundedContext context,
                               List<AddNumber> commands,
                               List<NumberImported> eventsToImport,
                               List<NumberReacted> eventsToReact) {

            Stream<Callable<Object>> signalStream =
                    concat(
                            commandCallables(context, commands),
                            importEventCallables(context, eventsToImport),
                            reactEventsCallables(context, eventsToReact)
                    );
            Collection<Callable<Object>> signals = signalStream.collect(toList());
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            try {
                executorService.invokeAll(signals);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                executorService.shutdown();
            }
        }

        private static Stream<Callable<Object>> commandCallables(BlackBoxBoundedContext context,
                                                                 List<AddNumber> commands) {
            return commands.stream()
                           .map((c) -> () -> {
                               context.receivesCommand(c);
                               return new Object();
                           });
        }

        private static Stream<Callable<Object>> importEventCallables(BlackBoxBoundedContext context,
                                                                     List<NumberImported> events) {
            return events.stream()
                         .map((e) -> () -> {
                             context.importsEvent(e);
                             return new Object();
                         });
        }

        private static Stream<Callable<Object>> reactEventsCallables(BlackBoxBoundedContext context,
                                                                     List<NumberReacted> events) {
            return events.stream()
                         .map((e) -> () -> {
                             context.receivesEvent(e);
                             return new Object();
                         });
        }
    }

    private static void changeShardCountTo(int shards) {
        Delivery newDelivery = Delivery.localWithShardsAndWindow(shards, Durations.ZERO);
        ServerEnvironment.instance()
                         .configureDelivery(newDelivery);
    }
}