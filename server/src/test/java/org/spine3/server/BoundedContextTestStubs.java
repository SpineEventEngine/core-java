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

package org.spine3.server;

import org.spine3.server.command.CommandBus;
import org.spine3.server.event.EventBus;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.storage.memory.InMemoryStorageFactory;

import static org.mockito.Mockito.spy;
import static org.spine3.testdata.TestCommands.newCommandBus;
import static org.spine3.testdata.TestEventFactory.newEventBus;

/**
 * Creates stubs with instances of {@link BoundedContext} for testing purposes.
 *
 * @author Alexander Yevsyukov
 */
public class BoundedContextTestStubs {

    public static BoundedContext create() {
        final StorageFactory storageFactory = InMemoryStorageFactory.getInstance();
        return create(storageFactory);
    }

    public static BoundedContext create(StorageFactory storageFactory) {
        final CommandBus commandBus = newCommandBus(storageFactory);
        final EventBus eventBus = newEventBus(storageFactory);
        final BoundedContext.Builder builder = BoundedContext.newBuilder()
                .setStorageFactory(storageFactory)
                .setCommandBus(spy(commandBus))
                .setEventBus(spy(eventBus));
        return builder.build();
    }

    private BoundedContextTestStubs() {}
}
