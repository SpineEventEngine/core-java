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

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import io.spine.type.MessageClass;
import io.spine.type.TypeUrl;

import java.util.Set;

/**
 * Utilities for working with {@linkplain MessageChannel message channels}.
 *
 * @author Dmitry Ganzha
 */
public final class Channels {

    /** Prevents instantiation of this utility class. */
    private Channels() {}

    /**
     * Generates the {@code ChannelId} for the passed {@code MessageClass}.
     *
     * @param messageClass a message class for which a channel identifier is generated
     * @return a channel identifier
     */
    public static ChannelId newId(MessageClass messageClass) {
        final String messageTypeUrl = TypeUrl.of(messageClass.value())
                                             .value();
        final ChannelId channelId = ChannelId.newBuilder()
                                             .setMessageTypeUrl(messageTypeUrl)
                                             .build();
        return channelId;
    }

    /**
     * Generates the string representations for the passed message channels.
     *
     * @param messageChannels message channels for which string representations are generated
     * @return the string representations for the passed message channels
     */
    public static Set<String> from(Iterable<? extends MessageChannel> messageChannels) {
        final ImmutableSet<String> result =
                FluentIterable.from(messageChannels)
                              .transform(new Function<MessageChannel, String>() {
                                  @Override
                                  public String apply(MessageChannel input) {
                                      return input.toString();
                                  }
                              })
                              .toSet();
        return result;
    }
}
