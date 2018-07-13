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

package io.spine.server.event;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Any;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Message;
import com.google.protobuf.Value;
import io.spine.base.Error;
import io.spine.protobuf.AnyPacker;
import io.spine.core.MessageRejection;
import io.spine.type.TypeName;

import java.util.Map;

/**
 * A base for exceptions related to events.
 *
 * @author Alexander Litus
 */
public abstract class EventException extends RuntimeException implements MessageRejection {

    private static final long serialVersionUID = 0L;

    public static final String ATTR_EVENT_TYPE_NAME = "eventType";

    /**
     * The event message or the message packed into {@link Any}.
     *
     * <p>We use {@link GeneratedMessageV3} (not {@code Message}) because
     * it is {@link java.io.Serializable Serializable}.
     */
    private final GeneratedMessageV3 eventMessage;

    /**
     * The error passed with the exception.
     */
    private final Error error;

    /**
     * Creates a new instance.
     *
     * @param messageText  an error message text
     * @param eventMessage a related event message
     * @param error        an error occurred
     */
    protected EventException(String messageText, Message eventMessage, Error error) {
        super(messageText);
        if (eventMessage instanceof GeneratedMessageV3) {
            this.eventMessage = (GeneratedMessageV3) eventMessage;
        } else {
            // In an unlikely case on encountering a message, which is not `GeneratedMessageV3`,
            // wrap it into `Any`.
            this.eventMessage = AnyPacker.pack(eventMessage);
        }
        this.error = error;
    }

    /**
     * Returns a map with an event type attribute.
     *
     * @param eventMessage an event message to get the type from
     */
    public static Map<String, Value> eventTypeAttribute(Message eventMessage) {
        String type = TypeName.of(eventMessage)
                                    .value();
        Value value = Value.newBuilder()
                                 .setStringValue(type)
                                 .build();
        Map<String, Value> result = ImmutableMap.of(ATTR_EVENT_TYPE_NAME, value);
        return result;
    }

    /**
     * Returns a related event message.
     */
    public Message getEventMessage() {
        if (eventMessage instanceof Any) {
            Any any = (Any) eventMessage;
            Message unpacked = AnyPacker.unpack(any);
            return unpacked;
        }
        return eventMessage;
    }

    @Override
    public Error asError() {
        return error;
    }

    @Override
    public Throwable asThrowable() {
        return this;
    }
}
