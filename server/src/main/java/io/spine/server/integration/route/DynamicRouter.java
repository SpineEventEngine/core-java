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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import io.spine.server.integration.ChannelHub;
import io.spine.server.integration.ExternalMessage;
import io.spine.server.integration.MessageRouted;
import io.spine.server.integration.Publisher;

import static com.google.common.collect.FluentIterable.from;

/**
 * The {@code DynamicRouter} routes messages to suitable {@code MessageChannel}s
 * based on various rules.
 *
 * @param <P> the type of the publisher message channel
 * @author Dmitry Ganzha
 */
public class DynamicRouter<P extends Publisher> implements Router {

    private final ChannelHub<P> channelHub;

    private final RoutingSchema routingSchema;

    public DynamicRouter(ChannelHub<P> channelHub, RoutingSchema routingSchema) {
        this.channelHub = channelHub;
        this.routingSchema = routingSchema;
    }

    @Override
    public Iterable<Publisher> route(ExternalMessage message) {
        final Iterable<Publisher> messageChannels = determineTargetChannels(message);
        for (Publisher messageChannel : messageChannels) {
            messageChannel.publish(message.getId(), message);
        }
        return messageChannels;
    }

    @Override
    public void register(Route route) {
        routingSchema.add(route);
    }

    @Override
    public void unregister(Route route) {
        routingSchema.remove(route);
    }

    private Iterable<Publisher> determineTargetChannels(ExternalMessage message) {
        final Iterable<Route> suitableRoutes = findSuitableRoutes(message);
        final ImmutableSet<Publisher> targetChannels =
                from(suitableRoutes)
                        .transform(new Function<Route, Publisher>() {
                            @Override
                            public Publisher apply(Route input) {
                                final Publisher publisher =
                                        channelHub.get(input.getChannelIdentifier());
                                return publisher;
                            }
                        })
                        .toSet();
        return targetChannels;
    }

    private Iterable<Route> findSuitableRoutes(final ExternalMessage message) {
        final ImmutableSet<Route> suitableRoutes =
                from(routingSchema.getAll())
                        .filter(new Predicate<Route>() {
                            @Override
                            public boolean apply(Route input) {
                                final MessageRouted messageRouted = input.accept(message);
                                return messageRouted.getMessageSuitable()
                                                    .getSuitable();
                            }
                        })
                        .toSet();
        return suitableRoutes;
    }

    @Override
    public void close() throws Exception {
        channelHub.close();
    }
}
