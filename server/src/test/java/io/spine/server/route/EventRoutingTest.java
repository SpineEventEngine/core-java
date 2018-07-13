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

package io.spine.server.route;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.base.Time;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.EventEnvelope;
import io.spine.server.command.TestEventFactory;
import io.spine.test.TestValues;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static io.spine.test.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Alexander Yevsyukov
 */
@SuppressWarnings({"SerializableInnerClassWithNonSerializableOuterClass"
        /* OK as custom routes do not refer to the test suite. */,
        "DuplicateStringLiteralInspection" /* Common test display names. */})
@DisplayName("EventRouting should")
class EventRoutingTest {

    /** The set of IDs returned by the {@link #customRoute}. */
    private static final ImmutableSet<Long> CUSTOM_ROUTE = ImmutableSet.of(5L, 6L, 7L);

    /** The set of IDs returned by the {@link #defaultRoute}. */
    private static final ImmutableSet<Long> DEFAULT_ROUTE = ImmutableSet.of(0L, 1L);

    /** The object under the test. */
    private EventRouting<Long> eventRouting;

    /** A custom route for {@code StringValue} messages. */
    private final EventRoute<Long, StringValue> customRoute = new EventRoute<Long, StringValue>() {

        private static final long serialVersionUID = 0L;

        @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType") // return immutable instance
        @Override
        public Set<Long> apply(StringValue message, EventContext context) {
            return CUSTOM_ROUTE;
        }
    };

    /** The default route to be used by the routing under the test. */
    private final EventRoute<Long, Message> defaultRoute = new EventRoute<Long, Message>() {

        private static final long serialVersionUID = 0L;

        @Override
        public Set<Long> apply(Message message, EventContext context) {
            return DEFAULT_ROUTE;
        }
    };

    private final TestEventFactory eventFactory = TestEventFactory.newInstance(getClass());

    @BeforeEach
    void setUp() {
        eventRouting = EventRouting.withDefault(defaultRoute);
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        NullPointerTester nullPointerTester = new NullPointerTester()
                .setDefault(EventContext.class, EventContext.getDefaultInstance());

        nullPointerTester.testAllPublicInstanceMethods(eventRouting);
        nullPointerTester.testAllPublicStaticMethods(EventRouting.class);
    }

    @Test
    @DisplayName("have default route")
    void haveDefaultRoute() throws Exception {
        assertNotNull(eventRouting.getDefault());
    }

    @Test
    @DisplayName("allow replacing default route")
    void allowReplacingDefaultRoute() {
        EventRoute<Long, Message> newDefault = new EventRoute<Long, Message>() {

            private static final long serialVersionUID = 0L;

            @Override
            public Set<Long> apply(Message message, EventContext context) {
                return ImmutableSet.of(10L, 20L);
            }
        };

        assertSame(eventRouting, eventRouting.replaceDefault(newDefault));

        assertSame(newDefault, eventRouting.getDefault());
    }

    @Test
    @DisplayName("set custom route")
    void setCustomRoute() {
        assertSame(eventRouting, eventRouting.route(StringValue.class, customRoute));

        Optional<EventRoute<Long, StringValue>> route = eventRouting.get(StringValue.class);

        assertTrue(route.isPresent());
        assertSame(customRoute, route.get());
    }

    @Test
    @DisplayName("not allow overwriting set route")
    void notOverwriteSetRoute() {
        eventRouting.route(StringValue.class, customRoute);
        assertThrows(IllegalStateException.class,
                     () -> eventRouting.route(StringValue.class, customRoute));
    }

    @Test
    @DisplayName("remove previously set route")
    void removePreviouslySetRoute() {
        eventRouting.route(StringValue.class, customRoute);
        eventRouting.remove(StringValue.class);

        assertFalse(eventRouting.doGet(StringValue.class)
                                .isPresent());
    }

    @Test
    @DisplayName("throw ISE on removal if route is not set")
    void notRemoveIfRouteNotSet() {
        assertThrows(IllegalStateException.class, () -> eventRouting.remove(StringValue.class));
    }

    @Test
    @DisplayName("apply default route")
    void applyDefaultRoute() {
        // Have custom route too.
        eventRouting.route(StringValue.class, customRoute);

        // An event which has `Timestamp` as its message.
        // It should go through the default route, because only `StringValue` has a custom route.
        Event event = eventFactory.createEvent(Time.getCurrentTime());

        Set<Long> ids = eventRouting.apply(event.getMessage(), event.getContext());
        assertEquals(DEFAULT_ROUTE, ids);
    }

    @Test
    @DisplayName("apply custom route")
    void applyCustomRoute() {
        eventRouting.route(StringValue.class, customRoute);

        // An event which has `StringValue` as its message, which should go the custom route.
        EventEnvelope event = EventEnvelope.of(
                eventFactory.createEvent(TestValues.newUuidValue()));

        Set<Long> ids = eventRouting.apply(event.getMessage(), event.getEventContext());
        assertEquals(CUSTOM_ROUTE, ids);
    }
}
