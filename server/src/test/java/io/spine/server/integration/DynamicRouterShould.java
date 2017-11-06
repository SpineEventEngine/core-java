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

package io.spine.server.integration;

import com.google.common.collect.Lists;
import io.spine.core.BoundedContextName;
import io.spine.core.Event;
import io.spine.core.Rejection;
import io.spine.server.BoundedContext;
import io.spine.server.integration.memory.InMemoryTransportFactory;
import io.spine.server.integration.route.ChannelRoute;
import io.spine.server.integration.route.DynamicRouter;
import io.spine.server.integration.route.Route;
import io.spine.server.integration.route.Router;
import io.spine.server.integration.route.RoutingSchema;
import io.spine.test.integration.event.ItgProjectCreated;
import io.spine.test.integration.event.ItgProjectStarted;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;

import static com.google.common.collect.FluentIterable.from;
import static io.spine.core.EventClass.of;
import static io.spine.server.integration.Channels.newId;
import static io.spine.server.integration.given.IntegrationBusTestEnv.cannotStartArchivedProject;
import static io.spine.server.integration.given.IntegrationBusTestEnv.projectCreated;
import static io.spine.server.integration.given.IntegrationBusTestEnv.projectStarted;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author Dmitry Ganzha
 */
public class DynamicRouterShould {
    @Test
    public void route_the_message_to_suitable_routes() {
        final BoundedContextName boundedContextName = BoundedContext.newName("External context ID");

        final List<Route> routes = Lists.newArrayList();
        routes.add(new ChannelRoute(newId(of(ItgProjectStarted.class))));
        routes.add(new ChannelRoute(newId(of(ItgProjectCreated.class))));

        final RoutingSchema mockRoutingSchema = Mockito.mock(RoutingSchema.class);
        when(mockRoutingSchema.getAll()).thenReturn(routes);

        final PublisherHub publisherHub = new PublisherHub(InMemoryTransportFactory.newInstance());
        final Router router = new DynamicRouter<>(publisherHub, mockRoutingSchema);

        final Event projectCreated = projectCreated();
        final Event projectStarted = projectStarted();
        final Rejection notRegisteredRejection = cannotStartArchivedProject();

        final ExternalMessage externalProjectCreated = ExternalMessages.of(projectCreated,
                                                                           boundedContextName);
        final ExternalMessage externalProjectStarted = ExternalMessages.of(projectStarted,
                                                                           boundedContextName);
        final ExternalMessage externalNotRegistered = ExternalMessages.of(notRegisteredRejection,
                                                                          boundedContextName);

        Iterable<Publisher> projectCreatedChannels = router.route(externalProjectCreated);
        Iterable<Publisher> projectStartedChannels = router.route(externalProjectStarted);
        Iterable<Publisher> channelsForNotRegistered = router.route(externalNotRegistered);

        assertFalse(from(projectCreatedChannels)
                            .isEmpty());
        assertFalse(from(projectStartedChannels)
                            .isEmpty());
        assertTrue(from(channelsForNotRegistered)
                           .isEmpty());

    }

}
