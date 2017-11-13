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

import io.spine.core.BoundedContextName;
import io.spine.core.Event;
import io.spine.server.BoundedContext;
import io.spine.server.integration.route.ChannelRoute;
import io.spine.server.integration.route.Route;
import io.spine.test.integration.event.ItgProjectStarted;
import org.junit.Test;

import static io.spine.core.EventClass.of;
import static io.spine.server.integration.Channels.newId;
import static io.spine.server.integration.given.IntegrationBusTestEnv.projectCreated;
import static io.spine.server.integration.given.IntegrationBusTestEnv.projectStarted;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Dmitry Ganzha
 */
public class ChannelRouteShould {

    @Test
    public void accept_the_suitable_message_for_the_route() {
        final BoundedContextName boundedContextName = BoundedContext.newName("External context ID");
        final Route route = new ChannelRoute(newId(of(ItgProjectStarted.class)));
        final Event projectStarted = projectStarted();
        final ExternalMessage externalProjectStarted = ExternalMessages.of(projectStarted,
                                                                           boundedContextName);

        final MessageRouted messageRouted = route.accept(externalProjectStarted);

        assertTrue(messageRouted.getMessageMatched()
                                .getMatched());
        assertTrue(messageRouted.getMessageMatched()
                                .getDescription()
                                .isEmpty());
    }

    @Test
    public void not_accept_the_not_suitable_message_for_the_route() {
        final BoundedContextName boundedContextName = BoundedContext.newName("External context ID");
        final Route route = new ChannelRoute(newId(of(ItgProjectStarted.class)));
        final Event projectCreated = projectCreated();
        final ExternalMessage externalProjectCreated = ExternalMessages.of(projectCreated,
                                                                           boundedContextName);

        final MessageRouted messageRouted = route.accept(externalProjectCreated);

        assertFalse(messageRouted.getMessageMatched()
                                 .getMatched());
        assertFalse(messageRouted.getMessageMatched()
                                 .getDescription()
                                 .isEmpty());
    }
}
