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

import com.google.common.collect.ImmutableList;
import io.spine.logging.Logging;
import io.spine.server.delivery.CatchUp;
import io.spine.server.delivery.CatchUpId;
import io.spine.server.delivery.CatchUpReadRequest;
import io.spine.server.delivery.CatchUpStorage;
import io.spine.server.storage.AbstractStorage;
import io.spine.type.TypeUrl;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

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

    @Override
    public Iterable<CatchUp> readByType(TypeUrl projectionType) {
        ImmutableList<CatchUp> allInstances = multitenantStorage.currentSlice()
                                                                .readAll();
        String typeAsString = projectionType.toString();
        List<CatchUp> filtered =
                allInstances.stream()
                            .filter(c -> c.getId()
                                          .getProjectionType()
                                          .equals(typeAsString))
                            .collect(toList());
        return filtered;
    }
}
