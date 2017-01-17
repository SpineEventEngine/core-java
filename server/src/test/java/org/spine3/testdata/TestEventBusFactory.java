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

package org.spine3.testdata;

import com.google.common.util.concurrent.MoreExecutors;
import org.spine3.server.event.EventBus;
import org.spine3.server.event.EventStorage;
import org.spine3.server.event.EventStore;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.storage.memory.InMemoryStorageFactory;

/**
 * Creates {@link org.spine3.server.command.CommandBus}s for tests.
 *
 * @author Andrey Lavrov
 */

@SuppressWarnings("UtilityClass")
public class TestEventBusFactory {

    private TestEventBusFactory() {
    }

    public static EventBus create() {
        final EventStorage storage = InMemoryStorageFactory.getInstance()
                                                           .createEventStorage();
        final EventStore store = EventStore.newBuilder()
                                           .setStreamExecutor(MoreExecutors.directExecutor())
                                           .setStorage(storage)
                                           .build();
        final EventBus eventBus = EventBus.newBuilder()
                                          .setEventStore(store)
                                          .build();
        return eventBus;
    }

    /** Creates a new event bus with the given storage factory. */
    public static EventBus create(StorageFactory storageFactory) {
        final EventStore store = EventStore.newBuilder()
                                           .setStreamExecutor(MoreExecutors.directExecutor())
                                           .setStorage(storageFactory.createEventStorage())
                                           .build();
        final EventBus eventBus = EventBus.newBuilder()
                                          .setEventStore(store)
                                          .build();
        return eventBus;
    }
}
