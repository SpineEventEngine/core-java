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

package io.spine.server.entity;

import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import io.spine.test.TimeTests;
import io.spine.time.Interval;
import io.spine.time.Intervals;
import io.spine.time.Time;
import org.junit.Test;

import java.lang.reflect.Constructor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Yevsyukov
 */
public class EntityClassShould {

    private final EntityClass<NanoEntity> entityClass = new EntityClass<>(
            (Class<? extends NanoEntity>) NanoEntity.class);

    @Test
    public void return_id_class() {
        @SuppressWarnings("unchecked") //
        final Class<Long> actual = (Class<Long>) entityClass.getIdClass();
        assertEquals(Long.class, actual);
    }

    @Test
    public void obtain_entity_constructor() {
        final Constructor<NanoEntity> ctor = entityClass.getConstructor();
        assertNotNull(ctor);
    }

    @Test
    public void create_and_initialize_entity_instance() {
        final Long id = 100L;
        final Timestamp before = TimeTests.Past.secondsAgo(1);

        // Create and init the entity.
        final EntityClass<NanoEntity> entityClass = new EntityClass<>(NanoEntity.class);
        final AbstractVersionableEntity<Long, StringValue> entity = entityClass.createEntity(id);

        final Timestamp after = Time.getCurrentTime();

        // The interval with a much earlier start to allow non-zero interval on faster computers.
        final Interval whileWeCreate = Intervals.between(before, after);

        assertEquals(id, entity.getId());
        assertEquals(0, entity.getVersion()
                              .getNumber());
        assertTrue(Intervals.contains(whileWeCreate, entity.whenModified()));
        assertEquals(StringValue.getDefaultInstance(), entity.getState());
        assertFalse(entity.isArchived());
        assertFalse(entity.isDeleted());
    }

    /** A test entity which defines ID and state. */
    private static class NanoEntity extends AbstractVersionableEntity<Long, StringValue> {
        private NanoEntity(Long id) {
            super(id);
        }
    }
}
