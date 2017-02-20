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

package org.spine3.server.storage;

import org.junit.Test;
import org.spine3.server.aggregate.storage.AggregateField;
import org.spine3.server.event.storage.EventContextField;
import org.spine3.server.event.storage.EventField;

import java.lang.reflect.Method;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * @author Dmytro Dashenkov.
 */
public class StorageFieldShould {

    @Test
    public void declare_no_methods() {
        final Class<?> clazz = StorageField.class;
        final Method[] methods = clazz.getDeclaredMethods();

        assertEquals(0, methods.length);
    }

    @Test
    public void enclose_all_aggregate_fields() {
        assertField(AggregateField.aggregate_id);
    }

    @Test
    public void enclose_all_event_fields() {
        assertField(EventField.event_id);
        assertField(EventField.event_type);
        assertField(EventField.producer_id);
    }

    @Test
    public void enclose_all_event_context_fields() {
        assertField(EventContextField.context_event_id);
        assertField(EventContextField.context_of_command);
        assertField(EventContextField.context_timestamp);
        assertField(EventContextField.context_version);
    }

    @Test
    public void enclose_all_entity_status_fields() {
        assertField(VisibilityField.archived);
        assertField(VisibilityField.deleted);
    }

    @Test
    public void enclose_all_entity_fields() {
        assertField(EntityField.timestamp);
        assertField(EntityField.timestamp_nanos);
        assertField(EntityField.type_url);
        assertField(EntityField.bytes);
    }

    private static void assertField(Enum field) {
        assertNotNull(field);
        assertThat(field, instanceOf(StorageField.class));
    }
}
