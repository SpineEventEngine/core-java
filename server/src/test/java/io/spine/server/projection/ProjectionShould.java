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

package io.spine.server.projection;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.BoolValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.StringValue;
import io.spine.core.Event;
import io.spine.core.EventClass;
import io.spine.core.EventContext;
import io.spine.core.Subscribe;
import io.spine.core.Version;
import io.spine.core.Versions;
import io.spine.protobuf.TypeConverter;
import io.spine.server.command.TestEventFactory;
import io.spine.server.entity.given.Given;
import io.spine.validate.StringValueVBuilder;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static io.spine.Identifier.newUuid;
import static io.spine.protobuf.TypeConverter.toMessage;
import static io.spine.server.projection.ProjectionEventDispatcher.dispatch;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProjectionShould {

    private TestProjection projection;

    @Before
    public void setUp() {
        projection = Given.projectionOfClass(TestProjection.class)
                          .withId(newUuid())
                          .withVersion(1)
                          .withState(TypeConverter.<String, StringValue>toMessage("Initial state"))
                          .build();
    }

    @Test
    public void handle_events() {
        final String stringValue = newUuid();

        dispatch(projection, toMessage(stringValue), EventContext.getDefaultInstance());
        assertTrue(projection.getState()
                             .getValue()
                             .contains(stringValue));

        assertTrue(projection.isChanged());

        final Integer integerValue = 1024;
        dispatch(projection, toMessage(integerValue), EventContext.getDefaultInstance());
        assertTrue(projection.getState()
                             .getValue()
                             .contains(String.valueOf(integerValue)));

        assertTrue(projection.isChanged());
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_no_handler_for_event() {
        dispatch(projection, BoolValue.getDefaultInstance(), EventContext.getDefaultInstance());
    }

    @Test
    public void return_event_classes_which_it_handles() {
        final Set<EventClass> classes = ProjectionClass.of(TestProjection.class)
                                                       .getEventSubscriptions();

        assertEquals(TestProjection.HANDLING_EVENT_COUNT, classes.size());
        assertTrue(classes.contains(EventClass.of(StringValue.class)));
        assertTrue(classes.contains(EventClass.of(Int32Value.class)));
    }

    @Test
    public void expose_playing_events_to_the_package() {
        final TestEventFactory eventFactory = TestEventFactory.newInstance(getClass());
        final StringValue strValue = StringValue.newBuilder()
                                             .setValue("eins zwei drei")
                                             .build();
        final Int32Value intValue = Int32Value.newBuilder().setValue(123).build();
        final Version nextVersion = Versions.increment(projection.getVersion());
        final Event e1 = eventFactory.createEvent(strValue, nextVersion);
        final Event e2 = eventFactory.createEvent(intValue, Versions.increment(nextVersion));

        final boolean projectionChanged = Projection.play(projection, ImmutableList.of(e1, e2));

        final String projectionState = projection.getState()
                                                 .getValue();

        assertTrue(projectionChanged);
        assertTrue(projectionState.contains(strValue.getValue()));
        assertTrue(projectionState.contains(String.valueOf(intValue.getValue())));
    }

    private static class TestProjection
            extends Projection<String, StringValue, StringValueVBuilder> {

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
            // Get the current state within the transaction.
            final String currentState = getBuilder().internalBuild()
                                                    .getValue();
            final String result = currentState + (currentState.length() > 0 ? " + " : "") +
                    type + '(' + value + ')' + System.lineSeparator();
            return toMessage(result);
        }
    }
}
