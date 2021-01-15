/*
 * Copyright 2021, TeamDev. All rights reserved.
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

package io.spine.server.aggregate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import io.spine.core.Event;
import io.spine.core.Version;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.ContextSpec;
import io.spine.server.ServerEnvironment;
import io.spine.server.aggregate.AggregateStorageTest.TestAggregate;
import io.spine.server.aggregate.given.fibonacci.FibonacciRepository;
import io.spine.server.aggregate.given.fibonacci.Sequence;
import io.spine.server.aggregate.given.fibonacci.SequenceId;
import io.spine.server.aggregate.given.fibonacci.command.MoveSequence;
import io.spine.server.aggregate.given.fibonacci.command.SetStartingNumbers;
import io.spine.server.aggregate.given.fibonacci.event.StartingNumbersSet;
import io.spine.test.aggregate.AggProject;
import io.spine.test.aggregate.ProjectId;
import io.spine.testdata.Sample;
import io.spine.testing.server.TestEventFactory;
import io.spine.testing.server.blackbox.BlackBox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.google.protobuf.util.Timestamps.add;
import static com.google.protobuf.util.Timestamps.subtract;
import static io.spine.base.Identifier.newUuid;
import static io.spine.base.Time.currentTime;
import static io.spine.core.Versions.increment;
import static io.spine.core.Versions.zero;
import static io.spine.protobuf.Durations2.seconds;
import static io.spine.server.aggregate.given.aggregate.AggregateTestEnv.event;
import static io.spine.server.aggregate.given.fibonacci.FibonacciAggregate.lastNumberOne;
import static io.spine.server.aggregate.given.fibonacci.FibonacciAggregate.lastNumberTwo;
import static io.spine.testing.server.TestEventFactory.newInstance;
import static java.lang.Integer.MAX_VALUE;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests the truncation of an Aggregate history.
 *
 * <p>Wishing to customize the storage for these tests, descendants may configure it via:
 * <pre>
 *     ServerEnvironment.when(Tests.class)
 *                      .use(customStorageFactory);
 * </pre>
 *
 * <p>Please note that for the test name to make sense the descendants should have some
 * meaningful display names, e.g. {@code "InMemoryAggregateStorage"}.
 */
@SuppressWarnings("AbstractClassWithoutAbstractMethods") // designed for the various storage impls.
public abstract class AggregateHistoryTruncationTest {

    private static final SequenceId ID = SequenceId
            .newBuilder()
            .setValue(newUuid())
            .vBuild();

    @Nested
    @DisplayName("after the history truncation should ")
    class VerifyIntegrity {

        @Test
        @DisplayName("restore the `Aggregate` state properly")
        void restoreAggregateState() {
            FibonacciRepository repo = new FibonacciRepository();
            BlackBox context = BlackBox.from(
                    BoundedContextBuilder.assumingTests()
                                         .add(repo)
            );

            // Set the starting numbers.
            SetStartingNumbers setStartingNumbers = SetStartingNumbers
                    .newBuilder()
                    .setId(ID)
                    .setNumberOne(0)
                    .setNumberTwo(1)
                    .vBuild();
            context.receivesCommand(setStartingNumbers)
                   .assertEvents()
                   .withType(StartingNumbersSet.class)
                   .hasSize(1);
            // Send a lot of `MoveSequence` events, so several snapshots are created.
            MoveSequence moveSequence = MoveSequence
                    .newBuilder()
                    .setId(ID)
                    .vBuild();
            int snapshotTrigger = repo.snapshotTrigger();
            for (int i = 0; i < snapshotTrigger * 5 + 1; i++) {
                context.receivesCommand(moveSequence);
            }

            // Compare against the numbers calculated by hand.
            int expectedNumberOne = 121393;
            int expectedNumberTwo = 196418;
            assertThat(lastNumberOne())
                    .isEqualTo(expectedNumberOne);
            assertThat(lastNumberTwo())
                    .isEqualTo(expectedNumberTwo);

            // Truncate the storage.
            AggregateStorage<SequenceId, Sequence> storage = repo.aggregateStorage();
            int countBeforeTruncate = recordCount(storage);
            assertThat(countBeforeTruncate)
                    .isGreaterThan(snapshotTrigger);
            storage.truncateOlderThan(0);
            int countAfterTruncate = recordCount(storage);
            assertThat(countAfterTruncate)
                    .isAtMost(snapshotTrigger);

            // Run one more command and check the result.
            int expectedNext = lastNumberOne() + lastNumberTwo();
            context.receivesCommand(moveSequence);
            assertThat(lastNumberTwo())
                    .isEqualTo(expectedNext);
        }

        private int recordCount(AggregateStorage<SequenceId, Sequence> storage) {
            Iterator<AggregateEventRecord> iterator = storage.historyBackward(ID,
                                                                              Integer.MAX_VALUE);
            return Iterators.size(iterator);
        }
    }

