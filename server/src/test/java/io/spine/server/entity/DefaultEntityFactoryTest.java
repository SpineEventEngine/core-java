/*
 * Copyright 2023, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import io.spine.server.entity.given.DefaultEntityFactoryTestEnv.TestEntity1;
import io.spine.server.entity.given.DefaultEntityFactoryTestEnv.TestEntity2;
import io.spine.server.entity.given.DefaultEntityFactoryTestEnv.TestRepository1;
import io.spine.server.entity.given.DefaultEntityFactoryTestEnv.TestRepository2;
import io.spine.server.given.groups.GroupName;
import io.spine.test.entity.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DefaultEntityFactory should")
class DefaultEntityFactoryTest {

    private EntityFactory<TestEntity1> entityFactory1;
    private EntityFactory<TestEntity2> entityFactory2;

    @BeforeEach
    void setUp() {
        RecordBasedRepository<String, TestEntity1, Project> r1 = new TestRepository1();
        RecordBasedRepository<String, TestEntity2, GroupName> r2 = new TestRepository2();

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
