/*
 * Copyright 2022, TeamDev. All rights reserved.
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

package io.spine.server.aggregate.model;

import io.spine.server.BoundedContextBuilder;
import io.spine.server.aggregate.given.part.AnAggregatePart;
import io.spine.server.aggregate.given.part.AnAggregateRoot;
import io.spine.server.aggregate.given.part.WrongAggregatePart;
import io.spine.server.model.ModelError;
import io.spine.test.aggregate.ProjectId;
import io.spine.testing.server.model.ModelTests;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.server.aggregate.model.AggregatePartClass.asAggregatePartClass;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("`AggregatePartClass` should")
class AggregatePartClassTest {

    private final AggregatePartClass<AnAggregatePart> partClass =
            asAggregatePartClass(AnAggregatePart.class);
    private AnAggregateRoot root;

    @BeforeEach
    void setUp() {
        ModelTests.dropAllModels();
        var boundedContext = BoundedContextBuilder.assumingTests().build();
        var projectId = ProjectId.generate();
        root = new AnAggregateRoot(boundedContext, projectId);
    }

    @Test
    @DisplayName("obtain aggregate part constructor")
    void getAggregatePartConstructor() {
        assertNotNull(partClass.constructor());
    }

    @Test
    @DisplayName("throw exception when aggregate part does not have appropriate constructor")
    void throwOnNoProperCtorAvailable() {
        var wrongPartClass = asAggregatePartClass(WrongAggregatePart.class);
        assertThrows(ModelError.class, wrongPartClass::constructor);
    }

    @Test
    @DisplayName("create aggregate part entity")
    void createAggregatePartEntity() {
        var part = partClass.create(root);

        assertNotNull(part);
        assertEquals(root.id(), part.id());
    }
}
