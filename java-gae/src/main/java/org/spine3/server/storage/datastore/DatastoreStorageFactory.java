/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.storage.datastore;

import com.google.protobuf.Message;
import org.spine3.server.Entity;
import org.spine3.server.aggregate.Aggregate;
import org.spine3.server.storage.*;

public class DatastoreStorageFactory implements StorageFactory {
    @Override
    public CommandStorage createCommandStorage() {
        //TODO:2015-09-21:alexander.yevsyukov: Implement
        return null;
    }

    @Override
    public EventStorage createEventStorage() {
        //TODO:2015-09-21:alexander.yevsyukov: Implement
        return null;
    }

    @Override
    public <I> AggregateStorage<I> createAggregateRootStorage(Class<? extends Aggregate<I, ?>> aggregateClass) {
        //TODO:2015-09-21:alexander.yevsyukov: Implement
        return null;
    }

    @Override
    public <I, M extends Message> EntityStorage<I, M> createEntityStorage(Class<? extends Entity<I, M>> entityClass) {
        //TODO:2015-09-21:alexander.yevsyukov: Implement
        return null;
    }
}
