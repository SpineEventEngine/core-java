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

package org.spine3.server.projection;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.BoolValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.StringValue;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.EventContext;
import org.spine3.annotation.Subscribe;
import org.spine3.protobuf.Wrapper;
import org.spine3.test.Given;
import org.spine3.type.EventClass;
import org.spine3.validate.StringValueValidatingBuilder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.protobuf.Wrapper.forInteger;
import static org.spine3.server.projection.ProjectionTransaction.start;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;

public class ProjectionShould {

    private TestProjection projection;

    @Before
    public void setUp() {
        projection = Given.projectionOfClass(TestProjection.class)
                                          .withId(newUuid())
                                          .withVersion(1)
                                          .withState(Wrapper.forString("Initial state"))
                                          .build();
    }

    @Test
    public void handle_events() {
        final String stringValue = newUuid();

        ProjectionTransaction<?, ?, ?> tx;
        tx = start(projection);
        assertFalse(projection.isChanged());
        projection.handle(Wrapper.forString(stringValue), EventContext.getDefaultInstance());
        tx.commit();
        assertTrue(projection.getState()
                             .getValue()
                             .contains(stringValue));

        assertTrue(projection.isChanged());

        final Integer integerValue = 1024;

        tx = start(projection);
        assertFalse(projection.isChanged());
        projection.handle(forInteger(integerValue), EventContext.getDefaultInstance());
        tx.commit();
        assertTrue(projection.getState()
                             .getValue()
                             .contains(String.valueOf(integerValue)));

        assertTrue(projection.isChanged());
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_no_handler_for_event() {
        projection.handle(BoolValue.getDefaultInstance(), EventContext.getDefaultInstance());
    }

    @Test
    public void return_event_classes_which_it_handles() {
        final ImmutableSet<EventClass> classes =
                Projection.TypeInfo.getEventClasses(TestProjection.class);

        assertEquals(TestProjection.HANDLING_EVENT_COUNT, classes.size());
        assertTrue(classes.contains(EventClass.of(StringValue.class)));
        assertTrue(classes.contains(EventClass.of(Int32Value.class)));
    }

    @Test
    public void have_TypeInfo_utility_class() {
        assertHasPrivateParameterlessCtor(Projection.TypeInfo.class);
    }

    private static class TestProjection
            extends Projection<String, StringValue, StringValueValidatingBuilder> {

        /** The number of events this class handles. */
        private static final int HANDLING_EVENT_COUNT = 2;

        protected TestProjection(String id) {
            super(id);
        }

        @Subscribe
        public void on(StringValue event) {
            final StringValue newState = createNewState("stringState", event.getValue());
            getBuilder().mergeFrom(newState);
        }

        @Subscribe
        public void on(Int32Value event) {
            final StringValue newState = createNewState("integerState",
                                                       String.valueOf(event.getValue()));
            getBuilder().mergeFrom(newState);
        }

        private StringValue createNewState(String type, String value) {
            final String currentState = getState().getValue();
            final String result = currentState + (currentState.length() > 0 ? " + " : "") +
                    type + '(' + value + ')' + System.lineSeparator();
            return Wrapper.forString(result);
        }
    }
}
