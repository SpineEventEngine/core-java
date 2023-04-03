/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Duration;
import com.google.protobuf.util.Durations;
import io.spine.server.NodeId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static io.spine.server.delivery.DeliveryStrategy.newIndex;
import static io.spine.server.delivery.given.DeliveryTestEnv.generateNodeId;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static java.util.stream.IntStream.range;

/**
 * An abstract base for {@link ShardedWorkRegistry} tests.
 */
@DisplayName("`ShardedWorkRegistry` should")
public abstract class ShardedWorkRegistryTest {

    /**
     * Creates a new instance of {@code ShardedWorkRegistry}.
     */
    protected abstract ShardedWorkRegistry registry();

    @Test
    @DisplayName("pick up the shard if it is not picked up previously and allow to complete it")
    void testPickUp() {
        ShardedWorkRegistry registry = registry();

        ShardIndex index = newIndex(1, 42);
        NodeId node = generateNodeId();

        PickUpOutcome outcome = registry.pickUp(index, node);
        ShardSessionRecord session = assertSession(outcome, index);

        assertAlreadyPicked(registry.pickUp(index, node), session.getWorker());
        assertAlreadyPicked(registry.pickUp(index, generateNodeId()), session.getWorker());

        registry.release(session);
        PickUpOutcome newOutcome = registry.pickUp(index, generateNodeId());
        assertSession(newOutcome, index);
    }

    @Test
    @DisplayName("release the shards which sessions expired")
    void testReleaseExpired() {
        ShardedWorkRegistry registry = registry();

        int totalShards = 35;

        ImmutableSet<ShardIndex> indexes = pickUp(registry, totalShards, totalShards);
        Iterable<ShardIndex> releasedIndexes =
                registry.releaseExpiredSessions(Durations.fromSeconds(100));
        assertThat(releasedIndexes.iterator()
                                  .hasNext()).isFalse();

        sleepUninterruptibly(ofMillis(101));
        releasedIndexes = registry.releaseExpiredSessions(Durations.fromMillis(100));
        assertThat(releasedIndexes).containsExactlyElementsIn(indexes);

        for (ShardIndex shardIndex : indexes) {
            NodeId anotherNode = generateNodeId();
            PickUpOutcome outcome = registry.pickUp(shardIndex, anotherNode);
            assertSession(outcome, shardIndex);
        }
    }

    @Test
    @DisplayName("release a shard only if it's blocked")
    void testOnlyReleaseBlocked() {
        ShardedWorkRegistry registry = registry();

        int totalShards = 12;

        Duration expirationPeriod = Durations.fromMillis(1);
        pickUp(registry, totalShards, totalShards);
        sleepUninterruptibly(ofSeconds(1));
        registry.releaseExpiredSessions(expirationPeriod);

        // Pick up half of the shards and leave another half empty.
        ImmutableSet<ShardIndex> newIndexes =
                pickUp(registry, totalShards, totalShards / 2);
        sleepUninterruptibly(ofSeconds(1));
        Iterable<ShardIndex> releasedIndexes = registry.releaseExpiredSessions(expirationPeriod);
        assertThat(releasedIndexes).containsExactlyElementsIn(newIndexes);
    }

    private static ImmutableSet<ShardIndex>
    pickUp(ShardedWorkRegistry registry, int outOfTotal, int howMany) {
        ImmutableSet<ShardIndex> indexes = range(1, howMany)
                .mapToObj(i -> {
                    NodeId newNode = generateNodeId();
                    ShardIndex newIndex = newIndex(i, outOfTotal);

                    PickUpOutcome outcome = registry.pickUp(newIndex, newNode);
                    assertSession(outcome, newIndex);
                    return newIndex;
                })
                .collect(toImmutableSet());
        return indexes;
    }

    /**
     * Asserts that the given {@code outcome} indicates a successfully picked shard with
     * the given {@code index}.
     */
    @CanIgnoreReturnValue
    private static ShardSessionRecord assertSession(PickUpOutcome outcome, ShardIndex index) {
        assertThat(outcome.session()).isPresent();
        ShardSessionRecord actualSession = outcome.getSession();
        assertThat(actualSession.getIndex()).isEqualTo(index);
        return actualSession;
    }

    /**
     * Asserts that the given {@code outcome} indicates that shard is already picked by the given
     * {@code expected} worker, and returns the {@code WorkerId} from the {@code outcome}.
     */
    @CanIgnoreReturnValue
    private static WorkerId assertAlreadyPicked(PickUpOutcome outcome, WorkerId expected) {
        assertThat(outcome.alreadyPickedBy()).hasValue(expected);
        return outcome.getAlreadyPickedBy();
    }
}
