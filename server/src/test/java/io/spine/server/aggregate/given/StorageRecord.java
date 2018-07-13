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

import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.spine.core.Event;
import io.spine.server.aggregate.AggregateEventRecord;
import io.spine.server.command.TestEventFactory;
import io.spine.test.aggregate.event.AggProjectCreated;
import io.spine.testdata.Sample;

import static io.spine.server.command.TestEventFactory.newInstance;

/**
 * Utilities for creating test instances of {@link AggregateEventRecord}.
 *
 * @author Alexander Yevsyukov
 */
public class StorageRecord {

    private static final TestEventFactory eventFactory = newInstance(Given.class);

    private StorageRecord() {
    }

    public static AggregateEventRecord create(Timestamp timestamp) {
        Message eventMessage = Sample.messageOfType(AggProjectCreated.class);
        Event event = eventFactory.createEvent(eventMessage);
        AggregateEventRecord.Builder builder
                = AggregateEventRecord.newBuilder()
                                      .setTimestamp(timestamp)
                                      .setEvent(event);
        return builder.build();
    }

    public static AggregateEventRecord create(Timestamp timestamp, Event event) {
        AggregateEventRecord.Builder builder = create(timestamp)
                .toBuilder()
                .setEvent(event);
        return builder.build();
    }
}
