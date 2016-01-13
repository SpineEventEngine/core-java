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

package org.spine3.server.storage.filesystem;

import org.junit.After;
import org.spine3.server.aggregate.Aggregate;
import org.spine3.server.storage.AggregateStorage;
import org.spine3.server.storage.AggregateStorageShould;
import org.spine3.server.storage.StorageFactory;
import org.spine3.test.project.Project;
import org.spine3.test.project.ProjectId;

import javax.annotation.Nonnull;

/**
 * @author Mikhail Mikhaylov
 */
public class FsAggregateStorageShould extends AggregateStorageShould {

    private static final StorageFactory FACTORY = FileSystemStorageFactory.newInstance(FsAggregateStorageShould.class);

    private static final AggregateStorage<ProjectId> STORAGE = FACTORY.createAggregateStorage(AggregateForStorageTests.class);

    public FsAggregateStorageShould() {
        super(STORAGE);
    }

    @After
    public void tearDownTest() throws Exception {
        FACTORY.close();
    }

    public static class AggregateForStorageTests extends Aggregate<ProjectId, Project> {
        protected AggregateForStorageTests(ProjectId id) {
            super(id);
        }
        @Nonnull
        @Override protected Project getDefaultState() {
            return Project.getDefaultInstance();
        }
    }
}
