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

package io.spine.server.aggregate.model;

import io.spine.server.BoundedContext;
import io.spine.server.aggregate.given.part.AnAggregatePart;
import io.spine.server.aggregate.given.part.AnAggregateRoot;
import io.spine.server.aggregate.given.part.WrongAggregatePart;
import io.spine.server.model.ModelError;
import io.spine.testing.server.model.ModelTests;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.base.Identifier.newUuid;
import static io.spine.server.aggregate.model.AggregatePartClass.asAggregatePartClass;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Alexander Yevsyukov
 */
@DisplayName("AggregatePartClass should")
class AggregatePartClassTest {

    private final AggregatePartClass<AnAggregatePart> partClass =
            asAggregatePartClass(AnAggregatePart.class);
    private AnAggregateRoot root;

    @BeforeEach
    void setUp() {
        ModelTests.dropAllModels();
        BoundedContext boundedContext = BoundedContext.newBuilder()
                                                      .build();
        root = new AnAggregateRoot(boundedContext, newUuid());
    }

    @Test
    @DisplayName("obtain aggregate part constructor")
    void getAggregatePartConstructor() {
        assertNotNull(partClass.getConstructor());
    }

    @Test
    @DisplayName("throw exception when aggregate part does not have appropriate constructor")
    void throwOnNoProperCtorAvailable() {
        AggregatePartClass<WrongAggregatePart> wrongPartClass =
                asAggregatePartClass(WrongAggregatePart.class);
        assertThrows(ModelError.class, wrongPartClass::getConstructor);
    }

    @Test
    @DisplayName("create aggregate part entity")
    void createAggregatePartEntity() {
        AnAggregatePart part = partClass.createEntity(root);

        assertNotNull(part);
        assertEquals(root.getId(), part.getId());
    }
}
