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

package io.spine.server.entity;

import com.google.common.testing.EqualsTester;
import com.google.common.testing.SerializableTester;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.StringValue;
import io.spine.server.entity.given.DefaultEntityFactoryTestEnv.TestEntity1;
import io.spine.server.entity.given.DefaultEntityFactoryTestEnv.TestEntity2;
import io.spine.server.entity.given.DefaultEntityFactoryTestEnv.TestRepository1;
import io.spine.server.entity.given.DefaultEntityFactoryTestEnv.TestRepository2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("DuplicateStringLiteralInspection") // Common test display names.
@DisplayName("DefaultEntityFactory should")
class DefaultEntityFactoryTest {

    private EntityFactory<Long, TestEntity1> entityFactory1;
    private EntityFactory<Long, TestEntity2> entityFactory2;

    @BeforeEach
    void setUp() {
        RecordBasedRepository<Long, TestEntity1, StringValue> r1 = new TestRepository1();
        RecordBasedRepository<Long, TestEntity2, DoubleValue> r2 = new TestRepository2();

        entityFactory1 = r1.entityFactory();
        entityFactory2 = r2.entityFactory();
    }

    @Test
    @DisplayName("be serializable")
    void beSerializable() {
        SerializableTester.reserializeAndAssert(entityFactory1);
    }

    @Test
    @DisplayName("support equality")
    void supportEquality() {
        new EqualsTester()
                .addEqualityGroup(entityFactory1)
                .addEqualityGroup(entityFactory2)
                .testEquals();
    }
}
