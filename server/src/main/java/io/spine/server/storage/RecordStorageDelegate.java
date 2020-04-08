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

import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.client.ResponseFormat;

import java.util.Iterator;
import java.util.Optional;

/**
 * A {@link RecordStorage} which delegates all of its operations to another instance
 * of {@code RecordStorage}.
 *
 * @param <I>
 *         the type of the record identifiers
 * @param <R>
 *         the type of the message records
 */
@Internal
public abstract class RecordStorageDelegate<I, R extends Message> extends RecordStorage<I, R> {

    private final RecordStorage<I, R> delegate;

    protected RecordStorageDelegate(RecordStorage<I, R> delegate) {
        super(delegate.columns(), delegate.isMultitenant());
        this.delegate = delegate;
    }

    @Override
    public Optional<R> read(I id) {
        return delegate.read(id);
    }

    @Override
    public Optional<R> read(I id, FieldMask mask) {
        return delegate.read(id, mask);
    }

    @Override
    public Iterator<R> readAll(MessageQuery<I> query) {
        return delegate.readAll(query);
    }

    @Override
    public Iterator<R> readAll() {
        return delegate.readAll();
    }

    @Override
    public Iterator<R> readAll(Iterable<I> ids) {
        return delegate.readAll(ids);
    }

    @Override
    public Iterator<R> readAll(Iterable<I> ids, FieldMask mask) {
        return delegate.readAll(ids, mask);
    }

    @Override
    public Iterator<R> readAll(ResponseFormat format) {
        return delegate.readAll(format);
    }

    @Override
    public Iterator<R> readAll(MessageQuery<I> query, ResponseFormat format) {
        return delegate.readAll(query, format);
    }

    @Override
    public void write(MessageWithColumns<I, R> record) {
        delegate.write(record);
    }

    @Override
    public synchronized void write(I id, R record) {
        delegate.write(id, record);
    }

    @Override
    public void writeAll(Iterable<? extends MessageWithColumns<I, R>> records) {
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
    @Internal
    protected Columns<R> columns() {
        return delegate.columns();
    }

    @Override
    public boolean isMultitenant() {
        return delegate.isMultitenant();
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
    protected void checkNotClosed(String message) throws IllegalStateException {
        delegate.checkNotClosed(message);
    }

    @Override
    protected void checkNotClosed() throws IllegalStateException {
        delegate.checkNotClosed();
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    protected void writeRecord(MessageWithColumns<I, R> record) {
        delegate.writeRecord(record);
    }

    @Override
    @Internal
    protected void writeAllRecords(Iterable<? extends MessageWithColumns<I, R>> records) {
        delegate.writeAllRecords(records);
    }

    @Override
    protected Iterator<R> readAllRecords(MessageQuery<I> query, ResponseFormat format) {
        return delegate.readAllRecords(query, format);
    }

    @Override
    protected boolean deleteRecord(I id) {
        return delegate.deleteRecord(id);
    }
}
