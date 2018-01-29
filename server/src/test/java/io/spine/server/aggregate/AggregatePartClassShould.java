/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.server.aggregate;

import io.spine.server.BoundedContext;
import io.spine.server.aggregate.given.AggregatePartTestEnv.AnAggregatePart;
import io.spine.server.aggregate.given.AggregatePartTestEnv.AnAggregateRoot;
import io.spine.server.aggregate.given.AggregatePartTestEnv.WrongAggregatePart;
import io.spine.server.model.ModelError;
import io.spine.server.model.ModelTests;
import org.junit.Before;
import org.junit.Test;

import static io.spine.Identifier.newUuid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Alexander Yevsyukov
 */
public class AggregatePartClassShould {

    private final AggregatePartClass<AnAggregatePart> partClass =
            new AggregatePartClass<>(AnAggregatePart.class);
    private AnAggregateRoot root;

    @Before
    public void setUp() {
        ModelTests.clearModel();
        final BoundedContext boundedContext = BoundedContext.newBuilder()
                                                            .build();
        root = new AnAggregateRoot(boundedContext, newUuid());
    }

    @Test
    public void obtain_aggregate_part_constructor() {
        assertNotNull(partClass.getConstructor());
    }

    @Test(expected = ModelError.class)
    public void throw_exception_when_aggregate_part_does_not_have_appropriate_constructor() {
        new AggregatePartClass<>(WrongAggregatePart.class).getConstructor();
    }

    @Test
    public void create_aggregate_part_entity() throws NoSuchMethodException {
        final AnAggregatePart part = partClass.createEntity(root);

        assertNotNull(part);
        assertEquals(root.getId(), part.getId());
    }
}
