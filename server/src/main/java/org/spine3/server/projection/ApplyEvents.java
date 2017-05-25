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
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.TupleTag;
import org.spine3.base.Event;
import org.spine3.base.Events;
import org.spine3.server.entity.EntityRecord;
import org.spine3.server.entity.RecordBasedRepositoryIO;

import java.util.Collections;
import java.util.List;

/**
 * Applies events to a projection and emits a timestamp of last event as a side output.
 *
 * @author Alexander Yevsyukov
 */
class ApplyEvents<I> extends DoFn<KV<I, Iterable<Event>>, KV<I, EntityRecord>> {

    private static final long serialVersionUID = 0L;
    private final RecordBasedRepositoryIO.ReadFunction<I, ?, ?> loadOrCreate;
    private final TupleTag<Timestamp> timestampTag;

    ApplyEvents(RecordBasedRepositoryIO.ReadFunction<I, ?, ?> loadOrCreate,
                TupleTag<Timestamp> timestampTag) {
        this.loadOrCreate = loadOrCreate;
        this.timestampTag = timestampTag;
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        final I id = c.element()
                      .getKey();
        final List<Event> events = Lists.newArrayList(c.element()
                                                       .getValue());
        Collections.sort(events, Events.eventComparator());

        // Timestamps of all applied events.
        final List<Timestamp> timestamps = Lists.newArrayList();

        @SuppressWarnings("unchecked")
        // the types are preserved since the the function is returned by a projection repo.
        final Projection<I, ?> projection = (Projection<I, ?>) loadOrCreate.apply(id);

        // Apply events
        for (Event event : events) {
            projection.handle(Events.getMessage(event), event.getContext());
            timestamps.add(event.getContext()
                                .getTimestamp());
        }

        // Add the resulting record to output.
        @SuppressWarnings("unchecked") /* OK as the types are preserved since the the function
            is returned by a projection repo. */
        final EntityRecord record = ((Converter<? super Projection<I, ?>, EntityRecord>)
                loadOrCreate.getConverter()).convert(projection);
        c.output(KV.of(id, record));

        // Get the latest event timestamp.
        Collections.sort(timestamps, Timestamps.comparator());
        final Timestamp lastEventTimestamp = timestamps.get(timestamps.size() - 1);

        c.output(timestampTag, lastEventTimestamp);
    }

    /**
     * The class to be used instead of anonymous descendants of {@link TupleTag} required for
     * output tags.
     */
    static class TimestampTupleTag extends TupleTag<Timestamp> {
        private static final long serialVersionUID = 0L;
    }

}
