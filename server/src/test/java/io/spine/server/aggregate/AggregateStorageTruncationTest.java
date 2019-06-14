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

package io.spine.server.aggregate;

import com.google.common.collect.Iterators;
import io.spine.server.aggregate.given.fibonacci.FibonacciRepository;
import io.spine.server.aggregate.given.fibonacci.SequenceId;
import io.spine.server.aggregate.given.fibonacci.command.MoveSequence;
import io.spine.server.aggregate.given.fibonacci.command.SetStartingNumbers;
import io.spine.server.aggregate.given.fibonacci.event.StartingNumbersSet;
import io.spine.server.storage.StorageFactory;
import io.spine.testing.server.blackbox.BlackBoxBoundedContext;
import io.spine.testing.server.blackbox.SingleTenantBlackBoxContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Iterator;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.base.Identifier.newUuid;
import static io.spine.server.aggregate.given.fibonacci.FibonacciAggregate.lastNumberOne;
import static io.spine.server.aggregate.given.fibonacci.FibonacciAggregate.lastNumberTwo;
import static io.spine.testing.client.blackbox.Count.once;
import static io.spine.testing.server.blackbox.VerifyEvents.emittedEvent;

/**
 * Verifies the integrity of the aggregate history after the storage truncation happens.
 *
 * <p>Please note, that for the test name to make sense the implementor class should have some
 * meaningful display name like {@code "InMemoryAggregateStorage after truncation should"}.
 */
public abstract class AggregateStorageTruncationTest {

    private static final SequenceId ID = SequenceId
            .newBuilder()
            .setValue(newUuid())
            .vBuild();

    protected abstract StorageFactory storageFactory();

    @Test
    @DisplayName("restore aggregate state properly")
    void restoreAggregateState() {
        FibonacciRepository repo = new FibonacciRepository();
        repo.initStorage(storageFactory());
        SingleTenantBlackBoxContext context = BlackBoxBoundedContext
                .singleTenant()
                .with(repo);

        // Set the starting numbers.
        SetStartingNumbers setStartingNumbers = SetStartingNumbers
                .newBuilder()
                .setId(ID)
                .setNumberOne(0)
                .setNumberTwo(1)
                .vBuild();
        context.receivesCommand(setStartingNumbers)
               .assertThat(emittedEvent(StartingNumbersSet.class, once()));

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
        AggregateStorage<SequenceId> storage = repo.aggregateStorage();
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

    private static int recordCount(AggregateStorage<SequenceId> storage) {
        int batchSize = 1;
        AggregateReadRequest<SequenceId> request = new AggregateReadRequest<>(ID, batchSize);
        Iterator<AggregateEventRecord> iterator = storage.historyBackward(request);
        return Iterators.size(iterator);
    }
}
