/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.storage;

import com.google.common.base.Throwables;
import com.google.protobuf.Message;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.spine3.test.Tests;

import static com.google.common.base.Throwables.propagate;
import static org.junit.Assert.*;

/**
 * Abstract storage tests.
 *
 * @param <I> the type of IDs of storage records
 * @param <R> the type of records kept in the storage
 * @author Alexander Litus
 */
@SuppressWarnings({"InstanceMethodNamingConvention", "ClassWithTooManyMethods"})
public abstract class AbstractStorageShould<I, R extends Message> {

    private AbstractStorage<I, R> storage;

    @Before
    public void setUpAbstractStorageTest() {
        storage = getStorage();
    }

    @After
    public void tearDownAbstractStorageTest() {
        close(storage);
    }

    /**
     * Used to initialize the storage before each test.
     *
     * <p>NOTE: the storage is closed after each test.
     *
     * @return an empty storage instance
     * @see AbstractStorage#close()
     */
    protected abstract AbstractStorage<I, R> getStorage();

    /** Creates a new storage record. */
    protected abstract R newStorageRecord();

    /** Creates a new unique storage record ID. */
    protected abstract I newId();

    /**
     * Closes the storage and propagates an exception if any occurs.
     *
     * @see Throwables#propagate(Throwable)
     */
    protected void close(AbstractStorage storage) {
        if (storage.isOpen()) {
            try {
                storage.close();
            } catch (Exception e) {
                throw propagate(e);
            }
        }
    }

    /** Closes the storage and fails the test if any exception occurs. */
    protected void closeAndFailIfException(AbstractStorage<I, R> storage) {
        try {
            storage.close();
        } catch (Exception e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
            fail("An unexpected exception: " + e.getClass() + "; " + e.getMessage());
        }
    }

    /** Writes a record, reads it and asserts it is the same as the expected one. */
    protected void writeAndReadRecordTest(I id) {
        final R expected = newStorageRecord();
        storage.write(id, expected);

        final R actual = storage.read(id);

        assertEquals(expected, actual);
    }

    @Test
    public void return_default_record_instance_if_no_record_with_such_id() {
        final R record = storage.read(newId());

        assertEquals(record.getDefaultInstanceForType(), record);
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_read_by_null_id() {
        storage.read(Tests.<I>nullRef());
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_write_by_null_id() {
        storage.write(Tests.<I>nullRef(), newStorageRecord());
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_write_null_record() {
        storage.write(newId(), Tests.<R>nullRef());
    }

    @Test
    public void write_and_read_record() {
        writeAndReadRecordTest(newId());
    }

    @Test
    public void write_and_read_several_records_by_different_ids() {
        writeAndReadRecordTest(newId());
        writeAndReadRecordTest(newId());
        writeAndReadRecordTest(newId());
    }

    @Test
    public void rewrite_record_if_write_by_the_same_id() {
        final I id = newId();
        writeAndReadRecordTest(id);
        writeAndReadRecordTest(id);
    }

    @Test(expected = IllegalStateException.class)
    public void assure_it_is_closed() throws Exception {
        closeAndFailIfException(storage);

        storage.checkNotClosed();
    }

    @Test
    public void not_throw_exception_if_it_is_not_closed_on_check() {
        storage.checkNotClosed();
    }

    @Test
    public void return_true_if_it_is_opened() {
        assertTrue(storage.isOpen());
    }

    @Test
    public void return_false_if_it_not_opened() throws Exception {
        storage.close();

        assertFalse(storage.isOpen());
    }

    @Test
    public void return_true_if_it_is_closed() throws Exception {
        storage.close();

        assertTrue(storage.isClosed());
    }

    @Test
    public void return_false_if_it_not_closed() throws Exception {
        assertFalse(storage.isClosed());
    }

    @Test(expected = IllegalStateException.class)
    public void close_itself_and_throw_exception_if_read_after() throws Exception {
        closeAndFailIfException(storage);

        storage.read(newId());
    }

    @Test(expected = IllegalStateException.class)
    public void close_itself_and_throw_exception_if_write_after() throws Exception {
        closeAndFailIfException(storage);

        storage.write(newId(), newStorageRecord());
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_close_twice() {
        closeAndFailIfException(storage);

        storage.read(newId());
    }
}
