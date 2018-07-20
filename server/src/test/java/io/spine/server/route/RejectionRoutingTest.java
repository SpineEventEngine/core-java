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

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.base.Identifier;
import io.spine.core.RejectionContext;
import io.spine.server.entity.rejection.StandardRejections.EntityAlreadyArchived;
import io.spine.server.entity.rejection.StandardRejections.EntityAlreadyDeleted;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
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
@DisplayName("RejectionRouting should")
class RejectionRoutingTest {

    /** The set of IDs returned by the {@link #defaultRoute}. */
    private static final ImmutableSet<String> DEFAULT_ROUTE = ImmutableSet.of("α", "β", "γ");

    /** The set of IDs returned by the {@link #customRoute}. */
    private static final ImmutableSet<String> CUSTOM_ROUTE = ImmutableSet.of("κ", "λ", "μ");

    /** The default route to be used by the routing under the test. */
    private final RejectionRoute<String, Message> defaultRoute =
            new RejectionRoute<String, Message>() {

                private static final long serialVersionUID = 0L;

                @Override
                public Set<String> apply(Message message, RejectionContext context) {
                    return DEFAULT_ROUTE;
                }
            };

    /** A custom route for {@code EntityAlreadyArchived} messages. */
    private final RejectionRoute<String, EntityAlreadyArchived> customRoute =
            new RejectionRoute<String, EntityAlreadyArchived>() {

                private static final long serialVersionUID = 0L;

                @Override
                public Set<String> apply(EntityAlreadyArchived message, RejectionContext context) {
                    return CUSTOM_ROUTE;
                }
            };

    /** The object under the test. */
    private RejectionRouting<String> rejectionRouting;

    @BeforeEach
    void setUp() {
        rejectionRouting = RejectionRouting.withDefault(defaultRoute);
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        NullPointerTester nullPointerTester = new NullPointerTester()
                .setDefault(RejectionContext.class, RejectionContext.getDefaultInstance());

        nullPointerTester.testAllPublicInstanceMethods(rejectionRouting);
        nullPointerTester.testAllPublicStaticMethods(RejectionRouting.class);
    }

    @Test
    @DisplayName("have default route")
    void haveDefaultRoute() {
        assertNotNull(rejectionRouting.getDefault());
    }

    @Test
    @DisplayName("allow replacing default route")
    void allowReplacingDefaultRoute() {
        RejectionRoute<String, Message> newDefault =
                new RejectionRoute<String, Message>() {
                    private static final long serialVersionUID = 0L;

                    @Override
                    public Set<String> apply(Message message, RejectionContext context) {
                        return ImmutableSet.of("a", "b", "c");
                    }
                };

        assertSame(rejectionRouting, rejectionRouting.replaceDefault(newDefault));

        assertSame(newDefault, rejectionRouting.getDefault());
    }

    @Test
    @DisplayName("set custom route")
    void setCustomRoute() {
        assertSame(rejectionRouting, rejectionRouting.route(EntityAlreadyArchived.class,
                                                            customRoute));

        Optional<RejectionRoute<String, EntityAlreadyArchived>> route =
                rejectionRouting.get(EntityAlreadyArchived.class);

        assertTrue(route.isPresent());
        assertSame(customRoute, route.get());
    }

    @Test
    @DisplayName("not allow overwriting set route")
    void notOverwriteSetRoute() {
        rejectionRouting.route(EntityAlreadyArchived.class, customRoute);
        assertThrows(IllegalStateException.class,
                     () -> rejectionRouting.route(EntityAlreadyArchived.class, customRoute));
    }

    @Test
    @DisplayName("remove previously set route")
    void removePreviouslySetRoute() {
        rejectionRouting.route(EntityAlreadyArchived.class, customRoute);
        rejectionRouting.remove(EntityAlreadyArchived.class);

        assertFalse(rejectionRouting.doGet(EntityAlreadyArchived.class)
                                    .isPresent());
    }

    @Test
    @DisplayName("throw ISE on removal if route is not set")
    void notRemoveIfRouteNotSet() {
        assertThrows(IllegalStateException.class,
                     () -> rejectionRouting.remove(EntityAlreadyArchived.class));
    }

    @Test
    @DisplayName("apply default route")
    void applyDefaultRoute() {
        // Create a rejection for which there's no custom path.
        EntityAlreadyDeleted rejection = EntityAlreadyDeleted.newBuilder()
                                                             .setEntityId(thisTestAsEntity())
                                                             .build();

        // Do have custom route.
        rejectionRouting.route(EntityAlreadyArchived.class, customRoute);

        Set<String> ids = rejectionRouting.apply(rejection, RejectionContext.getDefaultInstance());

        assertEquals(DEFAULT_ROUTE, ids);
    }

    @Test
    @DisplayName("apply custom route")
    void applyCustomRoute() {
        rejectionRouting.route(EntityAlreadyArchived.class, customRoute);

        EntityAlreadyArchived rejection = EntityAlreadyArchived.newBuilder()
                                                               .setEntityId(thisTestAsEntity())
                                                               .build();
        Set<String> ids = rejectionRouting.apply(rejection, RejectionContext.getDefaultInstance());
        assertEquals(CUSTOM_ROUTE, ids);
    }

    private Any thisTestAsEntity() {
        return Identifier.pack(getClass().getName());
    }
}
