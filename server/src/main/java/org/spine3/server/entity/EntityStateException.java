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

package org.spine3.server.entity;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Any;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Message;
import com.google.protobuf.Value;
import org.spine3.base.Error;
import org.spine3.protobuf.AnyPacker;
import org.spine3.type.TypeName;

import java.util.Map;

/**
 * A base for exceptions related to an entity state.
 *
 * @author Dmytro Grankin
 */
abstract class EntityStateException extends RuntimeException {

    /**
     * The name of the attribute of the entity state type reported in an error.
     *
     * @see #entityStateTypeAttribute(Message)
     * @see Error
     */
    private static final String ATTR_ENTITY_STATE_TYPE_NAME = "entityStateType";

    private static final long serialVersionUID = 0L;

    private final GeneratedMessageV3 entityState;

    /**
     * The error passed with the exception.
     */
    private final Error error;

    /**
     * Creates a new instance.
     *
     * @param messageText the error message text
     * @param entityState the entity state
     * @param error       the occurred error
     */
    EntityStateException(String messageText, Message entityState, Error error) {
        super(messageText);
        this.entityState = entityState instanceof GeneratedMessageV3
                           ? (GeneratedMessageV3) entityState
                           : AnyPacker.pack(entityState);
        this.error = error;
    }

    /**
     * Returns a related event message.
     */
    public Message getEntityState() {
        if (entityState instanceof Any) {
            final Any any = (Any) entityState;
            Message unpacked = AnyPacker.unpack(any);
            return unpacked;
        }
        return entityState;
    }

    /**
     * Returns an error occurred.
     */
    public Error getError() {
        return error;
    }

    /**
     * Returns a map with an entity state type attribute.
     *
     * @param entityState the entity state to get the type from
     */
    static Map<String, Value> entityStateTypeAttribute(Message entityState) {
        final String entityStateType = TypeName.of(entityState)
                                               .value();
        final Value value = Value.newBuilder()
                                 .setStringValue(entityStateType)
                                 .build();
        return ImmutableMap.of(ATTR_ENTITY_STATE_TYPE_NAME, value);
    }
}
