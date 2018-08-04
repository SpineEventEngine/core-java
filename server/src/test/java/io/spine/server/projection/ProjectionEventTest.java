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

package io.spine.server.projection;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.BoolValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.StringValue;
import io.spine.core.Event;
import io.spine.core.EventClass;
import io.spine.core.EventContext;
import io.spine.core.Version;
import io.spine.core.Versions;
import io.spine.protobuf.TypeConverter;
import io.spine.server.projection.given.ProjectionTestEnv.TestProjection;
import io.spine.testing.server.TestEventFactory;
import io.spine.testing.server.entity.given.Given;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static io.spine.base.Identifier.newUuid;
import static io.spine.protobuf.TypeConverter.toMessage;
import static io.spine.server.projection.model.ProjectionClass.asProjectionClass;
import static io.spine.testing.server.projection.ProjectionEventDispatcher.dispatch;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Alexander Yevsyukov
 * @author Alex Tymchenko
 * @author Dmytro Dashenkov
 */
// TODO:2018-07-16:dmytro.dashenkov: Rename to `ProjectionTest`.
// todo                              https://github.com/SpineEventEngine/core-java/issues/753
@DisplayName("Projection should")
class ProjectionEventTest {

    private TestProjection projection;

    @BeforeEach
    void setUp() {
        projection = Given.projectionOfClass(TestProjection.class)
                          .withId(newUuid())
                          .withVersion(1)
                          .withState(TypeConverter.toMessage("Initial state"))
                          .build();
    }

    @Test
    @DisplayName("handle events")
    void handleEvents() {
        String stringValue = newUuid();

        dispatch(projection, toMessage(stringValue), EventContext.getDefaultInstance());
        assertTrue(projection.getState()
                             .getValue()
                             .contains(stringValue));

        assertTrue(projection.isChanged());

        Integer integerValue = 1024;
        dispatch(projection, toMessage(integerValue), EventContext.getDefaultInstance());
        assertTrue(projection.getState()
                             .getValue()
                             .contains(String.valueOf(integerValue)));

        assertTrue(projection.isChanged());
    }

    @Test
    @DisplayName("throw ISE if no handler is present for event")
    void throwIfNoHandlerPresent() {
        assertThrows(IllegalStateException.class,
                     () -> dispatch(projection,
                                    BoolValue.getDefaultInstance(),
                                    EventContext.getDefaultInstance()));
    }

    @Test
    @DisplayName("return handled event classes")
    void exposeEventClasses() {
        Set<EventClass> classes =
                asProjectionClass(TestProjection.class).getEventClasses();

        assertEquals(TestProjection.HANDLING_EVENT_COUNT, classes.size());
        assertTrue(classes.contains(EventClass.of(StringValue.class)));
        assertTrue(classes.contains(EventClass.of(Int32Value.class)));
    }

    @Test
    @DisplayName("expose `play events` operation to package")
    void exposePlayingEvents() {
        TestEventFactory eventFactory = TestEventFactory.newInstance(getClass());
        StringValue strValue = StringValue.newBuilder()
                                          .setValue("eins zwei drei")
                                          .build();
        Int32Value intValue = Int32Value.newBuilder()
                                        .setValue(123)
                                        .build();
        Version nextVersion = Versions.increment(projection.getVersion());
        Event e1 = eventFactory.createEvent(strValue, nextVersion);
        Event e2 = eventFactory.createEvent(intValue, Versions.increment(nextVersion));

        boolean projectionChanged = Projection.play(projection, ImmutableList.of(e1, e2));

        String projectionState = projection.getState()
                                           .getValue();

        assertTrue(projectionChanged);
        assertTrue(projectionState.contains(strValue.getValue()));
        assertTrue(projectionState.contains(String.valueOf(intValue.getValue())));
    }
}
