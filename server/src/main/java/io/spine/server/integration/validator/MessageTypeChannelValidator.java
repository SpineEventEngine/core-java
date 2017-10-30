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

package io.spine.server.integration.validator;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.protobuf.AnyPacker;
import io.spine.server.integration.ChannelId;
import io.spine.server.integration.ExternalMessage;
import io.spine.server.integration.RequestForExternalMessages;
import io.spine.type.TypeUrl;

/**
 * The {@code MessageTypeChannelValidator} checks if the message is suitable for the channel by the
 * message type.
 *
 * @author Dmitry Ganzha
 */
public class MessageTypeChannelValidator implements ChannelValidator {

    private static final String MESSAGE_FIELD_NAME = "message";

    @Override
    public boolean validate(ChannelId channelId, ExternalMessage message) {
        final String typeUrlOfChannel = channelId.getMessageTypeUrl();
        final Message originalMessage = AnyPacker.unpack(message.getOriginalMessage());

        // instanceof is needed because the process of getting type URL differs for document messages
        // and other types of messages(e.g. events, rejections).
        if (originalMessage instanceof RequestForExternalMessages) {
            return typeUrlOfChannel.equals(TypeUrl.of(originalMessage)
                                                  .value());
        }

        final Message eventOrRejection = AnyPacker.unpack(
                (Any) originalMessage.getField(
                        originalMessage.getDescriptorForType()
                                       .findFieldByName(MESSAGE_FIELD_NAME)));
        return typeUrlOfChannel.equals(TypeUrl.of(eventOrRejection)
                                              .value());
    }
}
