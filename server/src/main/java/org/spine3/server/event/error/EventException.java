/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.event.error;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Message;
import com.google.protobuf.Value;
import org.spine3.base.Error;
import org.spine3.protobuf.TypeUrl;

import java.io.Serializable;
import java.util.Map;

/**
 * A base for exceptions related to events.
 *
 * @author Alexander Litus
 */
public abstract class EventException extends RuntimeException {

    /** Use {@link GeneratedMessage} because it is {@link Serializable}. */
    private final GeneratedMessage eventMessage;

    private final Error error;

    /**
     * Creates a new instance.
     *
     * @param messageText  an error message text
     * @param eventMessage a related event
     * @param error        an error occurred
     */
    protected EventException(String messageText, Message eventMessage, Error error) {
        super(messageText);
        this.eventMessage = (GeneratedMessage) eventMessage;
        this.error = error;
    }

    /**
     * Returns a map with an event type attribute.
     *
     * @param eventMessage an event message to get the type from
     */
    public static Map<String, Value> eventTypeAttribute(Message eventMessage) {
        final String type = TypeUrl.of(eventMessage).getTypeName();
        final Value value = Value.newBuilder()
                                 .setStringValue(type)
                                 .build();
        return ImmutableMap.of(Attribute.EVENT_TYPE_NAME, value);
    }

    /** Returns a related event. */
    public Message getEventMessage() {
        return eventMessage;
    }

    /** Returns an error occurred. */
    public Error getError() {
        return error;
    }

    /** Attribute names for event-related business failures. */
    public interface Attribute {
        String EVENT_TYPE_NAME = "eventType";
    }

    private static final long serialVersionUID = 0L;
}
