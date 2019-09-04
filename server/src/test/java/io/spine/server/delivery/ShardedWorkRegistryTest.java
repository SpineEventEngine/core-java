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

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.util.Durations;
import io.spine.server.NodeId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static io.spine.server.delivery.given.DeliveryTestEnv.generateNodeId;
import static io.spine.server.delivery.given.DeliveryTestEnv.newShardIndex;
import static java.util.stream.IntStream.rangeClosed;

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

        ShardIndex index = newShardIndex(1, 42);
        NodeId node = generateNodeId();

        Optional<ShardProcessingSession> session = registry.pickUp(index, node);
        ShardProcessingSession actualSession = assertSession(session, index);

        assertThat(registry.pickUp(index, node))
              .isEmpty();
        assertThat(registry.pickUp(index, generateNodeId()))
              .isEmpty();

        actualSession.complete();
        Optional<ShardProcessingSession> newSession = registry.pickUp(index, generateNodeId());
        assertSession(newSession, index);
    }

    @Test
    @DisplayName("release the shards which sessions expired")
    void testReleaseExpired() {
        ShardedWorkRegistry registry = registry();

        int totalShards = 35;

        ImmutableSet.Builder<ShardIndex> indexBuilder = ImmutableSet.builder();
        rangeClosed(1, totalShards)
                .forEach((i) -> {
                    NodeId newNode = generateNodeId();
                    ShardIndex newIndex = newShardIndex(i, totalShards);

                    Optional<ShardProcessingSession> session = registry.pickUp(newIndex, newNode);
                    assertSession(session, newIndex);
                    indexBuilder.add(newIndex);
                });
        ImmutableSet<ShardIndex> indexes = indexBuilder.build();

        Iterable<ShardIndex> releasedIndexes =
                registry.releaseExpiredSessions(Durations.fromSeconds(100));
        assertThat(releasedIndexes.iterator()
                                  .hasNext()).isFalse();

        sleepUninterruptibly(101, TimeUnit.MILLISECONDS);
        releasedIndexes = registry.releaseExpiredSessions(Durations.fromMillis(100));

        ImmutableSet<ShardIndex> releasedValues = ImmutableSet.<ShardIndex>builder()
                .addAll(releasedIndexes)
                .build();
        assertThat(releasedValues).isEqualTo(indexes);

        for (ShardIndex shardIndex : indexes) {
            NodeId anotherNode = generateNodeId();
            Optional<ShardProcessingSession> newSession = registry.pickUp(shardIndex, anotherNode);
            assertSession(newSession, shardIndex);
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")     // asserting the `Optional`.
    @CanIgnoreReturnValue
    private static ShardProcessingSession
    assertSession(Optional<ShardProcessingSession> session, ShardIndex index) {
        assertThat(session)
              .isPresent();
        ShardProcessingSession actualSession = session.get();
        assertThat(actualSession.shardIndex()).isEqualTo(index);
        return actualSession;
    }
}
