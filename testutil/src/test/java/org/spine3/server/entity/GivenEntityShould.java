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

package org.spine3.server.entity;

import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import org.junit.Test;
import org.spine3.protobuf.Timestamps;
import org.spine3.protobuf.Values;

import static org.junit.Assert.assertEquals;


@SuppressWarnings({"ConstantConditions" /* some of the methods test `null` arguments */,
                   "ResultOfMethodCallIgnored" /* we ignore when we test for `null`s */})
public class GivenEntityShould {

    /**
     * Convenience method that mimics the way tests would call creation of an entity.
     */
    private static GivenEntity<TestEntity, Long, StringValue> givenEntity() {
        return GivenEntity.<TestEntity, Long, StringValue>whichIs().ofClass(TestEntity.class);
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_class() {
        GivenEntity.whichIs().ofClass(null);
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_ID() {
        givenEntity().withId(null);
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_state() {
        givenEntity().withState(null);
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_timestamp() {
        givenEntity().modifiedOn(null);
    }

    @Test
    public void obtain_entity_id_class() {
        assertEquals(Long.class, givenEntity().getIdClass());
    }

    @Test
    public void create_entity() {
        final long id = 1024L;
        final int version = 100500;
        final StringValue state = Values.newStringValue(getClass().getName());
        final Timestamp timestamp = Timestamps.getCurrentTime();

        final Entity entity = givenEntity()
                .withId(id)
                .withVersion(version)
                .withState(state)
                .modifiedOn(timestamp)
                .build();

        assertEquals(TestEntity.class, entity.getClass());
        assertEquals(id, entity.getId());
        assertEquals(state, entity.getState());
        assertEquals(version, entity.getVersion());
        assertEquals(timestamp, entity.whenModified());
    }

    private static class TestEntity extends Entity<Long, StringValue> {
        protected TestEntity(Long id) {
            super(id);
        }
    }
}
