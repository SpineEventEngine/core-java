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

package io.spine.server.integration.route;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import io.spine.server.integration.ChannelHub;
import io.spine.server.integration.ExternalMessage;
import io.spine.server.integration.MessageChannel;
import io.spine.server.integration.MessageRouted;

import static com.google.common.collect.FluentIterable.from;

/**
 * @author Dmitry Ganzha
 */
public abstract class AbstractMessageRouter<C extends MessageChannel, I> implements RouterManagement<I> {

    private final ChannelHub<C> channelHub;

    private final RouteHolder<I> routeHolder;

    public AbstractMessageRouter(ChannelHub<C> channelHub, RouteHolder<I> routeHolder) {
        this.channelHub = channelHub;
        this.routeHolder = routeHolder;
    }

    public abstract Iterable<C> route(ExternalMessage message);

    /**
     * Subclasses must implement this method to return a Collection of zero or more
     * MessageChannels to which the given Message should be routed.
     *
     * @param message the message
     * @return the collection of message channels
     */
    protected abstract Iterable<C> determineTargetChannels(ExternalMessage message);

    protected Iterable<Route<I>> findSuitableRoutes(final ExternalMessage message) {
        ImmutableSet<Route<I>> suitableRoutes =
                from(routeHolder.getRoutes())
                        .filter(new Predicate<Route<I>>() {
                            @Override
                            public boolean apply(Route<I> input) {
                                final MessageRouted messageRouted = input.accept(message);
                                return messageRouted.getRouted();
                            }
                        })
                        .toSet();
        return suitableRoutes;
    }

    protected ChannelHub<C> getChannelHub() {
        return channelHub;
    }

    @Override
    public void addRoute(Route<I> route) {
        routeHolder.addRoute(route);
    }

    @Override
    public void removeRoute(Route<I> route) {
        routeHolder.removeRoute(route);
    }

    @Override
    public void close() throws Exception {
        channelHub.close();
    }
}
