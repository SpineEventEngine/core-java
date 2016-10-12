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

import com.google.common.base.Supplier;
import com.google.protobuf.FieldMask;
import org.junit.Test;
import org.spine3.protobuf.AnyPacker;
import org.spine3.protobuf.Timestamps;
import org.spine3.test.projection.Project;
import org.spine3.test.projection.ProjectId;
import org.spine3.test.projection.Task;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;
import static org.spine3.test.Verify.assertEmpty;
import static org.spine3.test.Verify.assertSize;

/**
 * @author Dmytro Dashenkov
 */
public abstract class RecordStorageShould {

    @Test
    public void retrieve_empty_map_if_storage_is_empty() {
        final RecordStorage<String> storage = createStorage();

        final FieldMask nonEmptyFieldMask = FieldMask.newBuilder()
                                                     .addPaths("invalid-path")
                                                     .build();

        final Map empty = storage.readAll(nonEmptyFieldMask);
        assertNotNull(empty);
        assertEmpty(empty);
    }

    @Test
    public void retrieve_all_records() {
        final RecordStorage<String> storage = createStorage();
        final Collection<String> ids = fill(storage, 10, new Supplier<String>() {
            @Override
            public String get() {
                return UUID.randomUUID()
                           .toString();
            }
        });

        final Map<String, EntityStorageRecord> allRecords = storage.readAll();
        assertSize(ids.size(), allRecords);

    }

    protected static <T> List<T> fill(RecordStorage<T> storage, int count, Supplier<T> idSupplier) {
        final List<T> ids = new LinkedList<>();

        for (int i = 0; i < count; i++) {
            final T genericId = idSupplier.get();
            final ProjectId id = ProjectId.newBuilder()
                                          .setId(genericId.toString())
                                          .build();
            final Project project = Project.newBuilder()
                                           .setId(id)
                                           .setStatus(Project.Status.CREATED)
                                           .setName(String.format("test-project-%s", i))
                                           .addTask(Task.getDefaultInstance())
                                           .build();
            final EntityStorageRecord record = EntityStorageRecord.newBuilder()
                                                                  .setState(AnyPacker.pack(project))
                                                                  .setWhenModified(Timestamps.getCurrentTime())
                                                                  .setVersion(1)
                                                                  .build();
            storage.write(genericId, record);
            ids.add(genericId);
        }

        return ids;
    }

    protected abstract <T> RecordStorage<T> createStorage();
}
