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

package io.spine.server.event;

import io.spine.annotation.SPI;
import io.spine.base.Event;
import io.spine.users.TenantId;
import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.extensions.protobuf.ProtoCoder;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;

import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.storage.RecordStorageIO.toInstant;

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
     * Reads events matching an {@link EventStreamQuery}.
     */
    public static class Query extends PTransform<PCollection<EventStreamQuery>,
                                                 PCollection<Event>> {

        private static final long serialVersionUID = 0L;
        private final DoFn<EventStreamQuery, Event> fn;

        public static Query of(QueryFn fn) {
            checkNotNull(fn);
            final Query result = new Query(fn);
            return result;
        }

        private Query(QueryFn fn) {
            this.fn = fn;
        }

        @Override
        public PCollection<Event> expand(PCollection<EventStreamQuery> input) {
            final PCollection<Event> result = input.apply(ParDo.of(fn));
            return result;
        }
    }

    /**
     * Abstract base for operations reading events matching {@link EventStreamQuery}.
     */
    public abstract static class QueryFn extends DoFn<EventStreamQuery, Event> {

        private static final long serialVersionUID = 0L;
        private final TenantId tenantId;

        protected QueryFn(TenantId tenantId) {
            this.tenantId = tenantId;
        }

        @ProcessElement
        public void processElement(ProcessContext context) {
            final EventStreamQuery query = context.element();
            final Iterator<Event> iterator = read(tenantId, query);
            while (iterator.hasNext()) {
                final Event event = iterator.next();
                context.outputWithTimestamp(event, toInstant(event.getContext()
                                                                  .getTimestamp()));
            }
        }

        protected abstract Iterator<Event> read(TenantId tenantId, EventStreamQuery query);
    }
}
