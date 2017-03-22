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

import java.util.Map;

/**
 * @author Dmytro Dashenkov
 */
public class EntityRecordEnvelope {

    private final EntityRecord record;
    private final ImmutableMap<String, Object> storageFields;

    public EntityRecordEnvelope(EntityRecord record, Map<String, Object> storageFields) {
        this.record = record;
        this.storageFields = ImmutableMap.copyOf(storageFields);
    }

    public EntityRecord getRecord() {
        return record;
    }

    @SuppressWarnings("ReturnOfCollectionOrArrayField") // Immutable structure
    public ImmutableMap<String, Object> getStorageFields() {
        return storageFields;
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
