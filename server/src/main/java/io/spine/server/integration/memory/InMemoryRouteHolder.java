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

package io.spine.server.integration.memory;

import com.google.common.collect.ImmutableSet;
import io.spine.server.integration.ChannelId;
import io.spine.server.integration.route.Route;
import io.spine.server.integration.route.RouteHolder;

import java.util.Set;

public class InMemoryRouteHolder implements RouteHolder<ChannelId> {
    private final Set<Route<ChannelId>> routes;

    public InMemoryRouteHolder(Set<Route<ChannelId>> routes) {
        this.routes = routes;
    }

    @Override
    public void addRoute(Route<ChannelId> route) {
        routes.add(route);
    }

    @Override
    public void removeRoute(Route<ChannelId> route) {
        routes.remove(route);
    }

    @Override
    public Iterable<Route<ChannelId>> getRoutes() {
        return ImmutableSet.copyOf(routes);
    }
}
