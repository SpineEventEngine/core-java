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

package io.spine.server.route;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.Identifier;
import io.spine.core.RejectionContext;
import io.spine.server.entity.rejection.Rejections.EntityAlreadyArchived;
import io.spine.server.entity.rejection.Rejections.EntityAlreadyDeleted;
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
public class RejectionRoutingShould {

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

    @Before
    public void setUp() {
        rejectionRouting = RejectionRouting.withDefault(defaultRoute);
    }

    @Test
    public void have_default_route() {
        assertNotNull(rejectionRouting.getDefault());
    }

    @Test
    public void allow_replacing_default_route() {
        final RejectionRoute<String, Message> newDefault =
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
    public void set_custom_route() {
        assertSame(rejectionRouting, rejectionRouting.route(EntityAlreadyArchived.class,
                                                            customRoute));

        final Optional<RejectionRoute<String, EntityAlreadyArchived>> route =
                rejectionRouting.get(EntityAlreadyArchived.class);

        assertTrue(route.isPresent());
        assertSame(customRoute, route.get());
    }

    @Test(expected = IllegalStateException.class)
    public void not_allow_overwriting_set_route() {
        rejectionRouting.route(EntityAlreadyArchived.class, customRoute);
        rejectionRouting.route(EntityAlreadyArchived.class, customRoute);
    }

    @Test
    public void remove_previously_set_route() {
        rejectionRouting.route(EntityAlreadyArchived.class, customRoute);
        rejectionRouting.remove(EntityAlreadyArchived.class);

        assertFalse(rejectionRouting.doGet(EntityAlreadyArchived.class)
                                    .isPresent());
    }

    @Test(expected = IllegalStateException.class)
    public void complain_on_removal_if_route_was_not_set() {
        rejectionRouting.remove(EntityAlreadyArchived.class);
    }

    @Test
    public void apply_default_route() {
        // Create a rejection for which there's no custom path.
        final EntityAlreadyDeleted rejection =
                EntityAlreadyDeleted.newBuilder()
                                    .setEntityId(thisTestAsEntity())
                                    .build();

        // Do have custom route.
        rejectionRouting.route(EntityAlreadyArchived.class, customRoute);

        final Set<String> ids = rejectionRouting.apply(rejection,
                                                       RejectionContext.getDefaultInstance());

        assertEquals(DEFAULT_ROUTE, ids);
    }

    @Test
    public void apply_custom_route() {
        rejectionRouting.route(EntityAlreadyArchived.class, customRoute);

        final EntityAlreadyArchived rejection =
                EntityAlreadyArchived.newBuilder()
                                     .setEntityId(thisTestAsEntity())
                                     .build();
        final Set<String> ids = rejectionRouting.apply(rejection,
                                                       RejectionContext.getDefaultInstance());
        assertEquals(CUSTOM_ROUTE, ids);
    }

    private Any thisTestAsEntity() {
        return Identifier.pack(getClass().getName());
    }

    @Test
    public void pass_null_tolerance_check() {
        final NullPointerTester nullPointerTester = new NullPointerTester()
                .setDefault(RejectionContext.class, RejectionContext.getDefaultInstance());

        nullPointerTester.testAllPublicInstanceMethods(rejectionRouting);
        nullPointerTester.testAllPublicStaticMethods(RejectionRouting.class);
    }
}
