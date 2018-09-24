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

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;
import io.spine.server.entity.Entity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.truth.Truth.assertThat;
import static io.spine.testing.Tests.nullRef;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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
@SuppressWarnings({"ClassWithTooManyMethods",
        "unused" /* JUnit nested classes considered unused in the abstract class. */,
        "DuplicateStringLiteralInspection" /* Common test display names. */})
public abstract class AbstractStorageTest<I,
                                          M extends Message,
                                          R extends ReadRequest<I>,
                                          S extends AbstractStorage<I, M, R>> {

    private S storage;

    @BeforeEach
    protected void setUpAbstractStorageTest() {
        storage = newStorage(getTestEntityClass());
    }

    @AfterEach
    protected void tearDownAbstractStorageTest() {
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
    private void closeAndFailIfException(AbstractStorage<I, M, R> storage) {
        try {
            storage.close();
        } catch (Exception e) {
            e.printStackTrace();
            fail("An unexpected exception: " + e.getClass() + "; " + e.getMessage());
        }
    }

    /** Writes a record, reads it and asserts it is the same as the expected one. */
    @SuppressWarnings("OptionalGetWithoutIsPresent") // We do check.
    private void writeAndReadRecordTest(I id) {
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
    @DisplayName("handle absence of record with passed ID")
    void handleAbsenceOfRecord() {
        R readRequest = newReadRequest(newId());
        Optional<M> record = storage.read(readRequest);

        assertResultForMissingId(record);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType") // This is the purpose of the method.
    private void assertResultForMissingId(Optional<M> record) {
        assertFalse(record.isPresent());
    }

    @Nested
    @DisplayName("throw NPE when")
    class ThrowNpeIf {

        @Test
        @DisplayName("reading by null ID")
        void readByNullId() {
            assertThrows(NullPointerException.class, () -> storage.read(nullRef()));
        }

        @Test
        @DisplayName("writing by null ID")
        void writeByNullId() {
            assertThrows(NullPointerException.class,
                         () -> storage.write(nullRef(), newStorageRecord()));
        }

        @Test
        @DisplayName("writing null record")
        void writeNullRecord() {
            assertThrows(NullPointerException.class, () -> storage.write(newId(), nullRef()));
        }
    }

    @Nested
    @DisplayName("write and read")
    class WriteAndRead {

        @Test
        @DisplayName("one record")
        void record() {
            writeAndReadRecordTest(newId());
        }

        @Test
        @DisplayName("several records by different IDs")
        void severalRecords() {
            writeAndReadRecordTest(newId());
            writeAndReadRecordTest(newId());
            writeAndReadRecordTest(newId());
        }
    }

    /**
     * Tests that the storage overwrites the existing record when storing a new record with the
     * same ID.
     *
     * This test should be overridden by the descendants working with storages which can store
     * multiple records by the same ID.
     */
    @Test
    @DisplayName("re-write record if writing by the same ID")
    protected void rewriteRecord() {
        I id = newId();
        writeAndReadRecordTest(id);
        writeAndReadRecordTest(id);
    }

    @Nested
    @DisplayName("have index which")
    class HaveIndex {

        @Test
        @DisplayName("is non-null")
        void nonNull() {
            Iterator<I> index = storage.index();
            assertNotNull(index);
        }

        @SuppressWarnings("unchecked") // Simplified for test.
        @Test
        @DisplayName("counts all IDs")
        void countingAllIds() {
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
            assertThat(indexValues).containsExactlyElementsIn(ids);
        }

        @Test
        @DisplayName("is immutable")
        void immutable() {
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
    }

    @Nested
    @DisplayName("check storage")
    class CheckStorage {

        @Test
        @DisplayName("not closed")
        void notClosed() {
            storage.checkNotClosed();
        }

        @Test
        @DisplayName("closed")
        void closed() {
            closeAndFailIfException(storage);

            assertThrows(IllegalStateException.class, () -> storage.checkNotClosed());
        }
    }

    @Nested
    @DisplayName("tell if storage is")
    class TellIfStorageIs {

        @Test
        @DisplayName("opened")
        void opened() {
            assertTrue(storage.isOpen());
        }

        @Test
        @DisplayName("not opened")
        void notOpened() {
            storage.close();

            assertFalse(storage.isOpen());
        }

        @Test
        @DisplayName("closed")
        void closed() {
            storage.close();

            assertTrue(storage.isClosed());
        }

        @Test
        @DisplayName("not closed")
        void notClosed() {
            assertFalse(storage.isClosed());
        }
    }

    @Nested
    @DisplayName("close itself and throw ISE on")
    class CloseAndThrowOn {

        @Test
        @DisplayName("read operation")
        void read() {
            closeAndFailIfException(storage);

            R readRequest = newReadRequest(newId());
            assertThrows(IllegalStateException.class, () -> storage.read(readRequest));
        }

        @Test
        @DisplayName("write operation")
        void write() {
            closeAndFailIfException(storage);

            assertThrows(IllegalStateException.class,
                         () -> storage.write(newId(), newStorageRecord()));
        }

        @Test
        @DisplayName("close operation")
        void close() {
            storage.close();
            assertThrows(IllegalStateException.class, () -> storage.close());
        }
    }

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection"/* Storing of generated objects and
                                                               checking via #contains(Object). */)
    @Test
    @DisplayName("return unique ID")
    void returnUniqueID() {
        int checkCount = 10;
        Set<I> ids = newHashSet();
        for (int i = 0; i < checkCount; i++) {
            I newId = newId();
            if (ids.contains(newId)) {
                fail("AbstractStorageTest.newId() should return unique IDs.");
            }
        }
    }
}
