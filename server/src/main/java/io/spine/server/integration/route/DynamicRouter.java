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
import io.spine.server.integration.ChannelId;
import io.spine.server.integration.ExternalMessage;
import io.spine.server.integration.MessageRouted;
import io.spine.server.integration.Publisher;

import java.util.Set;

import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Sets.newConcurrentHashSet;

/**
 * The {@code DynamicRouter} routes messages to suitable {@code MessageChannel}s
 * based on various rules.
 *
 * @author Dmitry Ganzha
 */
public class DynamicRouter implements Router {

    private final ChannelHub<Publisher> channelHub;
    private final Set<Route> routes;
    private final Publisher deadMessageChannel;
    private final RouterErrorHandler deadMessageHandler;

    public DynamicRouter(ChannelHub<Publisher> channelHub, ChannelId deadMessageChannelId) {
        this.channelHub = channelHub;
        this.routes = newConcurrentHashSet();
        this.deadMessageChannel = deadMessageChannel(deadMessageChannelId);
        this.deadMessageHandler = new DeadMessageHandler(deadMessageChannel);
    }

    @Override
    public Iterable<Publisher> route(ExternalMessage message) {
        final Iterable<Publisher> messageChannels = determineTargetChannels(message);

        if (from(messageChannels).isEmpty()) {
            deadMessageHandler.handle(message);
            return ImmutableSet.of(deadMessageChannel);
        }

        for (Publisher messageChannel : messageChannels) {
            messageChannel.publish(message.getId(), message);
        }
        return messageChannels;
    }

    @Override
    public void register(Route route) {
        routes.add(route);
    }

    @Override
    public void unregister(Route route) {
        routes.remove(route);
    }

    @Override
    public void close() throws Exception {
        channelHub.close();
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
                from(routes)
                        .filter(new Predicate<Route>() {
                            @Override
                            public boolean apply(Route input) {
                                final MessageRouted messageRouted = input.accept(message);
                                return messageRouted.getMessageMatched()
                                                    .getMatched();
                            }
                        })
                        .toSet();
        return suitableRoutes;
    }

    /**
     * Returns the dead message channel. Which will be used for messages that were not acceptable
     * by any {@code Route}.
     */
    private Publisher deadMessageChannel(ChannelId deadMessageChannelId) {
        return channelHub.get(deadMessageChannelId);
    }
}
