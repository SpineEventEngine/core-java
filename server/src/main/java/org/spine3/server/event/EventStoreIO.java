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

import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.extensions.protobuf.ProtoCoder;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.Values;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.spine3.annotation.SPI;
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
@SPI
public class EventStoreIO {

    private static final Coder<Event> eventCoder = ProtoCoder.of(Event.class);

    private EventStoreIO() {
        // Prevent instantiation of this utility class.
    }

    public static Coder<Event> getEventCoder() {
        return eventCoder;
    }

    /**
     * Abstract base for transformations reading events from {@link EventStore}.
     *
     * @author Alexander Yevsyukov
     */
    public static class Read extends PTransform<PBegin, PCollection<Event>> {
        private static final long serialVersionUID = 0L;
        private final RecordStorageIO.Read<EventId> read;

        Read(RecordStorageIO.Read<EventId> read) {
            this.read = read;
        }

        @Override
        public PCollection<Event> expand(PBegin input) {
            final PCollection<KV<EventId, EntityRecord>> withKeys = input.apply(read);
            final PCollection<EntityRecord> allRecords =
                    withKeys.apply(Values.<EntityRecord>create());
            final PCollection<Event> matching = allRecords.apply(
                    ParDo.of(new UnpackWithTimestamp()));
            return matching;
        }
    }

    /**
     * An {@link RecordStorageIO.UnpackFn UnpackFn} that extracts
     * events and accepts those matching the passed predicate.
     */
    private static class UnpackWithTimestamp extends RecordStorageIO.UnpackFn<Event> {

        private static final long serialVersionUID = 0L;

        @Override
        @ProcessElement
        public void processElement(ProcessContext c) {
            final Event event = doUnpack(c);
            c.outputWithTimestamp(event, toInstant(event.getContext()
                                                        .getTimestamp()));
        }
    }
}
