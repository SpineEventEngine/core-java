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

import static io.spine.base.Identifier.newUuid;

/**
 * Tests of the API provided by {@link RecordStorageDelegate} to the descendant classes.
 *
 * <p>Sample storage implementation used in the test is {@link StgProjectStorage}.
 *
 * <p>The aim of this test is to ensure that any storage implementations built on top of
 * the {@code RecordStorageDelegate} is able to utilize the API with the expected results.
 */
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
}
