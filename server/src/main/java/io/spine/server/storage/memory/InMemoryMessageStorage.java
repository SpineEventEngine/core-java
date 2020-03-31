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

package io.spine.server.storage.memory;

import com.google.protobuf.Message;
import io.spine.client.ResponseFormat;
import io.spine.server.storage.Columns;
import io.spine.server.storage.MessageQuery;
import io.spine.server.storage.MessageStorage;
import io.spine.server.storage.MessageWithColumns;

import java.util.Iterator;

/**
 * An in-memory implementation of {@link MessageStorage}.
 */
public class InMemoryMessageStorage<I, M extends Message> extends MessageStorage<I, M> {

    private final MultitenantStorage<TenantMessages<I, M>> multitenantStorage;

    InMemoryMessageStorage(Columns<M> columns, boolean multitenant) {
        super(columns, multitenant);
        this.multitenantStorage = new MultitenantStorage<TenantMessages<I, M>>(multitenant) {
            @Override
            TenantMessages<I, M> createSlice() {
                return new TenantMessages<>();
            }
        };
    }

    private TenantMessages<I, M> records() {
        return multitenantStorage.currentSlice();
    }

    @Override
    public Iterator<I> index() {
        return records().index();
    }

    @Override
    protected Iterator<M> readAllRecords(MessageQuery<I> query, ResponseFormat format) {
        return records().readAll(query, format);
    }

    @Override
    protected void writeRecord(MessageWithColumns<I, M> record) {
        records().put(record.id(), record);
    }

    @Override
    protected void writeAllRecords(Iterable<? extends MessageWithColumns<I, M>> records) {
        for (MessageWithColumns<I, M> record : records) {
            records().put(record.id(), record);
        }
    }

    @Override
    protected boolean deleteRecord(I id) {
        return records().delete(id);
    }
}
