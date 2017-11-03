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

import io.spine.server.integration.ChannelId;
import io.spine.server.integration.ChannelId.KindCase;
import io.spine.server.integration.ExternalMessage;
import io.spine.server.integration.MessageRouted;
import io.spine.server.integration.MessageSuitable;
import io.spine.server.integration.route.action.ChannelAction;
import io.spine.server.integration.route.action.SuitableByMessageTypeAction;

import java.util.Map;
import java.util.Objects;

import static com.google.common.collect.Maps.newHashMap;

/**
 * The {@code ChannelRoute} checks if the {@code ExternalMessage} is suitable for the
 * {@code MessageChannel} assigned to this route.
 *
 * @author Dmitry Ganzha
 */
public class ChannelRoute implements Route {

    /**
     * The map contains a kind related to a {@code ChannelAction}.
     */
    private static final Map<KindCase, ChannelAction> ACTIONS_BY_CHANNEL_KIND = newHashMap();

    static {
        ACTIONS_BY_CHANNEL_KIND.put(KindCase.MESSAGE_TYPE_URL, new SuitableByMessageTypeAction());
    }

    private final ChannelId channelId;

    public ChannelRoute(ChannelId channelId) {
        this.channelId = channelId;
    }

    @Override
    public MessageRouted accept(ExternalMessage message) {
        final KindCase kind = channelId.getKindCase();
        final ChannelAction action = ACTIONS_BY_CHANNEL_KIND.get(kind);
        final MessageSuitable messageSuitable = action.perform(channelId, message);
        return MessageRouted.newBuilder()
                            .setSource(message)
                            .setMessageSuitable(messageSuitable)
                            .build();
    }

    @Override
    public ChannelId getChannelIdentifier() {
        return channelId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(channelId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        ChannelRoute that = (ChannelRoute) o;
        return Objects.equals(channelId, that.channelId);
    }
}
