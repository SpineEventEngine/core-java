/*
 * Copyright 2018, TeamDev. All rights reserved.
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

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.core.Event;
import io.spine.protobuf.AnyPacker;
import io.spine.server.transport.MessageChannel;
import io.spine.type.TypeUrl;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protobuf.AnyPacker.unpack;

/**
 * A utility class for working with {@link MessageChannel message channels} and their
 * {@link ChannelId identifiers}, when they are used for {@link IntegrationBus} needs.
 *
 * @author Alex Tymchenko
 */
class IntegrationChannels {

    private static final TypeUrl EVENT_TYPE_URL = TypeUrl.of(Event.class);

    /**
     * Prevents the creation of the class instances.
     */
    private IntegrationChannels() {
    }

    /**
     * Creates a channel ID for a channel, serving to exchange the
     * {@linkplain io.spine.core.Subscribe#external() external messages} of a specified class.
     *
     * @param messageCls the class of external messages, that will be exchanged via the channel,
     *                   which ID is being created
     * @return the newly created channel ID
     */
    static ChannelId toId(ExternalMessageClass messageCls) {
        checkNotNull(messageCls);

        ChannelId result = toId(messageCls.value());
        return result;
    }

    /**
     * Creates a channel ID for a channel, serving to exchange the messages of a specified type.
     *
     * @param messageType the type of messages, that will be exchanged via the channel,
     *                    which ID is being created
     * @return the newly created channel ID
     */
    static ChannelId toId(Class<? extends Message> messageType) {
        checkNotNull(messageType);

        TypeUrl typeUrl = TypeUrl.of(messageType);

        StringValue asStringValue = StringValue
                .newBuilder()
                .setValue(typeUrl.value())
                .build();
        Any packed = AnyPacker.pack(asStringValue);
        ChannelId channelId = ChannelId.newBuilder()
                                       .setIdentifier(packed)
                                       .build();
        return channelId;
    }

    /**
     * Unpacks the channel ID and interprets it as {@code ExternalMessageType}.
     *
     * <p>This is an application of a generic nature of {@code ChannelId}, allowing to identify
     * channels by the types of external messages, that are travelling through these channels.
     *
     * @param channelId the channel identifier to be interpreted as {@code ExternalMessageType}
     * @return the type of external messages, that are being exchanged through this channel
     */
    static ExternalMessageType fromId(ChannelId channelId) {
        checkNotNull(channelId);

        StringValue rawValue = (StringValue) unpack(channelId.getIdentifier());
        TypeUrl typeUrl = TypeUrl.parse(rawValue.getValue());

        ExternalMessageType result = ExternalMessageType
                .newBuilder()
                .setMessageTypeUrl(typeUrl.value())
                .setWrapperTypeUrl(EVENT_TYPE_URL.value())
                .build();
        return result;
    }
}
