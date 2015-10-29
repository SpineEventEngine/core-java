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
import org.spine3.server.storage.EntityStorage;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.util.Identifiers.idToString;

/**
 * {@link EntityStorage} implementation based on Google App Engine Datastore.
 *
 * @see DatastoreStorageFactory
 * @see LocalDatastoreStorageFactory
 * @author Alexander Litus
 */
class DsEntityStorage<I, M extends Message> extends EntityStorage<I, M> {

    private final DsStorage<M> datastoreDepository;

    private DsEntityStorage(DsStorage<M> datastoreDepository) {
        this.datastoreDepository = datastoreDepository;
    }

    protected static <I, M extends Message> DsEntityStorage<I, M> newInstance(DsStorage<M> datastoreDepository) {
        return new DsEntityStorage<>(datastoreDepository);
    }

    @Override
    public M read(I id) {

        final String idString = idToString(id);

        final Message message = datastoreDepository.read(idString);

        @SuppressWarnings("unchecked") // save because messages of only this type are written
        final M result = (M) message;
        return result;
    }

    @Override
    public void write(I id, M message) {

        checkNotNull(id);
        checkNotNull(message);

        final String idString = idToString(id);

        datastoreDepository.storeEntity(idString, message);
    }
}
