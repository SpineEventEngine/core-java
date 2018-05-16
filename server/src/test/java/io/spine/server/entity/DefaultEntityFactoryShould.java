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

package io.spine.server.entity;

import com.google.common.testing.EqualsTester;
import com.google.common.testing.SerializableTester;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.StringValue;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Alexander Yevsyukov
 */
public class DefaultEntityFactoryShould {

    private EntityFactory<Long, TestEntity1> entityFactory1;
    private EntityFactory<Long, TestEntity2> entityFactory2;

    @Before
    public void setUp() {
        RecordBasedRepository<Long, TestEntity1, StringValue> r1 = new TestRepository1();
        RecordBasedRepository<Long, TestEntity2, DoubleValue> r2 = new TestRepository2();

        entityFactory1 = r1.entityFactory();
        entityFactory2 = r2.entityFactory();
    }

    @Test
    public void serialize() {
        SerializableTester.reserializeAndAssert(entityFactory1);
    }

    @Test
    public void have_custom_equals() {
        new EqualsTester()
                .addEqualityGroup(entityFactory1)
                .addEqualityGroup(entityFactory2)
                .testEquals();
    }

    /** A test entity class which is not versionable. */
    private static class TestEntity1 extends AbstractEntity<Long, StringValue> {
        private TestEntity1(Long id) {
            super(id);
        }
    }

    /** A test repository */
    private static class TestRepository1
            extends DefaultRecordBasedRepository<Long, TestEntity1, StringValue> {
    }

    /** Another entity with the same ID and different state. */
    private static class TestEntity2 extends AbstractEntity<Long, DoubleValue> {
        protected TestEntity2(Long id) {
            super(id);
        }
    }

    /** A repository for {@link TestEntity2}. */
    private static class TestRepository2
        extends DefaultRecordBasedRepository<Long, TestEntity2, DoubleValue> {
    }
}
