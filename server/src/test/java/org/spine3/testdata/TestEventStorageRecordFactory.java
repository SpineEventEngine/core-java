/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.testdata;

import com.google.protobuf.Timestamp;
import org.spine3.server.storage.EventStorageRecord;

import static org.spine3.testdata.TestContextFactory.createEventContext;
import static org.spine3.testdata.TestEventFactory.*;

/**
 * Contains EventStorageRecords for tests.
 *
 * @author Alexander Litus
 */
public class TestEventStorageRecordFactory {

    private static final EventStorageRecord PROJECT_CREATED_RECORD = EventStorageRecord.newBuilder()
            .setMessage(projectCreatedEventAny())
            .setEventId("project_created").build();

    private static final EventStorageRecord TASK_ADDED_RECORD = EventStorageRecord.newBuilder()
            .setMessage(taskAddedEventAny())
            .setEventId("task_added").build();

    private static final EventStorageRecord PROJECT_STARTED_RECORD = EventStorageRecord.newBuilder()
            .setMessage(projectStartedEventAny())
            .setEventId("project_started").build();

    private TestEventStorageRecordFactory() {}

    public static EventStorageRecord projectCreated() {
        return PROJECT_CREATED_RECORD;
    }

    public static EventStorageRecord taskAdded() {
        return TASK_ADDED_RECORD;
    }

    public static EventStorageRecord projectStarted() {
        return PROJECT_STARTED_RECORD;
    }

    public static EventStorageRecord projectCreated(Timestamp when) {
        final EventStorageRecord.Builder result = EventStorageRecord.newBuilder()
                .setMessage(projectCreatedEventAny())
                .setEventId("project_created_" + when.getSeconds())
                .setContext(createEventContext(when));
        return result.build();
    }

    public static EventStorageRecord taskAdded(Timestamp when) {
        final EventStorageRecord.Builder result = EventStorageRecord.newBuilder()
                .setMessage(taskAddedEventAny())
                .setEventId("task_added_" + when.getSeconds())
                .setContext(createEventContext(when));
        return result.build();
    }

    public static EventStorageRecord projectStarted(Timestamp when) {
        final EventStorageRecord.Builder result = EventStorageRecord.newBuilder()
                .setMessage(projectStartedEventAny())
                .setEventId("project_started_" + when.getSeconds())
                .setContext(createEventContext(when));
        return result.build();
    }
}
