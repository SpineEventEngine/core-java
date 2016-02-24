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

import com.google.protobuf.Message;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
    public void tearDownAbstractStorageTest() throws Exception {
        if (storage.isOpen()) {
            storage.close();
        }
    }

    /**
     * Used to initialize the storage before each test.
     *
     * @return an empty storage instance
     */
    protected abstract AbstractStorage<I, R> getStorage();

    /**
     * Creates a new storage record.
     */
    protected abstract R newStorageRecord();

    /**
     * Creates a new unique storage record ID.
     */
    protected abstract I newId();

    @Test
    public void return_default_record_instance_if_no_record_with_such_id() {
        final R record = storage.read(newId());

        assertEquals(record.getDefaultInstanceForType(), record);
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_read_by_null_id() {
        //noinspection ConstantConditions
        storage.read(null);
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_write_by_null_id() {
        //noinspection ConstantConditions
        storage.write(null, newStorageRecord());
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_write_null_record() {
        //noinspection ConstantConditions
        storage.write(newId(), null);
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

    private void writeAndReadRecordTest(I id) {
        final R expected = newStorageRecord();
        storage.write(id, expected);

        final R actual = storage.read(id);

        assertEquals(expected, actual);
    }
    
    @Test
    public void throw_exception_if_it_is_closed_on_check() throws Exception {
        storage.close();

        try {
            storage.checkNotClosed();
            fail("An exception must be thrown.");
        } catch (IllegalStateException e) {
            // is OK because it is closed
        }
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

    @Test
    public void close_itself_and_throw_exception_if_read_then() throws Exception {
        storage.close();

        try {
            storage.read(newId());
            fail("An exception must be thrown on attempt to read.");
        } catch (IllegalStateException e) {
            // is OK because storage is closed
        }
    }

    @Test
    public void close_itself_and_throw_exception_if_write_then() throws Exception {
        storage.close();

        try {
            storage.write(newId(), newStorageRecord());
            fail("An exception must be thrown on attempt to write.");
        } catch (IllegalStateException e) {
            // is OK because storage is closed
        }
    }
}
