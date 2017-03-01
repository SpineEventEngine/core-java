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

package org.spine3.server.aggregate;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.StringValue;
import org.junit.Before;
import org.junit.Test;
import org.spine3.server.BoundedContext;

import java.lang.reflect.Constructor;

import static org.junit.Assert.assertNotNull;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.server.aggregate.AggregatePart.create;
import static org.spine3.server.aggregate.AggregatePart.getConstructor;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;

/**
 * @author Illia Shepilov
 */
public class AggregatePartShould {

    private BoundedContext boundedContext;
    private AnAggregateRoot root;

    @Before
    public void setUp() {
        boundedContext = BoundedContext.newBuilder()
                                       .build();
        root = new AnAggregateRoot(boundedContext, newUuid());
    }

    @Test
    public void not_accept_nulls_as_parameter_values() throws NoSuchMethodException {
        final Constructor constructor = AnAggregateRoot.class
                                            .getDeclaredConstructor(BoundedContext.class,
                                                                    String.class);
        final NullPointerTester tester = new NullPointerTester();
        tester.setDefault(Constructor.class, constructor)
              .setDefault(BoundedContext.class, boundedContext)
              .setDefault(AggregateRoot.class, root)
              .testStaticMethods(AggregatePart.class, NullPointerTester.Visibility.PACKAGE);
    }

    @Test
    public void create_aggregate_part_entity() throws NoSuchMethodException {
        final Constructor<AnAggregatePart> constructor =
                AnAggregatePart.class.getDeclaredConstructor(AnAggregateRoot.class);
        final AggregatePart aggregatePart = create(constructor, root);
        assertNotNull(aggregatePart);
    }


    @Test(expected = IllegalStateException.class)
    public void throw_exception_when_aggregate_part_does_not_have_appropriate_constructor() {
        getConstructor(WrongAggregatePart.class);
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exc_during_aggregate_part_creation_when_it_does_not_have_appropriate_ctor()
            throws NoSuchMethodException {
        final Constructor<WrongAggregatePart> constructor =
                WrongAggregatePart.class.getDeclaredConstructor();
        create(constructor, root);
    }

    @Test
    public void obtain_aggregate_part_constructor() {
        final Constructor<AnAggregatePart> constructor =
                getConstructor(AnAggregatePart.class);
        assertNotNull(constructor);
    }

    @Test
    public void have_TypeInfo() {
        assertHasPrivateParameterlessCtor(AggregatePart.TypeInfo.class);
    }

    /*
     Test environment classes
    ***************************/

    private static class AnAggregateRoot extends AggregateRoot<String> {
        protected AnAggregateRoot(BoundedContext boundedContext, String id) {
            super(boundedContext, id);
        }
    }

    private static class WrongAggregatePart extends AggregatePart<String,
                                                                  StringValue,
                                                                  StringValue.Builder,
                                                                  AnAggregateRoot> {
        @SuppressWarnings("ConstantConditions")
        // Supply a "wrong" parameters on purpose to cause the validation failure
        protected WrongAggregatePart() {
            super(null);
        }
    }

    private static class AnAggregatePart extends AggregatePart<String,
                                                               StringValue,
                                                               StringValue.Builder,
                                                               AnAggregateRoot> {

        protected AnAggregatePart(AnAggregateRoot root) {
            super(root);
        }
    }
}
