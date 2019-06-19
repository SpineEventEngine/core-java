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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.protobuf.Any;
import com.google.protobuf.util.Durations;
import io.spine.base.CommandMessage;
import io.spine.core.TenantId;
import io.spine.protobuf.AnyPacker;
import io.spine.server.DefaultRepository;
import io.spine.server.ServerEnvironment;
import io.spine.server.delivery.given.CalcAggregate;
import io.spine.test.delivery.AddNumber;
import io.spine.test.delivery.Calc;
import io.spine.testing.server.blackbox.BlackBoxBoundedContext;
import io.spine.validate.Validated;
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
        this.originalDelivery = ServerEnvironment.getInstance()
                                                 .delivery();
    }

    @AfterEach
    public void tearDown() {
        ServerEnvironment.getInstance()
                         .setDelivery(originalDelivery);
    }

    @Test
    @DisplayName("a single shard to a single target in a multi-threaded env")
    public void singleTarget_singleShard_manyThreads() {
        changeShardCountTo(1);
        ImmutableSet<String> aTarget = singleTarget();
        new DeliveryTester(42).run(aTarget);
    }

    @Test
    @DisplayName("a single shard to multiple targets in a multi-threaded env")
    public void manyTargets_singleShard_manyThreads() {
        changeShardCountTo(1);
        ImmutableSet<String> targets = manyTargets(7);
        new DeliveryTester(10).run(targets);
    }

    @Test
    @DisplayName("multiple shards to a single target in a multi-threaded env")
    public void singleTarget_manyShards_manyThreads() {
        changeShardCountTo(1986);
        ImmutableSet<String> targets = singleTarget();
        new DeliveryTester(15).run(targets);
    }

    @Test
    @DisplayName("multiple shards to multiple targets in a multi-threaded env")
    public void manyTargets_manyShards_manyThreads() {
        changeShardCountTo(2004);
        ImmutableSet<String> targets = manyTargets(32);
        new DeliveryTester(19).run(targets);
    }

    @Test
    @DisplayName("multiple shards to a single target in a single-threaded env")
    public void singleTarget_manyShards_singleThread() {
        changeShardCountTo(12);
        ImmutableSet<String> aTarget = singleTarget();
        new DeliveryTester(1).run(aTarget);
    }

    @Test
    @DisplayName("a single shard to a single target in a single-threaded env")
    public void singleTarget_singleShard_singleThread() {
        changeShardCountTo(1);
        ImmutableSet<String> aTarget = singleTarget();
        new DeliveryTester(1).run(aTarget);
    }

    @Test
    @DisplayName("a single shard to mutiple targets in a single-threaded env")
    public void manyTargets_singleShard_singleThread() {
        changeShardCountTo(1);
        ImmutableSet<String> targets = manyTargets(11);
        new DeliveryTester(1).run(targets);
    }

    @Test
    @DisplayName("multiple shards to multiple targets in a single-threaded env")
    public void manyTargets_manyShards_singleThread() {
        changeShardCountTo(2019);
        ImmutableSet<String> targets = manyTargets(13);
        new DeliveryTester(1).run(targets);
    }

    private static ImmutableSet<String> manyTargets(int size) {
        return new SecureRandom().ints(size)
                                 .mapToObj((i) -> "calc-" + i)
                                 .collect(ImmutableSet.toImmutableSet());
    }

    private static ImmutableSet<String> singleTarget() {
        return ImmutableSet.of("the-calculator");
    }

    private static void changeShardCountTo(int shards) {
        Delivery newDelivery = Delivery.localWithShardsAndWindow(shards, Durations.ZERO);
        ServerEnvironment.getInstance()
                         .setDelivery(newDelivery);
    }

    /**
     * Posts addendum commands to instances of {@link CalcAggregate} in a selected number of threads
     * and verifies that each of the targets calculated a proper sum.
     */
    private static class DeliveryTester {

        private final int threadCount;

        protected DeliveryTester(int threadCount) {
            this.threadCount = threadCount;
        }

        private void run(Set<String> targets) {

            BlackBoxBoundedContext context =
                    BlackBoxBoundedContext.singleTenant()
                                          .with(DefaultRepository.of(CalcAggregate.class));

            ImmutableSet.Builder<CommandMessage> observedCommands = subscribeToDelivered();

            int streamSize = targets.size() * 30;
            IntStream ints = new SecureRandom().ints(streamSize, 42, 200);
            Iterator<String> targetsIterator = Iterators.cycle(targets);
            List<AddNumber> commands =
                    ints.mapToObj((value) -> AddNumber.newBuilder()
                                                      .setCalculatorId(targetsIterator.next())
                                                      .setValue(value)
                                                      .vBuild())
                        .collect(toList());
            postAsync(context, commands);

            Map<String, List<AddNumber>> commandsPerTarget =
                    commands.stream()
                            .collect(groupingBy(AddNumber::getCalculatorId));

            ImmutableSet<CommandMessage> receivedCommands = observedCommands.build();

            for (String calcId : commandsPerTarget.keySet()) {

                List<AddNumber> targetCommands = commandsPerTarget.get(calcId);
                assertTrue(receivedCommands.containsAll(targetCommands));

                Integer sumForTarget = targetCommands.stream()
                                                     .map(AddNumber::getValue)
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

        private static ImmutableSet.Builder<CommandMessage> subscribeToDelivered() {
            ImmutableSet.Builder<CommandMessage> observedMessages = ImmutableSet.builder();
            ServerEnvironment.getInstance()
                             .delivery()
                             .subscribe(update -> {
                                 if (update.hasCommand()) {
                                     Any packed = update.getCommand()
                                                        .getMessage();
                                     CommandMessage cmdMessage = (CommandMessage) AnyPacker.unpack(
                                             packed);
                                     observedMessages.add(cmdMessage);
                                 }
                             });
            return observedMessages;
        }

        private static void ensureInboxesEmpty() {
            ImmutableMap<ShardIndex, Page<InboxMessage>> shardedItems = inboxContent();

            for (ShardIndex index : shardedItems.keySet()) {
                Page<InboxMessage> page = shardedItems.get(index);
                assertTrue(page.contents()
                               .isEmpty());
                assertFalse(page.next()
                                .isPresent());
            }
        }

        private void postAsync(BlackBoxBoundedContext context, List<AddNumber> commands) {
            Collection<Callable<Object>> collect =
                    commands.stream()
                            .map((c) -> (Callable<Object>) () -> {
                                context.receivesCommand(c);
                                return new Object();
                            })
                            .collect(toList());
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            try {
                executorService.invokeAll(collect);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                executorService.shutdown();
            }
        }
    }

    private static ImmutableMap<ShardIndex, Page<InboxMessage>> inboxContent() {
        Delivery delivery = ServerEnvironment.getInstance()
                                             .delivery();
        InboxStorage storage = delivery.storage();
        int shardCount = delivery.shardCount();
        ImmutableMap.Builder<ShardIndex, Page<InboxMessage>> builder =
                ImmutableMap.builder();
        for (int shardIndex = 0; shardIndex < shardCount; shardIndex++) {
            @Validated ShardIndex index =
                    ShardIndex.newBuilder()
                              .setIndex(shardIndex)
                              .setOfTotal(shardCount)
                              .vBuild();
            Page<InboxMessage> page =
                    with(TenantId.getDefaultInstance())
                            .evaluate(() -> storage.contentsBackwards(index));

            builder.put(index, page);
        }

        return builder.build();
    }
}