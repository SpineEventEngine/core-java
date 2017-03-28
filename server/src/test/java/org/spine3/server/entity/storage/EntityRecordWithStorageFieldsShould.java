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

import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import org.junit.Test;
import org.spine3.server.entity.EntityRecord;
import org.spine3.server.entity.storage.reflect.Column;
import org.spine3.testdata.Sample;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.spine3.test.Verify.assertEmpty;
import static org.spine3.test.Verify.assertMapsEqual;

/**
 * @author Dmytro Dashenkov
 */
public class EntityRecordWithStorageFieldsShould {

    @Test
    public void initialize_with_record_and_storage_fields() {
        final EntityRecordWithStorageFields envelope =
                new EntityRecordWithStorageFields(EntityRecord.getDefaultInstance(),
                                                  Collections.<String, Column.MemoizedValue<?>>emptyMap());
        assertNotNull(envelope);
    }

    @Test
    public void initialize_with_record_only() {
        final EntityRecordWithStorageFields envelope =
                new EntityRecordWithStorageFields(EntityRecord.getDefaultInstance());
        assertNotNull(envelope);
    }

    @Test
    public void not_accept_nulls_in_ctor() {
        new NullPointerTester()
                .setDefault(EntityRecord.class, EntityRecord.getDefaultInstance())
                .testAllPublicConstructors(EntityRecordWithStorageFields.class);
    }

    @Test
    public void store_record() {
        final EntityRecordWithStorageFields envelope = newEnvelope();
        final EntityRecord record = envelope.getRecord();
        assertNotNull(record);
    }

    @Test
    public void store_storage_fields() {
        final Column.MemoizedValue<?> mockValue = mock(Column.MemoizedValue.class);
        final Map<String, Column.MemoizedValue<?>> storageFieldsExpected =
                Collections.<String, Column.MemoizedValue<?>>singletonMap("some-key", mockValue);

        final EntityRecordWithStorageFields envelope =
                new EntityRecordWithStorageFields(Sample.messageOfType(EntityRecord.class),
                                                  storageFieldsExpected);
        assertTrue(envelope.hasStorageFields());

        final Map<String, Column.MemoizedValue<?>> storageFieldActual =
                envelope.getStorageFields();
        assertMapsEqual(storageFieldsExpected, storageFieldActual, "storage fields");
    }

    @Test
    public void return_empty_map_if_no_storage_fields() {
        final EntityRecordWithStorageFields envelope = new EntityRecordWithStorageFields(
                EntityRecord.getDefaultInstance());
        assertFalse(envelope.hasStorageFields());
        final Map<String, Column.MemoizedValue<?>> fields = envelope.getStorageFields();
        assertEmpty(fields);
    }

    @Test
    public void have_equals() {
        final Column.MemoizedValue<?> mockValue = mock(Column.MemoizedValue.class);
        final EntityRecordWithStorageFields noFieldsEnvelope = new EntityRecordWithStorageFields(
                EntityRecord.getDefaultInstance()
        );
        final EntityRecordWithStorageFields emptyFieldsEnvelope = new EntityRecordWithStorageFields(
                EntityRecord.getDefaultInstance(),
                Collections.<String, Column.MemoizedValue<?>>emptyMap()
        );
        final EntityRecordWithStorageFields notEmptyFieldsEnvelope = new EntityRecordWithStorageFields(
                EntityRecord.getDefaultInstance(),
                Collections.<String, Column.MemoizedValue<?>>singletonMap("key", mockValue)
        );
        new EqualsTester()
                .addEqualityGroup(noFieldsEnvelope, emptyFieldsEnvelope, notEmptyFieldsEnvelope)
                .addEqualityGroup(newEnvelope())
                .addEqualityGroup(newEnvelope()) // Each one has different EntityRecord
                .testEquals();
    }



    private static EntityRecordWithStorageFields newEnvelope() {
        return new EntityRecordWithStorageFields(Sample.messageOfType(EntityRecord.class),
                                                 Collections.<String, Column.MemoizedValue<?>>emptyMap());
    }
}
