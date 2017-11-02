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
import io.spine.server.integration.ExternalMessage;
import io.spine.server.integration.MessageRouted;
import io.spine.server.integration.validator.ChannelValidator;
import io.spine.server.integration.validator.MessageTypeChannelValidator;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

public class ChannelRoute implements Route<ChannelId> {
    /**
     * The map contains a kind related to a {@code ChannelValidator}.
     */
    private static final Map<ChannelId.KindCase, ChannelValidator>
            VALIDATORS_BY_CHANNEL_KIND = newHashMap();

    static {
        VALIDATORS_BY_CHANNEL_KIND.put(ChannelId.KindCase.MESSAGE_TYPE_URL,
                                       new MessageTypeChannelValidator());
    }

    private final ChannelId channelId;

    public ChannelRoute(ChannelId channelId) {
        this.channelId = channelId;
    }

    @Override
    public MessageRouted accept(ExternalMessage message) {
        final ChannelId.KindCase kind = channelId.getKindCase();
        final ChannelValidator channelValidator = VALIDATORS_BY_CHANNEL_KIND.get(kind);
        final boolean isValid = channelValidator.validate(channelId, message);
        return MessageRouted.newBuilder()
                            .setSource(message)
                            .setRouted(isValid)
                            .build();
    }

    @Override
    public ChannelId getChannelIdentifier() {
        return channelId;
    }
}
