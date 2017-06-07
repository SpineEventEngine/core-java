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

import io.spine.base.Event;
import io.spine.server.storage.StorageFactory;
import io.spine.users.TenantId;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.Executor;

/**
 * A locally running {@code EventStore} implementation.
 *
 * @author Alexander Yevsyukov
 */
class LocalEventStore extends EventStore {

    private final ERepository storage;

    LocalEventStore(Executor catchUpExecutor,
                    StorageFactory storageFactory,
                    @Nullable Logger logger) {
        super(catchUpExecutor, logger);

        final ERepository eventRepository = new ERepository();
        eventRepository.initStorage(storageFactory);
        this.storage = eventRepository;
    }

    @Override
    protected void store(Event event) {
        storage.store(event);
    }

    @Override
    protected Iterator<Event> iterator(EventStreamQuery query) {
        return storage.iterator(query);
    }

    /**
     * Notifies all the subscribers and closes the underlying storage.
     *
     * @throws IOException if the attempt to close the storage throws an exception
     */
    @Override
    public void close() throws Exception {
        storage.close();
    }

    /*
     * Beam support
     *****************/

    @Override
    public EventStoreIO.Query query(TenantId tenantId) {
        return EventStoreIO.Query.of(storage.queryFn(tenantId));
    }
}
