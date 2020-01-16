/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.server.event.store;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Timestamp;
import io.spine.core.Event;
import io.spine.grpc.MemoizingObserver;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.event.EventStore;
import io.spine.server.event.EventStreamQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("EmptyEventStore should")
class EmptyEventStoreTest {

    @Test
    @DisplayName("do nothing on append(Event)")
    void notAppend() {
        EventStore store = new EmptyEventStore();
        store.append(Event.getDefaultInstance());
        assertEmpty(store);
    }

    @Test
    @DisplayName("do nothing on appendAll(Iterable<Event>)")
    void notAppendAll() {
        EventStore store = new EmptyEventStore();
        store.appendAll(ImmutableList.of(Event.getDefaultInstance()));
        assertEmpty(store);
    }

    @Test
    @DisplayName("be open and close")
    void beOpen() throws Exception {
        EventStore eventStore = new EmptyEventStore();
        assertTrue(eventStore.isOpen());
        eventStore.close();
        assertFalse(eventStore.isOpen());
    }

    @Test
    @DisplayName("register with a context")
    void register() {
        EventStore eventStore = new EmptyEventStore();
        assertFalse(eventStore.isRegistered());
        eventStore.registerWith(BoundedContextBuilder.assumingTests().build());
        assertTrue(eventStore.isRegistered());
    }

    private static void assertEmpty(EventStore store) {
        EventStreamQuery query = EventStreamQuery
                .newBuilder()
                .setAfter(Timestamp.getDefaultInstance())
                .buildPartial();
        MemoizingObserver<Event> observer = memoizingObserver();
        store.read(query, observer);
        assertTrue(observer.isCompleted());
        assertThat(observer.responses()).isEmpty();
    }
}
