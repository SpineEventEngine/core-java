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

package org.spine3.server.projection;

import com.google.common.base.Converter;
import com.google.common.collect.Lists;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.join.CoGbkResult;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.TupleTag;
import org.spine3.base.Event;
import org.spine3.base.Events;
import org.spine3.server.entity.EntityRecord;
import org.spine3.server.entity.EntityStorageConverter;

import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Applies events to a projection and emits a timestamp of last event as a side output.
 *
 * @author Alexander Yevsyukov
 */
class ApplyEvents<I> extends DoFn<KV<I, CoGbkResult>, KV<I, EntityRecord>> {

    private static final long serialVersionUID = 0L;

    private final TupleTag<Iterable<Event>> eventsTag;
    private final TupleTag<EntityRecord> entityRecordsTag;
    private final EntityStorageConverter<I, ?, ?> converter;
    private final TupleTag<Timestamp> timestampTag;

    ApplyEvents(TupleTag<Iterable<Event>> eventsTag,
                TupleTag<EntityRecord> entityRecordsTag,
                EntityStorageConverter<I, ?, ?> converter,
                TupleTag<Timestamp> timestampTag) {
        this.eventsTag = eventsTag;
        this.entityRecordsTag = entityRecordsTag;
        this.converter = converter;
        this.timestampTag = timestampTag;
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        final I id = c.element()
                      .getKey();
        final CoGbkResult coGbkResult = c.element()
                                         .getValue();
        final List<Event> events = Lists.newArrayList(coGbkResult.getOnly(eventsTag));
        Collections.sort(events, Events.eventComparator());

        // Timestamps of all applied events.
        final List<Timestamp> timestamps = Lists.newArrayList();

        final EntityRecord entityRecord = coGbkResult.getOnly(entityRecordsTag);
        @SuppressWarnings("unchecked")
        // the types are preserved since the the function is returned by a projection repo.
        final Projection<I, ?, ?> projection = (Projection<I, ?, ?>)
                                                converter.reverse()
                                                         .convert(entityRecord);
        checkNotNull(projection);

        // Apply events
        final ProjectionTransaction<I, ?, ?> tx = ProjectionTransaction.start(projection);
        Projection.play(projection, events);
        tx.commit();
        for (Event event : events) {
            timestamps.add(event.getContext()
                                .getTimestamp());
        }

        // Add the resulting record to output.
        @SuppressWarnings("unchecked") /* OK as the types are preserved since the the function
            is returned by a projection repo. */
        final EntityRecord record = ((Converter<? super Projection<I, ?, ?>, EntityRecord>)
                                     converter).convert(projection);
        c.output(KV.of(id, record));

        // Get the latest event timestamp.
        Collections.sort(timestamps, Timestamps.comparator());
        final Timestamp lastEventTimestamp = timestamps.get(timestamps.size() - 1);

        c.output(timestampTag, lastEventTimestamp);
    }

    /**
     * The class to be used instead of anonymous descendant of {@link TupleTag} required for main
     * output of {@link ApplyEvents}.
     */
    static class RecordsTag<I> extends TupleTag<KV<I, EntityRecord>> {
        private static final long serialVersionUID = 0L;
    }

    /**
     * The class to be used instead of anonymous descendants of {@link TupleTag} required for
     * output tags.
     */
    static class TimestampTupleTag extends TupleTag<Timestamp> {
        private static final long serialVersionUID = 0L;
    }
}
