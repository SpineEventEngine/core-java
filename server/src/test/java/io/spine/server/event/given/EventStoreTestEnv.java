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

package io.spine.server.event.given;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import io.spine.core.Event;
import io.spine.server.BoundedContext;
import io.spine.server.event.EventStore;
import io.spine.server.event.EventStoreTest;
import io.spine.test.event.ProjectCreated;
import io.spine.test.event.TaskAdded;
import io.spine.testdata.Sample;
import io.spine.testing.server.command.TestEventFactory;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.fail;

public class EventStoreTestEnv {

    private static TestEventFactory eventFactory = null;

    /** Prevents instantiation of this utility class. */
    private EventStoreTestEnv() {
    }

    public static void initEventFactory() {
        eventFactory = TestEventFactory.newInstance(EventStoreTest.class);
    }

    public static EventStore eventStore() {
        final BoundedContext bc = BoundedContext.newBuilder()
                                                .setMultitenant(false)
                                                .build();
        return EventStore.newBuilder()
                         .setStorageFactory(bc.getStorageFactory())
                         .setStreamExecutor(MoreExecutors.directExecutor())
                         .withDefaultLogger()
                         .build();
    }

    public static Event projectCreated(Timestamp when) {
        final ProjectCreated msg = Sample.messageOfType(ProjectCreated.class);
        return eventFactory.createEvent(msg, null, when);
    }

    public static Event taskAdded(Timestamp when) {
        final TaskAdded msg = Sample.messageOfType(TaskAdded.class);
        return eventFactory.createEvent(msg, null, when);
    }

    public static void assertDone(AtomicBoolean done) {
        if (!done.get()) {
            fail("Please use the MoreExecutors.directExecutor in EventStore for tests.");
        }
    }

    public static class ResponseObserver implements StreamObserver<Event> {

        private final Collection<Event> resultStorage;
        private final AtomicBoolean doneFlag;

        public ResponseObserver(Collection<Event> resultStorage, AtomicBoolean doneFlag) {
            this.resultStorage = resultStorage;
            this.doneFlag = doneFlag;
        }

        @Override
        public void onNext(Event value) {
            resultStorage.add(value);
        }

        @Override
        public void onError(Throwable t) {
            fail(t.getMessage());
        }

        @Override
        public void onCompleted() {
            doneFlag.getAndSet(true);
        }
    }
}
