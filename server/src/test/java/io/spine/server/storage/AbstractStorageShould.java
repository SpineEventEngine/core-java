/*
 * Copyright 2018, TeamDev. All rights reserved.
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

import com.google.common.base.Optional;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;
import io.spine.server.entity.Entity;
import io.spine.test.Tests;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.rules.ExpectedException;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static io.spine.test.Verify.assertContainsAll;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * An abstract base for test suites testing storages.
 *
 * <p>Manages creation and closing of the {@linkplain #getStorage() storage}
 * for the {@linkplain #getTestEntityClass() test entity class}.
 *
 * <p>In case if the storage for different entity class should be tested,
 * it can be {@linkplain #newStorage(Class) created} manually, but closing of this storage
 * is a responsibility of a caller.
 *
 * <p>All storages should be {@linkplain #close(AbstractStorage) closed} after a test
 * to avoid the issues, which may occur due to unreleased resources.
 *
 * @param <I> the type of IDs of storage records
 * @param <M> the type of records kept in the storage
 * @param <R> the type of read requests for the storage
 * @author Alexander Litus
 */
@SuppressWarnings("ClassWithTooManyMethods")
public abstract class AbstractStorageShould<I,
                                            M extends Message,
                                            R extends ReadRequest<I>,
                                            S extends AbstractStorage<I, M, R>> {

    private S storage;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @BeforeEach
    public void setUpAbstractStorageTest() {
        storage = newStorage(getTestEntityClass());
    }

    @AfterEach
    public void tearDownAbstractStorageTest() {
        close(storage);
    }

    /**
     * Obtains the storage for the {@linkplain #getTestEntityClass() entity class}.
     *
     * @return the storage, which will be closed automatically after a test
     */
    protected final S getStorage() {
        return storage;
    }

    /**
     * Creates the storage for the specified entity class.
     *
     * <p>The resulting storage should be {@linkplain #close(AbstractStorage) closed} manually to
     * release resources, which may be used by the storage.
     *
     * <p>Use {@linkplain #getStorage() existing storage} if the storage may be tested for
     * the {@linkplain #getTestEntityClass() entity class}.
     *
     * @return an empty storage instance
     * @see AbstractStorage#close()
     */
    protected abstract S newStorage(Class<? extends Entity> cls);

    /** Creates a new storage record. */
    protected abstract M newStorageRecord();

    /** Creates a new unique storage record ID. */
    protected abstract I newId();

    /** Creates a new read request with the specified ID. */
    protected abstract R newReadRequest(I id);

    /** Returns the class of the test entity. */
    protected abstract Class<? extends Entity> getTestEntityClass();

    /**
     * Closes the storage and propagates an exception if any occurs.
     */
    protected void close(AbstractStorage storage) {
        if (storage.isOpen()) {
            try {
                storage.close();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /** Closes the storage and fails the test if any exception occurs. */
    @SuppressWarnings("CallToPrintStackTrace")
    protected void closeAndFailIfException(AbstractStorage<I, M, R> storage) {
        try {
            storage.close();
        } catch (Exception e) {
            e.printStackTrace();
            fail("An unexpected exception: " + e.getClass() + "; " + e.getMessage());
        }
    }

    /** Writes a record, reads it and asserts it is the same as the expected one. */
    @SuppressWarnings("OptionalGetWithoutIsPresent") // We do check.
    protected void writeAndReadRecordTest(I id) {
        M expected = writeRecord(id);

        R readRequest = newReadRequest(id);
        Optional<M> actual = storage.read(readRequest);

        assertTrue(actual.isPresent());
        assertEquals(expected, actual.get());
    }

    @CanIgnoreReturnValue
    private M writeRecord(I id) {
        M expected = newStorageRecord();
        storage.write(id, expected);
        return expected;
    }

    @Test
    public void handle_absence_of_record_with_passed_id() {
        R readRequest = newReadRequest(newId());
        Optional<M> record = storage.read(readRequest);

        assertResultForMissingId(record);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType") // This is the purpose of the method.
    protected void assertResultForMissingId(Optional<M> record) {
        assertFalse(record.isPresent());
    }

    @Test
    public void throw_exception_if_read_by_null_id() {
        thrown.expect(NullPointerException.class);
        storage.read(Tests.nullRef());
    }

    @Test
    public void throw_exception_if_write_by_null_id() {
        thrown.expect(NullPointerException.class);
        storage.write(Tests.nullRef(), newStorageRecord());
    }

    @Test
    public void throw_exception_if_write_null_record() {
        thrown.expect(NullPointerException.class);
        storage.write(newId(), Tests.nullRef());
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
        I id = newId();
        writeAndReadRecordTest(id);
        writeAndReadRecordTest(id);
    }

    @Test
    public void have_index_on_ID() {
        Iterator<I> index = storage.index();
        assertNotNull(index);
    }

    @Test
    public void index_all_IDs() {
        int recordCount = 10;
        Set<I> ids = new HashSet<>(recordCount);
        for (int i = 0; i < recordCount; i++) {
            I id = newId();
            writeRecord(id);
            ids.add(id);
        }

        Iterator<I> index = storage.index();
        Collection<I> indexValues = newHashSet(index);

        assertEquals(ids.size(), indexValues.size());
        assertContainsAll(indexValues, (I[]) ids.toArray());
    }

    @Test
    public void have_immutable_index() {
        writeRecord(newId());
        Iterator<I> index = storage.index();
        assertTrue(index.hasNext());
        try {
            index.remove();
            fail("Storage#index is mutable");

            // One of collections used in in-memory implementation throws IllegalStateException
            // but default behavior is UnsupportedOperationException
        } catch (UnsupportedOperationException | IllegalStateException ignored) {
            // One of valid exceptions was thrown
        }
    }

    @Test
    public void assure_it_is_closed() {
        closeAndFailIfException(storage);

        thrown.expect(IllegalStateException.class);
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
    public void return_false_if_it_not_opened() {
        storage.close();

        assertFalse(storage.isOpen());
    }

    @Test
    public void return_true_if_it_is_closed() {
        storage.close();

        assertTrue(storage.isClosed());
    }

    @Test
    public void return_false_if_it_not_closed() {
        assertFalse(storage.isClosed());
    }

    @Test
    public void close_itself_and_throw_exception_if_read_after() {
        closeAndFailIfException(storage);

        R readRequest = newReadRequest(newId());
        thrown.expect(IllegalStateException.class);
        storage.read(readRequest);
    }

    @Test
    public void close_itself_and_throw_exception_if_write_after() {
        closeAndFailIfException(storage);

        thrown.expect(IllegalStateException.class);
        storage.write(newId(), newStorageRecord());
    }

    @Test
    public void throw_exception_if_close_twice() {
        storage.close();
        thrown.expect(IllegalStateException.class);
        storage.close();
    }

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection"/* Storing of generated objects and
                                                               checking via #contains(Object). */)
    @Test
    public void return_unique_ID() {
        int checkCount = 10;
        Set<I> ids = newHashSet();
        for (int i = 0; i < checkCount; i++) {
            I newId = newId();
            if (ids.contains(newId)) {
                fail("AbstractStorageShould.newId() should return unique IDs.");
            }
        }
    }
}
