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
import org.spine3.server.reflect.Property;
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
public class EntityRecordEnvelopShould {

    @Test
    public void initialize_with_record_and_storage_fields() {
        final EntityRecordEnvelope envelope =
                new EntityRecordEnvelope(EntityRecord.getDefaultInstance(),
                                         Collections.<String, Property.MemoizedValue<?>>emptyMap());
        assertNotNull(envelope);
    }

    @Test
    public void initialize_with_record_only() {
        final EntityRecordEnvelope envelope =
                new EntityRecordEnvelope(EntityRecord.getDefaultInstance());
        assertNotNull(envelope);
    }

    @Test
    public void not_accept_nulls_in_ctor() {
        new NullPointerTester()
                .setDefault(EntityRecord.class, EntityRecord.getDefaultInstance())
                .testAllPublicConstructors(EntityRecordEnvelope.class);
    }

    @Test
    public void store_record() {
        final EntityRecordEnvelope envelope = newEnvelope();
        final EntityRecord record = envelope.getRecord();
        assertNotNull(record);
    }

    @Test
    public void store_storage_fields() {
        final Property.MemoizedValue<?> mockValue = mock(Property.MemoizedValue.class);
        final Map<String, Property.MemoizedValue<?>> storageFieldsExpected =
                Collections.<String, Property.MemoizedValue<?>>singletonMap("some-key", mockValue);

        final EntityRecordEnvelope envelope =
                new EntityRecordEnvelope(Sample.messageOfType(EntityRecord.class),
                                         storageFieldsExpected);
        assertTrue(envelope.hasStorageFields());

        final Map<String, Property.MemoizedValue<?>> storageFieldActual =
                envelope.getStorageFields();
        assertMapsEqual(storageFieldsExpected, storageFieldActual, "storage fields");
    }

    @Test
    public void return_empty_map_if_no_storage_fields() {
        final EntityRecordEnvelope envelope = new EntityRecordEnvelope(
                EntityRecord.getDefaultInstance());
        assertFalse(envelope.hasStorageFields());
        final Map<String, Property.MemoizedValue<?>> fields = envelope.getStorageFields();
        assertEmpty(fields);
    }

    @Test
    public void have_equals() {
        final Property.MemoizedValue<?> mockValue = mock(Property.MemoizedValue.class);
        final EntityRecordEnvelope noFieldsEnvelope = new EntityRecordEnvelope(
                EntityRecord.getDefaultInstance()
        );
        final EntityRecordEnvelope emptyFieldsEnvelope = new EntityRecordEnvelope(
                EntityRecord.getDefaultInstance(),
                Collections.<String, Property.MemoizedValue<?>>emptyMap()
        );
        final EntityRecordEnvelope notEmptyFieldsEnvelope = new EntityRecordEnvelope(
                EntityRecord.getDefaultInstance(),
                Collections.<String, Property.MemoizedValue<?>>singletonMap("key", mockValue)
        );
        new EqualsTester()
                .addEqualityGroup(noFieldsEnvelope, emptyFieldsEnvelope, notEmptyFieldsEnvelope)
                .addEqualityGroup(newEnvelope())
                .addEqualityGroup(newEnvelope()) // Each one has different EntityRecord
                .testEquals();
    }



    private static EntityRecordEnvelope newEnvelope() {
        return new EntityRecordEnvelope(Sample.messageOfType(EntityRecord.class),
                                        Collections.<String, Property.MemoizedValue<?>>emptyMap());
    }
}
