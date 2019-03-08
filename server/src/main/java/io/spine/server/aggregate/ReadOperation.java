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

import io.spine.core.Event;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.Deque;
import java.util.Iterator;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.protobuf.TextFormat.shortDebugString;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * Method object for reading {@link AggregateHistory}s.
 *
 * @param <I> the type of aggregate IDs
 */
final class ReadOperation<I> {

    private final AggregateStorage<I> storage;
    private final AggregateReadRequest<I> request;
    private final Deque<Event> history;

    private @MonotonicNonNull Snapshot snapshot = null;

    ReadOperation(AggregateStorage<I> storage, AggregateReadRequest<I> request) {
        storage.checkNotClosed();
        this.storage = storage;
        this.request = checkNotNull(request);
        this.history = newLinkedList();
    }

    Optional<AggregateHistory> perform() {
        Iterator<AggregateEventRecord> historyBackward = storage.historyBackward(request);
        if (!historyBackward.hasNext()) {
            return Optional.empty();
        }

        while (historyBackward.hasNext() && snapshot == null) {
            AggregateEventRecord record = historyBackward.next();
            handleRecord(record);
        }

        AggregateHistory result = buildRecord();
        return Optional.of(result);
    }

    private void handleRecord(AggregateEventRecord record) {
        switch (record.getKindCase()) {
            case EVENT:
                history.addFirst(record.getEvent());
                break;
            case SNAPSHOT:
                snapshot = record.getSnapshot();
                break;
            case KIND_NOT_SET:
            default:
                throw newIllegalStateException("Event or snapshot missing in record: \"%s\"",
                                               shortDebugString(record));
        }
    }

    @SuppressWarnings("CheckReturnValue") // calling builder
    private AggregateHistory buildRecord() {
        AggregateHistory.Builder builder = AggregateHistory.newBuilder();
        if (snapshot != null) {
            builder.setSnapshot(snapshot);
        }
        builder.addAllEvent(history);

        AggregateHistory result = builder.build();
        checkRecord(result);
        return result;
    }

    /**
     * Ensures that the {@link AggregateHistory} is valid.
     *
     * <p>{@link AggregateHistory} is considered valid when one of the following is true:
     * <ul>
     *     <li>{@linkplain AggregateHistory#getSnapshot() snapshot} is not default;
     *     <li>{@linkplain AggregateHistory#getEventList() event list} is not empty.
     * </ul>
     *
     * @param record the record to check
     * @throws IllegalStateException if the {@link AggregateHistory} is not valid
     */
    private static void checkRecord(AggregateHistory record) throws IllegalStateException {
        boolean snapshotIsNotSet = !record.hasSnapshot();
        boolean noEvents = record.getEventList()
                                 .isEmpty();
        if (noEvents && snapshotIsNotSet) {
            throw new IllegalStateException("AggregateStateRecord instance should have either "
                                                    + "snapshot or non-empty event list.");
        }
    }
}
