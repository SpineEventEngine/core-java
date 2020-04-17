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

import io.spine.server.ServerEnvironment;
import io.spine.server.storage.given.GivenStorageProject;
import io.spine.server.storage.given.StgProjectStorage;
import io.spine.test.storage.StgProject;
import io.spine.test.storage.StgProjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.spine.base.Identifier.newUuid;

/**
 * Tests of the API provided by {@link RecordStorageDelegate} to the descendant classes.
 *
 * <p>Sample storage implementation used in the test is {@link StgProjectStorage}.
 *
 * <p>The aim of this test is to ensure that any storage implementations built on top of
 * the {@code RecordStorageDelegate} is able to utilize the API with the expected results.
 */
//TODO:2020-04-17:alex.tymchenko: complete the test case.
@DisplayName("A `RecordStorageDelegate` descendant should")
public class RecordStorageDelegateTest
        extends AbstractStorageTest<StgProjectId, StgProject, StgProjectStorage> {

    @Override
    protected StgProjectStorage newStorage() {
        StorageFactory factory = ServerEnvironment.instance()
                                                  .storageFactory();
        return new StgProjectStorage(factory, false);
    }

    @Override
    protected StgProject newStorageRecord(StgProjectId id) {
        return GivenStorageProject.newState(id);
    }

    @Override
    protected StgProjectId newId() {
        return StgProjectId.newBuilder()
                           .setId(newUuid())
                           .build();
    }

    @Nested
    @DisplayName("write")
    class Write {

        @Test
        @DisplayName("many records")
        void manyRecords() {
        }
    }

    @Nested
    @DisplayName("read")
    class Read {

        @Test
        @DisplayName("a single record with the particular `FieldMask`")
        void singleRecordWithMask() {}

        @Test
        @DisplayName("all records in the storage")
        void all() {}

        @Test
        @DisplayName("several records by their IDs")
        void allByIds() {}

        @Test
        @DisplayName("several records by their IDs and the `FieldMask`")
        void allByIdsAndMask() {}

        @Test
        @DisplayName("several records according to the `FieldMask`")
        void allByMask() {}


        @Test
        @DisplayName("many records by the `RecordQuery` only")
        void manyRecordsByQueryWithDefaultResponseFormat() {}

        @Test
        @DisplayName("many records by the `RecordQuery` and `ResponseFormat`")
        void manyRecordsByQueryAndResponseFormat() {}
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("a single record by its ID")
        void recordById() {}

        @Test
        @DisplayName("several records by their IDs at once")
        void manyRecordByIds() {}
    }

    @Nested
    @DisplayName("throw an `IllegalStateException` if it is closed and the user invokes")
    class ThrowIseIfClosed {

        @Test
        @DisplayName("`write(record)` method")
        void write(){}

        @Test
        @DisplayName("`writeAll(Iterable)` method")
        void writeAll(){}

        @Test
        @DisplayName("`write(id, record)` method")
        void writeIdRecord(){}

        @Test
        @DisplayName("`read(id, FieldMask)` method")
        void readIdFieldMask(){}

        @Test
        @DisplayName("`readAll()` method")
        void readAll(){}

        @Test
        @DisplayName("`readAll(RecordQuery)` method")
        void readAllByQuery(){}

        @Test
        @DisplayName("`readAll(IDs)` method")
        void readAllByIds(){}

        @Test
        @DisplayName("`readAll(IDs, FieldMask)` method")
        void readAllByIdsAndMask(){}

        @Test
        @DisplayName("`readAll(ResponseFormat)` method")
        void readAllInFormat(){}

        @Test
        @DisplayName("`readAll(RecordQuery, ResponseFormat)` method")
        void readAllByQueryAndFormat(){}

        @Test
        @DisplayName("`delete(ID)` method")
        void delete(){}

        @Test
        @DisplayName("`deleteAll(IDs)` method")
        void deleteAll(){}
    }
}
