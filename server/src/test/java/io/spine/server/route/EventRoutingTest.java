/*
 * Copyright 2019, TeamDev. All rights reserved.
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

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.NullPointerTester;
import com.google.common.truth.OptionalSubject;
import com.google.common.truth.Truth8;
import io.spine.base.EventMessage;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.server.type.EventEnvelope;
import io.spine.server.type.given.GivenEvent;
import io.spine.test.route.AccountSuspended;
import io.spine.test.route.LoginEvent;
import io.spine.test.route.UserAccountEvent;
import io.spine.test.route.UserEvent;
import io.spine.test.route.UserLoggedIn;
import io.spine.test.route.UserLoggedOut;
import io.spine.test.route.UserRegistered;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static io.spine.testing.TestValues.random;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/* OK as custom routes do not refer to the test suite. */
@SuppressWarnings("SerializableInnerClassWithNonSerializableOuterClass")
@DisplayName("EventRouting should")
class EventRoutingTest {

    /** The set of IDs returned by the {@link #defaultRoute}. */
    private static final ImmutableSet<Long> DEFAULT_ROUTE = ImmutableSet.of(0L, 1L);

    /** The set of IDs returned by the {@link #customRoute}. */
    private static final ImmutableSet<Long> CUSTOM_ROUTE = ImmutableSet.of(5L, 6L, 7L);

    /** The set of IDs returned by the {@link #alternativeRoute}. */
    private static final ImmutableSet<Long> ALT_ROUTE = ImmutableSet.of(100L, 200L, 300L, 400L);

    /** The object under the test. */
    private EventRouting<Long> eventRouting;

    /** The default route to be used by the routing under the test. */
    private final EventRoute<Long, EventMessage> defaultRoute = (event, context) -> DEFAULT_ROUTE;

    /** A custom route. */
    private final EventRoute<Long, UserEvent> customRoute = (event, context) -> CUSTOM_ROUTE;

    /** Another custom route. */
    private final EventRoute<Long, UserEvent> alternativeRoute = (event, context) -> ALT_ROUTE;

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
    void haveDefaultRoute() {
        assertNotNull(eventRouting.defaultRoute());
    }

    @Test
    @DisplayName("allow replacing default route")
    void allowReplacingDefaultRoute() {
        EventRoute<Long, EventMessage> newDefault = new EventRoute<Long, EventMessage>() {

            private static final long serialVersionUID = 0L;

            @Override
            public Set<Long> apply(EventMessage message, EventContext context) {
                return ImmutableSet.of(10L, 20L);
            }
        };

        assertThat(eventRouting.replaceDefault(newDefault))
                .isSameInstanceAs(eventRouting);
        assertThat(eventRouting.defaultRoute())
                .isSameInstanceAs(newDefault);
    }

    @Test
    @DisplayName("set custom route")
    void setCustomRoute() {
        assertThat(eventRouting.route(UserRegistered.class, customRoute))
                .isSameInstanceAs(eventRouting);

        Optional<EventRoute<Long, UserRegistered>> route = eventRouting.get(UserRegistered.class);

        OptionalSubject assertRoute = Truth8.assertThat(route);
        assertRoute.isPresent();
        assertRoute.hasValue(customRoute);
    }

    @Test
    @DisplayName("not allow overwriting set route")
    void notOverwriteSetRoute() {
        eventRouting.route(UserRegistered.class, customRoute);
        assertThrows(IllegalStateException.class,
                     () -> eventRouting.route(UserRegistered.class, customRoute));
    }

    @Test
    @DisplayName("remove previously set route")
    void removePreviouslySetRoute() {
        eventRouting.route(UserRegistered.class, customRoute);
        eventRouting.remove(UserRegistered.class);

        assertThat(eventRouting.routeFor(UserLoggedIn.class)
                               .found())
                .isFalse();
    }

    @Test
    @DisplayName("throw ISE on removal if route is not set")
    void notRemoveIfRouteNotSet() {
        assertThrows(IllegalStateException.class, () -> eventRouting.remove(UserRegistered.class));
    }

    @Test
    @DisplayName("apply default route")
    void applyDefaultRoute() {
        // Have custom route too.
        eventRouting.route(UserRegistered.class, customRoute);

        Event event = GivenEvent.arbitrary();
        Set<Long> ids = eventRouting.apply(event.enclosedMessage(), event.context());

        assertThat(ids)
                .isEqualTo(DEFAULT_ROUTE);
    }

    @Test
    @DisplayName("apply custom route")
    void applyCustomRoute() {
        eventRouting.route(UserRegistered.class, customRoute);

        UserRegistered eventMessage = UserRegistered
                .newBuilder()
                .setId(random(1, 100))
                .build();
        EventEnvelope event = EventEnvelope.of(GivenEvent.withMessage(eventMessage));

        Set<Long> ids = eventRouting.apply(event.message(), event.context());
        assertThat(ids)
                .isEqualTo(CUSTOM_ROUTE);
    }

    @Test
    @DisplayName("allow routing via interface")
    void routesViaInterface() {
        eventRouting.route(UserLoggedOut.class, alternativeRoute)
                    .route(LoginEvent.class, customRoute);

        EventContext ctx = EventContext.getDefaultInstance();

        // Check routing via common interface `LoginEvent`.
        assertThat(eventRouting.apply(UserLoggedIn.getDefaultInstance(), ctx))
                .isEqualTo(CUSTOM_ROUTE);

        // Check routing via specific type.
        assertThat(eventRouting.apply(UserLoggedOut.getDefaultInstance(), ctx))
                .isEqualTo(ALT_ROUTE);
    }

    @Test
    @DisplayName("prohibit adding specific type after interface routing")
    void overridingInterfaceRouting() {
        assertThrows(IllegalStateException.class, () ->
                eventRouting.route(UserAccountEvent.class, alternativeRoute)
                            .route(AccountSuspended.class, customRoute)
        );
    }
}
