/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.server.aggregate.given;

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import io.spine.base.EventMessage;
import io.spine.core.Event;
import io.spine.server.aggregate.AggregateEventRecord;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.event.AggProjectCreated;
import io.spine.testdata.Sample;
import io.spine.testing.server.TestEventFactory;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.protobuf.util.Timestamps.add;
import static io.spine.base.Time.getCurrentTime;
import static io.spine.protobuf.Durations2.seconds;
import static io.spine.server.aggregate.given.Given.EventMessage.projectCreated;
import static io.spine.server.aggregate.given.Given.EventMessage.taskAdded;
import static io.spine.testing.server.TestEventFactory.newInstance;

/**
 * Utilities for creating test instances and sequences of {@link AggregateEventRecord}.
 */
public class StorageRecords {

    private static final TestEventFactory eventFactory = newInstance(Given.class);

    /** Prevents instantiation of this utility class. */
    private StorageRecords() {
    }

    /** Creates new builder for an aggregate event record and sets the passed timestamp. */
    private static AggregateEventRecord.Builder newRecordWith(Timestamp timestamp) {
        return AggregateEventRecord
                .newBuilder()
                .setTimestamp(timestamp);
    }

    /**
     * Creates a sample {@linkplain AggregateEventRecord record} with the passed timestamp.
     */
    public static AggregateEventRecord create(Timestamp timestamp) {
        EventMessage eventMessage = Sample.messageOfType(AggProjectCreated.class);
        Event event = eventFactory.createEvent(eventMessage);
        return newRecordWith(timestamp)
                .setEvent(event)
                .build();
    }

    /**
     * Creates a record with the passed event and timestamp.
     */
    public static AggregateEventRecord create(Timestamp timestamp, Event event) {
        return newRecordWith(timestamp)
                .setEvent(event)
                .build();
    }

    /**
     * Returns several records sorted by timestamp ascending.
     * First record's timestamp is the current time.
     */
    public static List<AggregateEventRecord> sequenceFor(ProjectId id) {
        return sequenceFor(id, getCurrentTime());
    }

    /**
     * Returns several records sorted by timestamp ascending.
     *
     * @param start the timestamp of first record.
     */
    public static List<AggregateEventRecord> sequenceFor(ProjectId id, Timestamp start) {
        Duration delta = seconds(10);
        Timestamp timestamp2 = add(start, delta);
        Timestamp timestamp3 = add(timestamp2, delta);

        TestEventFactory eventFactory = newInstance(Given.class);

        Event e1 = eventFactory.createEvent(projectCreated(id, Given.projectName(id)),
                                            null,
                                            start);
        AggregateEventRecord record1 = create(start, e1);

        Event e2 = eventFactory.createEvent(taskAdded(id), null, timestamp2);
        AggregateEventRecord record2 = create(timestamp2, e2);

        Event e3 = eventFactory.createEvent(Given.EventMessage.projectStarted(id),
                                            null,
                                            timestamp3);
        AggregateEventRecord record3 = create(timestamp3, e3);

        return newArrayList(record1, record2, record3);
    }
}
