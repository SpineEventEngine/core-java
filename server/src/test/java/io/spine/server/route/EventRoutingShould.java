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

package io.spine.server.route;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.EventEnvelope;
import io.spine.server.command.TestEventFactory;
import io.spine.test.TestValues;
import io.spine.time.Time;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("SerializableInnerClassWithNonSerializableOuterClass")
    // OK as custom routes do not refer to the test suite.
public class EventRoutingShould {

    /** The set of IDs returned by the {@link #customRoute}. */
    private static final ImmutableSet<Long> CUSTOM_ROUTE = ImmutableSet.of(5L, 6L, 7L);

    /** The set of IDs returned by the {@link #defaultRoute}. */
    private static final ImmutableSet<Long> DEFAULT_ROUTE = ImmutableSet.of(0L, 1L);

    /** The object under the test. */
    private EventRouting<Long> eventRouting;

    /** A custom route for {@code StringValue} messages. */
    private final EventRoute<Long, StringValue> customRoute = new EventRoute<Long, StringValue>() {

        private static final long serialVersionUID = 0L;

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

    @Before
    public void setUp() {
        eventRouting = EventRouting.withDefault(defaultRoute);
    }

    @Test
    public void have_default_route() throws Exception {
        assertNotNull(eventRouting.getDefault());
    }

    @Test
    public void allow_replacing_default_route() {
        final EventRoute<Long, Message> newDefault = new EventRoute<Long, Message>() {

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
    public void set_custom_route() {
        assertSame(eventRouting, eventRouting.route(StringValue.class, customRoute));

        final Optional<EventRoute<Long, StringValue>> route = eventRouting.get(StringValue.class);

        assertTrue(route.isPresent());
        assertSame(customRoute, route.get());
    }

    @Test(expected = IllegalStateException.class)
    public void not_allow_overwriting_a_set_route() {
        eventRouting.route(StringValue.class, customRoute);
        eventRouting.route(StringValue.class, customRoute);
    }

    @Test
    public void remove_previously_set_route() {
        eventRouting.route(StringValue.class, customRoute);
        eventRouting.remove(StringValue.class);

        assertFalse(eventRouting.doGet(StringValue.class)
                                .isPresent());
    }

    @Test(expected = IllegalStateException.class)
    public void complain_on_removal_if_route_was_not_set() {
        eventRouting.remove(StringValue.class);
    }

    @Test
    public void apply_default_route() {
        // Have custom route too.
        eventRouting.route(StringValue.class, customRoute);

        // An event which has `Timestamp` as its message.
        // It should go through the default route, because only `StringValue` has a custom route.
        final Event event = eventFactory.createEvent(Time.getCurrentTime());

        final Set<Long> ids = eventRouting.apply(event.getMessage(), event.getContext());
        assertEquals(DEFAULT_ROUTE, ids);
    }

    @Test
    public void apply_custom_route() {
        eventRouting.route(StringValue.class, customRoute);

        // An event which has `StringValue` as its message, which should go the custom route.
        final EventEnvelope event = EventEnvelope.of(
                eventFactory.createEvent(TestValues.newUuidValue()));

        final Set<Long> ids = eventRouting.apply(event.getMessage(), event.getEventContext());
        assertEquals(CUSTOM_ROUTE, ids);
    }

    @Test
    public void pass_null_tolerance() {
        final NullPointerTester nullPointerTester = new NullPointerTester()
                .setDefault(EventContext.class, EventContext.getDefaultInstance());

        nullPointerTester.testAllPublicInstanceMethods(eventRouting);
        nullPointerTester.testAllPublicStaticMethods(EventRouting.class);
    }
}
