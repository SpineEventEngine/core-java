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
import com.google.common.collect.Sets;
import io.spine.server.integration.route.Route;
import io.spine.server.integration.route.RoutingSchema;

import java.util.Set;

/**
 * An in-memory implementation of the {@link RoutingSchema}.
 *
 * @author Dmitry Ganzha
 */
public class InMemoryRoutingSchema implements RoutingSchema {
    private final Set<Route> routes;

    /** Prevent direct instantiation from the outside. */
    private InMemoryRoutingSchema() {
        this.routes = Sets.newConcurrentHashSet();
    }

    /**
     * Creates a new instance of {@code InMemoryRoutingSchema}.
     */
    public static InMemoryRoutingSchema newInstance() {
        return new InMemoryRoutingSchema();
    }

    @Override
    public void add(Route route) {
        routes.add(route);
    }

    @Override
    public void remove(Route route) {
        routes.remove(route);
    }

    @Override
    public Iterable<Route> getAll() {
        return ImmutableSet.copyOf(routes);
    }
}