    @Nested
    @DisplayName("should truncate the history of an `Aggregate` instance")
    class Truncate {

        private final ProjectId id = Sample.messageOfType(ProjectId.class);
        private final TestEventFactory eventFactory = newInstance(AggregateStorageTest.class);

        private Version currentVersion;
        private AggregateStorage<ProjectId, AggProject> storage;

        @BeforeEach
        void setUp() {
            currentVersion = zero();

            ContextSpec spec = ContextSpec.singleTenant("Aggregate truncation tests");
            storage = ServerEnvironment.instance()
                                       .storageFactory()
                                       .createAggregateStorage(spec, TestAggregate.class);
        }

        @Test
        @DisplayName("to the Nth latest snapshot")
        void toTheNthSnapshot() {
            writeSnapshot();
            writeEvent();
            Snapshot latestSnapshot = writeSnapshot();

            int snapshotIndex = 0;
            storage.truncateOlderThan(snapshotIndex);

            List<AggregateEventRecord> records = historyBackward();
            assertThat(records)
                    .hasSize(1);
            assertThat(records.get(0)
                              .getSnapshot())
                    .isEqualTo(latestSnapshot);
        }

        @Test
        @DisplayName("by date")
        void byDate() {
            Duration delta = seconds(10);
            Timestamp now = currentTime();
            Timestamp before = subtract(now, delta);
            Timestamp after = add(now, delta);

            writeSnapshot(before);
            writeEvent(before);
            Event latestEvent = writeEvent(after);
            Snapshot latestSnapshot = writeSnapshot(after);

            int snapshotIndex = 0;
            storage.truncateOlderThan(snapshotIndex, now);

            List<AggregateEventRecord> records = historyBackward();
            assertThat(records)
                    .hasSize(2);
            assertThat(records.get(0)
                              .getSnapshot())
                    .isEqualTo(latestSnapshot);
            assertThat(records.get(1)
                              .getEvent())
                    .isEqualTo(latestEvent);
        }

        @Test
        @DisplayName("by date preserving at least the Nth latest snapshot")
        void byDateAndSnapshot() {
            Duration delta = seconds(10);
            Timestamp now = currentTime();
            Timestamp before = subtract(now, delta);
            Timestamp after = add(now, delta);

            writeEvent(before);
            Snapshot snapshot1 = writeSnapshot(before);
            Event event1 = writeEvent(before);
            Event event2 = writeEvent(after);
            Snapshot snapshot2 = writeSnapshot(after);

            int snapshotIndex = 1;
            storage.truncateOlderThan(snapshotIndex, now);

            // The `event1` should be preserved event though it occurred before the specified date.
            List<AggregateEventRecord> records = historyBackward();
            assertThat(records).hasSize(4);
            assertThat(records.get(0)
                              .getSnapshot())
                    .isEqualTo(snapshot2);
            assertThat(records.get(1)
                              .getEvent())
                    .isEqualTo(event2);
            assertThat(records.get(2)
                              .getEvent())
                    .isEqualTo(event1);
            assertThat(records.get(3)
                              .getSnapshot())
                    .isEqualTo(snapshot1);
        }

        @Test
        @DisplayName("with an `IllegalArgumentException` thrown in case " +
                "an incorrect snapshot index is specified for truncate operation")
        void throwIaeOnInvalidTruncate() {
            assertThrows(IllegalArgumentException.class, () -> storage.truncateOlderThan(-1));
            assertThrows(IllegalArgumentException.class,
                         () -> storage.truncateOlderThan(-2, Timestamp.getDefaultInstance()));
        }

        private ImmutableList<AggregateEventRecord> historyBackward() {
            Iterator<AggregateEventRecord> iterator = storage.historyBackward(id, MAX_VALUE);
            return ImmutableList.copyOf(iterator);
        }

        @CanIgnoreReturnValue
        private Snapshot writeSnapshot() {
            return writeSnapshot(Timestamp.getDefaultInstance());
        }

        @CanIgnoreReturnValue
        private Snapshot writeSnapshot(Timestamp atTime) {
            currentVersion = increment(currentVersion);
            Snapshot snapshot = Snapshot
                    .newBuilder()
                    .setTimestamp(atTime)
                    .setVersion(currentVersion)
                    .build();
            storage.writeSnapshot(id, snapshot);
            return snapshot;
        }

        @CanIgnoreReturnValue
        private Event writeEvent() {
            return writeEvent(Timestamp.getDefaultInstance());
        }

        @CanIgnoreReturnValue
        private Event writeEvent(Timestamp atTime) {
            currentVersion = increment(currentVersion);
            AggProject state = AggProject.getDefaultInstance();
            Event event = eventFactory.createEvent(event(state), currentVersion, atTime);
            storage.writeEvent(id, event);
            return event;
        }
    }
}
