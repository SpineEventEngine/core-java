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

import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import org.junit.Test;
import org.spine3.protobuf.AnyPacker;
import org.spine3.protobuf.Timestamps;

import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.spine3.test.Verify.assertEmpty;

/**
 * @author Dmytro Dashenkov
 */
public abstract class RecordStorageShould<I> extends AbstractStorageShould<I, EntityStorageRecord> {

    protected abstract Message emptyState();

    @Override
    protected EntityStorageRecord newStorageRecord() {
        final Any state = AnyPacker.pack(emptyState());
        final EntityStorageRecord record = EntityStorageRecord.newBuilder()
                                                              .setState(state)
                                                              .setWhenModified(Timestamps.getCurrentTime())
                                                              .setVersion(0)
                                                              .build();
        return record;
    }

    @Test
    public void retrieve_empty_map_if_storage_is_empty() {
        final RecordStorage storage = (RecordStorage) getStorage();

        final FieldMask nonEmptyFieldMask = FieldMask.newBuilder()
                                                     .addPaths("invalid-path")
                                                     .build();

        final Map empty = storage.readAll(nonEmptyFieldMask);
        assertNotNull(empty);
        assertEmpty(empty);
    }
}
