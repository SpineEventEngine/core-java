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

package io.spine.server.storage;

import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.client.ResponseFormat;

import java.util.Iterator;

/**
 * @author Alex Tymchenko
 */
public abstract class MessageStorage<I, M extends Message> extends AbstractStorage<I, M>
        implements PlainMessageStorage<I, M> {

    private final Columns<M> columns;

    protected MessageStorage(Columns<M> columns, boolean multitenant) {
        super(multitenant);
        this.columns = columns;
    }

    @Override
    public final synchronized void write(I id, M record) {
        MessageWithColumns<I, M> withCols = MessageWithColumns.create(id, record, this.columns);
        write(withCols);
    }

    @Override
    public final Iterator<M> readAll(MessageQuery<I> query) {
        return readAll(query, ResponseFormat.getDefaultInstance());
    }

    public final Iterator<M> readAll() {
        return readAll(MessageQuery.all());
    }

    @Internal
    public Columns<M> columns() {
        return columns;
    }
}
