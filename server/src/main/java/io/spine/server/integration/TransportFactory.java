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

import io.spine.annotation.SPI;
import io.spine.server.integration.route.matcher.ChannelMatcher;

/**
 * A factory for creating channel-based transport for {@code Message} inter-exchange between the
 * current deployment component and other application parts.
 *
 * Inspired by <a href="http://www.enterpriseintegrationpatterns.com/patterns/messaging/PublishSubscribeChannel.html">
 * Publish-Subscriber Channel pattern.</a>
 *
 * @author Alex Tymchenko
 * @author Dmitry Ganzha
 */
@SPI
public interface TransportFactory {

    /**
     * Creates a {@link Publisher} for the messages that
     * {@linkplain ChannelMatcher#match(ChannelId, ExternalMessage)} correspond}
     * to the passed channel ID.
     *
     * @param channelId the identifier of the channel
     * @return a new {@code Publisher} instance
     */
    Publisher createPublisher(ChannelId channelId);

    /**
     * Creates a {@link Subscriber} for the messages that correspond to the passed channel ID.
     *
     * @param channelId the identifier of the channel
     * @return a new {@code Subscriber} instance
     */
    Subscriber createSubscriber(ChannelId channelId);
}
