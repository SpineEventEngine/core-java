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
import org.junit.Before;
import org.junit.Test;
import org.spine3.server.BoundedContext;
import org.spine3.server.aggregate.AggregatePart;
import org.spine3.server.aggregate.AggregateRoot;

import java.lang.reflect.Constructor;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.server.entity.Entities.createAggregatePartEntity;
import static org.spine3.server.entity.Entities.createAggregateRootEntity;
import static org.spine3.server.entity.Entities.getAggregatePartConstructor;
import static org.spine3.server.entity.Entities.getConstructor;
import static org.spine3.test.Tests.hasPrivateParameterlessCtor;

/**
 * @author Illia Shepilov
 */
public class EntitiesShould {

    private BoundedContext boundedContext;
    private AnAggregateRoot root;
    private String id;

    @Before
    public void setUp() {
        boundedContext = BoundedContext.newBuilder()
                                       .build();
        id = newUuid();
        root = new AnAggregateRoot(boundedContext, id);
    }

    @Test
    public void have_private_constructor() {
        assertTrue(hasPrivateParameterlessCtor(Entities.class));
    }

    @Test
    public void create_aggregate_part_entity() throws NoSuchMethodException {
        final Constructor<AnAggregatePart> constructor =
                AnAggregatePart.class.getDeclaredConstructor(String.class, AnAggregateRoot.class);
        final AggregatePart aggregatePart = createAggregatePartEntity(constructor, id, root);
        assertNotNull(aggregatePart);
    }

    @Test
    public void create_aggregate_root_entity() {
        final AnAggregateRoot aggregateRoot =
                createAggregateRootEntity(id, boundedContext, AnAggregateRoot.class);
        assertNotNull(aggregateRoot);
    }

    @SuppressWarnings("unchecked")
    // Supply a "wrong" value on purpose to cause the validation failure.
    @Test(expected = IllegalStateException.class)
    public void throw_exception_when_aggregate_part_does_not_have_appropriate_constructor() {
        getAggregatePartConstructor(WrongAggregatePart.class, AggregateRoot.class, id.getClass());
    }

    @SuppressWarnings("unchecked")
    // Supply a "wrong" value on purpose to cause the validation failure.
    @Test(expected = IllegalStateException.class)
    public void throw_exception_when_aggregate_does_not_have_appropriate_constructor() {
        getConstructor(AggregatePart.class, id.getClass());
    }

    /*
     Test environment classes
    ***************************/

    private static class WrongAggregatePart
            extends AggregatePart<String, StringValue, StringValue.Builder> {

        @SuppressWarnings("ConstantConditions")
        // Supply a "wrong" parameters on purpose to cause the validation failure
        protected WrongAggregatePart() {
            super(null, null);
        }
    }

    private static class AnAggregatePart
            extends AggregatePart<String, StringValue, StringValue.Builder> {

        protected AnAggregatePart(String id, AnAggregateRoot root) {
            super(id, root);
        }
    }

    private static class AnAggregateRoot extends AggregateRoot<String> {

        /**
         * Creates an new instance.
         *
         * @param boundedContext the bounded context to which the aggregate belongs
         * @param id             the ID of the aggregate
         */
        protected AnAggregateRoot(BoundedContext boundedContext, String id) {
            super(boundedContext, id);
        }
    }
}
