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

import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import org.junit.Test;
import org.spine3.base.EntityRecord;

import static com.google.protobuf.util.TimeUtil.getCurrentTime;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.spine3.protobuf.Messages.toAny;
import static org.spine3.util.Identifiers.newUuid;

@SuppressWarnings({"InstanceMethodNamingConvention", "AbstractClassWithoutAbstractMethods",
        "ConstructorNotProtectedInAbstractClass"})
public abstract class EntityStorageShould {

    private static final int DEFAULT_ENTITY_VERSION = 5;

    private final EntityStorage<String> storage;

    public EntityStorageShould(EntityStorage<String> storage) {
        this.storage = storage;
    }

    @Test
    public void return_null_if_read_one_record_from_empty_storage() {
        final Message message = storage.load("nothing");
        assertNull(message);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void return_null_if_read_one_record_by_null_id() {
        final Message message = storage.load(null);
        assertNull(message);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_if_write_without_id() {
        storage.write(EntityStorageRecord.getDefaultInstance());
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = NullPointerException.class)
    public void throw_exception_if_write_null_record() {
        storage.write(null);
    }

    @Test
    public void write_and_read_message() {
        testWriteAndReadMessage("testId", "testValue");
    }

    @Test
    public void store_and_load_message() {
        final String id = newUuid();
        final EntityRecord expected = newEntityRecord(id);
        storage.store(expected);

        final EntityRecord actual = storage.load(id);

        assertEquals(expected, actual);
    }

    @Test
    public void write_and_read_several_messages_with_different_ids() {
        testWriteAndReadMessage("id-1", "value-1");
        testWriteAndReadMessage("id-2", "value-2");
        testWriteAndReadMessage("id-3", "value-3");
    }

    @Test
    public void rewrite_message_if_write_with_same_id() {
        final String id = "test-id-rewrite";
        testWriteAndReadMessage(id, "primary-value");
        testWriteAndReadMessage(id, "new-value");
    }

    private void testWriteAndReadMessage(String id, String value) {
        final EntityStorageRecord expected = newEntityStorageRecord(id, value);
        storage.write(expected);

        final EntityStorageRecord actual = storage.read(id);

        assertEquals(expected, actual);
    }

    private static EntityRecord newEntityRecord(String id) {
        final EntityRecord.Builder builder = EntityRecord.newBuilder()
                .setEntityState(toAny(newStringValue(newUuid())))
                .setEntityId(id)
                .setWhenModified(getCurrentTime())
                .setVersion(DEFAULT_ENTITY_VERSION);
        return builder.build();
    }

    private static EntityStorageRecord newEntityStorageRecord(String id, String value) {
        final EntityStorageRecord.Builder builder = EntityStorageRecord.newBuilder()
                .setEntityState(toAny(newStringValue(value)))
                .setEntityId(id)
                .setWhenModified(getCurrentTime())
                .setVersion(DEFAULT_ENTITY_VERSION);
        return builder.build();
    }

    private static StringValue newStringValue(String value) {
        return StringValue.newBuilder().setValue(value).build();
    }
}
