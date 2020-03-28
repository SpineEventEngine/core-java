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
import io.spine.client.ResponseFormat;

import java.util.Iterator;
import java.util.Optional;

/**
 * A {@code MessageStorage} which delegates all of its operations to another instance
 * of {@code MessageStorage}.
 */
public abstract class MessageStorageDelegate<I, M extends Message> extends MessageStorage<I, M> {

    private final MessageStorage<I, M> delegate;

    protected MessageStorageDelegate(MessageStorage<I, M> delegate) {
        super(delegate.columns(), delegate.isMultitenant());
        this.delegate = delegate;
    }

    @Override
    public Optional<M> read(I id) {
        return delegate.read(id);
    }

    @Override
    public Iterator<M> readAll(ResponseFormat format) {
        return delegate.readAll(format);
    }

    @Override
    public Iterator<M> readAll(MessageQuery<I> query, ResponseFormat format) {
        return delegate.readAll(query, format);
    }

    @Override
    public void write(MessageWithColumns<I, M> record) {
        delegate.write(record);
    }

    @Override
    public void writeAll(Iterable<MessageWithColumns<I, M>> records) {
        delegate.writeAll(records);
    }

    @Override
    public boolean delete(I id) {
        return delegate.delete(id);
    }

    @Override
    public void deleteAll(Iterable<I> ids) {
        delegate.deleteAll(ids);
    }

    @Override
    public Iterator<I> index() {
        return delegate.index();
    }

    @Override
    public boolean isMultitenant() {
        return delegate.isMultitenant();
    }

    @Override
    protected void checkNotClosed(String message) throws IllegalStateException {
        delegate.checkNotClosed(message);
    }

    @Override
    protected void checkNotClosed() throws IllegalStateException {
        delegate.checkNotClosed();
    }

    @Override
    public boolean isOpen() {
        return delegate.isOpen();
    }

    @Override
    public boolean isClosed() {
        return delegate.isClosed();
    }

    @Override
    public void close() {
        delegate.close();
    }
}
