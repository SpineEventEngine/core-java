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

package org.spine3.server.entity;

import com.google.protobuf.Message;
import org.spine3.Internal;
import org.spine3.base.CommandContext;
import org.spine3.base.Identifiers;
import org.spine3.server.error.MissingEntityIdException;

import javax.annotation.Nullable;

/**
 * Obtains a command target {@link Entity} ID based on a command {@link Message} and context.
 *
 * <p>An entity ID must be the first field in command messages (in Protobuf definition).
 * Its name must end with the {@link Identifiers#ID_PROPERTY_SUFFIX}.
 *
 * @param <I> the type of target entity IDs
 * @param <M> the type of command messages to get IDs from
 * @author Alexander Litus
 */
@Internal
public class GetTargetIdFromCommand<I, M extends Message> extends GetIdByFieldIndex<I, M, CommandContext> {

    public static final int ID_FIELD_INDEX = 0;

    private static final GetTargetIdFromCommand<Object, Message> ID_FUNCTION = newInstance();

    private GetTargetIdFromCommand() {
        super(ID_FIELD_INDEX);
    }

    /**
     * Creates a new ID function instance.
     */
    public static<I, M extends Message> GetTargetIdFromCommand<I, M> newInstance() {
        return new GetTargetIdFromCommand<>();
    }

    /**
     * Tries to obtain a target ID from the passed command message.
     *
     * @return an ID or {@code null} if {@link GetTargetIdFromCommand#getId(Message, Message)}
     * throws an exception (in the case if the command is not for an entity)
     */
    @Nullable
    public static Object asNullableObject(Message commandMessage) {
        //TODO:2016-05-09:alexander.yevsyukov: return Optional
        try {
            final Object id = ID_FUNCTION.getId(commandMessage, CommandContext.getDefaultInstance());
            return id;
        } catch (MissingEntityIdException | ClassCastException ignored) {
            return null;
        }
    }
}
