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

package io.spine.server.integration.route.matcher;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.server.integration.ChannelId;
import io.spine.server.integration.ExternalMessage;
import io.spine.server.integration.MessageMatched;
import io.spine.server.integration.RequestForExternalMessages;
import io.spine.type.TypeUrl;

import static io.spine.protobuf.AnyPacker.unpack;

/**
 * The {@code MessageTypeMatcher} checks if the message matches the {@code MessageChannel}
 * by the message type.
 *
 * @author Dmitry Ganzha
 */
public class MessageTypeMatcher implements ChannelMatcher {

    private static final String MESSAGE_FIELD_NAME = "message";
    private static final String ERROR_MESSAGE =
            "The message type URL does not match the type URL of messages which " +
                    "the message channel can transport.";

    private static MessageMatched constructResult(boolean isTypeUrlsSame) {
        final MessageMatched.Builder builder = MessageMatched.newBuilder();
        if (isTypeUrlsSame) {
            builder.setMatched(true);
        } else {
            builder.setMatched(false);
            builder.setDescription(ERROR_MESSAGE);
        }
        return builder.build();
    }

    @Override
    public MessageMatched match(ChannelId channelId, ExternalMessage message) {
        final String typeUrlOfChannel = channelId.getMessageTypeUrl();
        final Message originalMessage = unpack(message.getOriginalMessage());

        // instanceof is needed because the process of getting type URL differs for document messages
        // and other types of messages(e.g. events, rejections).
        if (originalMessage instanceof RequestForExternalMessages) {
            final String typeUrlOfMessage = TypeUrl.of(originalMessage)
                                                   .value();
            final boolean isTypeUrlsSame = typeUrlOfChannel.equals(typeUrlOfMessage);
            return constructResult(isTypeUrlsSame);
        }

        final Message eventOrRejection = unpack(
                (Any) originalMessage.getField(originalMessage.getDescriptorForType()
                                                              .findFieldByName(
                                                                      MESSAGE_FIELD_NAME)));
        final String typeUrlOfMessage = TypeUrl.of(eventOrRejection)
                                               .value();
        final boolean isTypeUrlsSame = typeUrlOfChannel.equals(typeUrlOfMessage);
        return constructResult(isTypeUrlsSame);
    }
}
