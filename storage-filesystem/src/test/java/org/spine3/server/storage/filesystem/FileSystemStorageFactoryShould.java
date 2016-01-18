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
import org.junit.Test;
import org.spine3.server.Entity;
import org.spine3.server.aggregate.Aggregate;
import org.spine3.server.storage.*;
import org.spine3.test.project.Project;

import javax.annotation.Nonnull;
import java.io.IOException;

import static org.junit.Assert.assertNotNull;

/**
 * @author Mikhail Mikhaylov
 */
@SuppressWarnings("InstanceMethodNamingConvention")
public class FileSystemStorageFactoryShould {

    private static final StorageFactory FACTORY = FileSystemStorageFactory.newInstance(FileSystemStorageFactoryShould.class);

    @After
    public void tearDownTest() throws IOException {
        FACTORY.close();
    }

    @Test
    public void create_storages_successfully() {

        final EventStorage eventStorage = FACTORY.createEventStorage();
        final CommandStorage commandStorage = FACTORY.createCommandStorage();
        final EntityStorage<String> entityStorage = FACTORY.createEntityStorage(TestEntity.class);
        final AggregateStorage<String> aggregateRootStorage = FACTORY.createAggregateStorage(TestAggregateWithIdString.class);

        assertNotNull(eventStorage);
        assertNotNull(commandStorage);
        assertNotNull(entityStorage);
        assertNotNull(aggregateRootStorage);
    }

    public static class TestAggregateWithIdString extends Aggregate<String, Project> {
        protected TestAggregateWithIdString(String id) {
            super(id);
        }
        @Nonnull
        @Override
        protected Project getDefaultState() {
            return Project.getDefaultInstance();
        }
    }

    public static class TestEntity extends Entity<String, Project> {

        private static final Project DEFAULT_STATE = Project.newBuilder().setStatus("default_state").build();

        protected TestEntity(String id) {
            super(id);
        }

        @Nonnull
        @Override
        protected Project getDefaultState() {
            return DEFAULT_STATE;
        }

        @Override
        protected void validate(Project state) throws IllegalStateException {
            super.validate(state);
        }
    }
}
