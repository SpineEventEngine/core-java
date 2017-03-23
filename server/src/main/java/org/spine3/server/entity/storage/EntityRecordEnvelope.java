/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.entity.storage;

import com.google.common.collect.ImmutableMap;
import org.spine3.server.entity.EntityRecord;
import org.spine3.server.reflect.Property;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * @author Dmytro Dashenkov
 */
public class EntityRecordEnvelope {

    private final EntityRecord record;

    @Nullable
    private final ImmutableMap<String, Property.MemoizedValue<?>> storageFields;

    public EntityRecordEnvelope(EntityRecord record,
                                Map<String, Property.MemoizedValue<?>> storageFields) {
        this.record = record;
        this.storageFields = ImmutableMap.copyOf(storageFields);
    }

    @SuppressWarnings("ConstantConditions")
        // null value for the storage fields map
    public EntityRecordEnvelope(EntityRecord record) {
        this.record = record;
        this.storageFields = null;
    }

    public EntityRecord getRecord() {
        return record;
    }

    @SuppressWarnings("ReturnOfCollectionOrArrayField") // Immutable structure
    public Map<String, Property.MemoizedValue<?>> getStorageFields() {
        return storageFields == null
                ? StorageFields.empty()
                : storageFields;
    }

    public boolean hasStorageFiedls() {
        return storageFields != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EntityRecordEnvelope envelope = (EntityRecordEnvelope) o;

        return getRecord().equals(envelope.getRecord());
    }

    @Override
    public int hashCode() {
        return getRecord().hashCode();
    }
}
