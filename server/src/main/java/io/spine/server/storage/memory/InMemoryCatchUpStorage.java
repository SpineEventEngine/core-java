/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server.storage.memory;

import io.spine.logging.Logging;
import io.spine.server.catchup.CatchUp;
import io.spine.server.catchup.CatchUpId;
import io.spine.server.delivery.CatchUpReadRequest;
import io.spine.server.delivery.CatchUpStorage;
import io.spine.server.storage.AbstractStorage;
import io.spine.string.Stringifiers;

import java.util.Iterator;
import java.util.Optional;

/**
 * An in-memory implementation of {@code CatchUpStorage}.
 */
public class InMemoryCatchUpStorage extends AbstractStorage<CatchUpId, CatchUp, CatchUpReadRequest>
        implements CatchUpStorage, Logging {

    private final MultitenantStorage<TenantCatchUpRecords> multitenantStorage;

    protected InMemoryCatchUpStorage(boolean multitenant) {
        super(multitenant);
        this.multitenantStorage = new MultitenantStorage<TenantCatchUpRecords>(multitenant) {
            @Override
            TenantCatchUpRecords createSlice() {
                return new TenantCatchUpRecords();
            }
        };
    }

    @Override
    public void write(CatchUp message) {
        System.out.println(" Writing the catch-up state: " + Stringifiers.toString(message));
        multitenantStorage.currentSlice()
                          .put(message.getId(), message);
    }

    @Override
    public Iterable<CatchUp> readAll() {
        return multitenantStorage.currentSlice()
                                 .readAll();
    }

    @Override
    public Iterator<CatchUpId> index() {
        return multitenantStorage.currentSlice()
                                 .index();
    }

    @Override
    public Optional<CatchUp> read(CatchUpReadRequest request) {
        TenantCatchUpRecords records = multitenantStorage.currentSlice();
        return records.get(request.recordId());
    }

    @Override
    public void write(CatchUpId id, CatchUp record) {
        TenantCatchUpRecords records = multitenantStorage.currentSlice();
        records.put(id, record);
    }
}
