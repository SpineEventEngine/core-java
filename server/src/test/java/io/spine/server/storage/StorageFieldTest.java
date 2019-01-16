/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server.storage;

import io.spine.server.aggregate.AggregateField;
import io.spine.server.event.storage.EventContextField;
import io.spine.server.event.storage.EventField;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("StorageField should")
class StorageFieldTest {

    @Test
    @DisplayName("declare no methods")
    void declareNoMethods() {
        Class<?> clazz = StorageField.class;
        Method[] methods = clazz.getDeclaredMethods();

        assertEquals(0, methods.length);
    }

    @Test
    @DisplayName("enclose all aggregate fields")
    void encloseAggregateFields() {
        assertField(AggregateField.aggregate_id);
    }

    @Test
    @DisplayName("enclose all event fields")
    void encloseEventFields() {
        assertField(EventField.event_id);
        assertField(EventField.event_type);
        assertField(EventField.producer_id);
    }

    @Test
    @DisplayName("enclose all event context fields")
    void encloseEventContextFields() {
        assertField(EventContextField.context_of_command);
        assertField(EventContextField.context_timestamp);
        assertField(EventContextField.context_version);
    }

    @Test
    @DisplayName("enclose all entity status fields")
    void encloseEntityStatusFields() {
        assertField(LifecycleFlagField.archived);
        assertField(LifecycleFlagField.deleted);
    }

    @Test
    @DisplayName("enclose all entity fields")
    void encloseEntityFields() {
        assertField(VersionField.timestamp);
        assertField(VersionField.timestamp_nanos);
        assertField(StateField.type_url);
        assertField(StateField.bytes);
    }

    private static void assertField(Enum field) {
        assertNotNull(field);
        assertThat(field, instanceOf(StorageField.class));
    }
}
