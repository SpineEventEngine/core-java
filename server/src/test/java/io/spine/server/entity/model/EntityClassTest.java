/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.server.entity.model;

import com.google.common.collect.Range;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import io.spine.base.Time;
import io.spine.server.entity.AbstractEntity;
import io.spine.server.entity.AbstractVersionableEntity;
import io.spine.server.model.ModelError;
import io.spine.time.testing.TimeTests;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.time.Instant;

import static io.spine.protobuf.Timestamps2.toInstant;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("EntityClass should")
class EntityClassTest {

    private final EntityClass<NanoEntity> entityClass =
            new EntityClass<>(NanoEntity.class);

    @Test
    @DisplayName("return ID class")
    void returnIdClass() {
        @SuppressWarnings("unchecked")
        Class<Long> actual = (Class<Long>) entityClass.getIdClass();
        assertEquals(Long.class, actual);
    }

    @Test
    @DisplayName("obtain entity constructor")
    void getEntityConstructor() {
        Constructor<NanoEntity> ctor = entityClass.getConstructor();
        assertNotNull(ctor);
    }

    @Test
    @DisplayName("create and initialize entity instance")
    void createEntityInstance() {
        Long id = 100L;
        Timestamp before = TimeTests.Past.secondsAgo(1);

        // Create and init the entity.
        EntityClass<NanoEntity> entityClass = new EntityClass<>(NanoEntity.class);
        AbstractVersionableEntity<Long, StringValue> entity = entityClass.createEntity(id);

        Timestamp after = Time.getCurrentTime();

        // The interval with a much earlier start to allow non-zero interval on faster computers.
        Range<Instant> whileWeCreate = Range.closed(toInstant(before), toInstant(after));

        assertEquals(id, entity.getId());
        assertEquals(0, entity.getVersion()
                              .getNumber());
        assertTrue(whileWeCreate.contains(toInstant(entity.whenModified())));
        assertEquals(StringValue.getDefaultInstance(), entity.getState());
        assertFalse(entity.isArchived());
        assertFalse(entity.isDeleted());
    }

    @Test
    @DisplayName("complain when there is no one-arg constructor for entity class")
    void searchForOneArgCtor() {
        assertThrows(ModelError.class,
                     () -> new EntityClass<>(NoArgEntity.class).getConstructor());
    }

    /** A test entity which defines ID and state. */
    private static class NanoEntity extends AbstractVersionableEntity<Long, StringValue> {
        private NanoEntity(Long id) {
            super(id);
        }
    }

    /** An entity class without ID constructor. */
    private static class NoArgEntity extends AbstractEntity<Long, StringValue> {
        private NoArgEntity() {
            super(0L);
        }
    }
}
