/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.storage.memory;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.junit.Before;
import org.junit.Test;
import org.spine3.server.storage.EntityStorage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * In-memory implementation of {@link org.spine3.server.storage.EntityStorage} tests.
 *
 * @author Alexander Litus
 */
@SuppressWarnings({"InstanceMethodNamingConvention", "MethodMayBeStatic", "MagicNumber", "ClassWithTooManyMethods",
        "DuplicateStringLiteralInspection", "ConstantConditions"})
public class InMemoryEntityStorageShould {

    private EntityStorage<String, Any> storage;
    private Any message;

    @Before
    public void setUp() {

        storage = InMemoryStorageFactory.instance().createEntityStorage(null);
        message = Any.newBuilder().setTypeUrl("typeUrl").build();
    }

    @Test
    public void return_null_if_read_one_record_from_empty_storage() {

        final Message message = storage.read("testId");
        assertNull(message);
    }

    @Test
    public void return_null_if_read_one_record_by_null_id() {

        final Message message = storage.read(null);
        assertNull(message);
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_write_by_null_id() {
        storage.write(null, message);
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_write_null_message() {
        storage.write("testId", null);
    }

    @Test
    public void save_and_read_message() {

        final String id = "testId";

        storage.write(id, message);

        final Any messageFromStorage = storage.read(id);

        assertEquals(message, messageFromStorage);
    }
}
