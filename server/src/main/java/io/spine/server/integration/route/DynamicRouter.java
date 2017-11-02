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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import io.spine.server.integration.ChannelHub;
import io.spine.server.integration.ChannelId;
import io.spine.server.integration.ExternalMessage;
import io.spine.server.integration.Publisher;
import io.spine.server.integration.memory.InMemoryRouteHolder;

import java.util.Set;

import static com.google.common.collect.FluentIterable.from;

public class DynamicRouter extends AbstractMessageRouter<Publisher, ChannelId> {

    public DynamicRouter(ChannelHub<Publisher> channelHub,
                         RouteHolder<ChannelId> routeHolder) {
        super(channelHub, routeHolder);
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
    protected Iterable<Publisher> determineTargetChannels(ExternalMessage message) {
        final Iterable<Route<ChannelId>> suitableRoutes = findSuitableRoutes(message);
        final ImmutableSet<Publisher> targetChannels =
                from(suitableRoutes)
                        .transform(new Function<Route<ChannelId>, Publisher>() {
                            @Override
                            public Publisher apply(Route<ChannelId> input) {
                                final Publisher publisher = getChannelHub().get(
                                        input.getChannelIdentifier());
                                return publisher;
                            }
                        })
                        .toSet();
        return targetChannels;
    }
}
