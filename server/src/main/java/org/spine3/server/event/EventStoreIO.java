/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.event;

import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.Values;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.spine3.base.Event;
import org.spine3.base.EventId;
import org.spine3.server.entity.EntityRecord;
import org.spine3.server.storage.RecordStorageIO;

import static org.spine3.server.storage.RecordStorageIO.toInstant;

/**
 * Beam support for I/O operations of {@link EventStore}.
 *
 * @author Alexander Yevsyukov
 */
public class EventStoreIO {

    private EventStoreIO() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Abstract base for transformations reading events from {@link EventStore}.
     *
     * @author Alexander Yevsyukov
     */
    public abstract static class Read extends PTransform<PBegin, PCollection<Event>> {
        private static final long serialVersionUID = 0L;
    }

    /**
     * Reads events matching the passed predicate.
     */
    static class Query extends Read {

        private static final long serialVersionUID = 0L;
        private final RecordStorageIO.Read<EventId> readAll;
        private final EventPredicate predicate;

        Query(RecordStorageIO.Read<EventId> readAll, EventPredicate predicate) {
            this.readAll = readAll;
            this.predicate = predicate;
        }

        @Override
        public PCollection<Event> expand(PBegin input) {
            final PCollection<KV<EventId, EntityRecord>> withKeys = input.apply(readAll);
            final PCollection<EntityRecord> allRecords =
                    withKeys.apply(Values.<EntityRecord>create());
            final PCollection<Event> matching = allRecords.apply(
                    ParDo.of(FilterFn.of(predicate)));
            return matching;
        }
    }

    /**
     * An {@link RecordStorageIO.UnpackFn UnpackFn} that extracts
     * events and accepts those matching the passed predicate.
     */
    public static class FilterFn extends RecordStorageIO.UnpackFn<Event> {

        private static final long serialVersionUID = 0L;
        private final EventPredicate predicate;

        /**
         * Creates a new instance that accepts events matching the passed predicate.
         */
        public static FilterFn of(EventPredicate predicate) {
            return new FilterFn(predicate);
        }

        private FilterFn(EventPredicate predicate) {
            this.predicate = predicate;
        }

        @Override
        @ProcessElement
        public void processElement(ProcessContext c) {
            final Event event = doUnpack(c);
            if (predicate.apply(event)) {
                c.outputWithTimestamp(event, toInstant(event.getContext()
                                                            .getTimestamp()));
            }
        }
    }
}
