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

import com.google.protobuf.Timestamp;
import org.apache.beam.sdk.transforms.DoFn;
import org.joda.time.Instant;
import org.spine3.annotation.Internal;
import org.spine3.base.Event;

import static com.google.protobuf.util.Timestamps.toMillis;

/**
 * Utilities for working with Apache Beam.
 *
 * @author Alexander Yevsyukov
 */
@Internal
public class BeamUtil {

    private static final DoFn<Event, Event> ASSIGN_EVENT_TIMESTAMP = new AssignTimestamp();

    private BeamUtil() {
        // Prevent instantiation of this utility class.
    }

    public static DoFn<Event, Event> assignEventTimestamp() {
        return ASSIGN_EVENT_TIMESTAMP;
    }

    /**
     * Assigns a timestamp from an {@link Event} to corresponding element of a {@code PCollection}.
     */
    private static class AssignTimestamp extends DoFn<Event, Event> {

        private static final long serialVersionUID = 1L;

        public void processElement(ProcessContext c) {
            // Extract the timestamp from an event we're currently processing.
            final Instant timestamp = toInstant(c.element()
                                                 .getContext()
                                                 .getTimestamp());

            // Use ProcessContext.outputWithTimestamp (rather than
            // ProcessContext.output) to emit the entry with timestamp attached.
            c.outputWithTimestamp(c.element(), timestamp);
        }
    }

    /**
     * Converts Protobuf {@link Timestamp} instance to Joda Time {@link Instant}.
     */
    private static Instant toInstant(Timestamp timestamp) {
        final long millis = toMillis(timestamp);
        return new Instant(millis);
    }
}
