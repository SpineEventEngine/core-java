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

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.query.RecordQuery;

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
public abstract class RecordStorageDelegate<I, R extends Message> extends RecordStorage<I, R> {

    private final RecordStorage<I, R> delegate;

    protected RecordStorageDelegate(RecordStorage<I, R> delegate) {
        super(delegate.recordSpec(), delegate.isMultitenant());
        this.delegate = delegate;
    }

    @Override
    public Iterator<I> index(RecordQuery<I, R> query) {
        return delegate.index(query);
    }

    @Override
    public Optional<R> read(I id) {
        return delegate.read(id);
    }

    @Override
    protected Optional<R> read(I id, FieldMask mask) {
        return delegate.read(id, mask);
    }

    @Override
    protected Iterator<R> readAll() {
        return delegate.readAll();
    }

    @Override
    protected Iterator<R> readAll(Iterable<I> ids) {
        return delegate.readAll(ids);
    }

    @Override
    protected Iterator<R> readAll(Iterable<I> ids, FieldMask mask) {
        return delegate.readAll(ids, mask);
    }

    @Override
    protected Iterator<R> readAll(RecordQuery<I, R> query) {
        return delegate.readAll(query);
    }

    @Override
    protected void write(RecordWithColumns<I, R> record) {
        delegate.write(record);
    }

    @Override
    protected void writeAll(Iterable<? extends RecordWithColumns<I, R>> records) {
        delegate.writeAll(records);
    }

    @Override
    public void write(I id, R record) {
        delegate.write(id, record);
    }

    @Override
    protected void writeRecord(RecordWithColumns<I, R> record) {
        delegate.writeRecord(record);
    }

    @Override
    @Internal
    protected void writeAllRecords(Iterable<? extends RecordWithColumns<I, R>> records) {
        delegate.writeAllRecords(records);
    }

    @Override
    @CanIgnoreReturnValue
    protected boolean delete(I id) {
        return delegate.delete(id);
    }

    @Override
    protected void deleteAll(Iterable<I> ids) {
        delegate.deleteAll(ids);
    }

    @Override
    protected RecordQuery<I, R> toQuery(I id) {
        return delegate.toQuery(id);
    }

    @Override
    protected RecordQuery<I, R> toQuery(I id, FieldMask mask) {
        return delegate.toQuery(id, mask);
    }

    @Override
    protected RecordQuery<I, R> toQuery(Iterable<I> ids) {
        return delegate.toQuery(ids);
    }

    @Override
    protected RecordQuery<I, R> toQuery(Iterable<I> ids, FieldMask mask) {
        return delegate.toQuery(ids, mask);
    }

    @Override
    protected RecordQuery<I, R> queryForAll() {
        return delegate.queryForAll();
    }


    @Override
    protected Iterator<R> readAllRecords(RecordQuery<I, R> query) {
        return delegate.readAllRecords(query);
    }

    @Override
    @CanIgnoreReturnValue
    protected boolean deleteRecord(I id) {
        return delegate.deleteRecord(id);
    }

    @Override
    @Internal
    protected RecordSpec<I, R, ?> recordSpec() {
        return delegate.recordSpec();
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

    @Override
    public Iterator<I> index() {
        return delegate.index();
    }
}
