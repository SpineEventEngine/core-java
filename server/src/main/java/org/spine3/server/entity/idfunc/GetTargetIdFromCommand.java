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

package org.spine3.server.entity.idfunc;

import com.google.common.base.Optional;
import com.google.protobuf.Message;
import org.spine3.Internal;
import org.spine3.base.CommandContext;
import org.spine3.server.error.MissingEntityIdException;

/**
 * Obtains a command target entity ID based on a command message and context.
 *
 * <p>The command target must be the first field defined in the command message.
 *
 * @param <I> the type of target entity IDs
 * @param <M> the type of command messages to get IDs from
 * @author Alexander Litus
 */
@Internal
public class GetTargetIdFromCommand<I, M extends Message> extends GetIdByFieldIndex<I, M, CommandContext> {

    private static final int ID_FIELD_INDEX = 0;

    private GetTargetIdFromCommand() {
        super(ID_FIELD_INDEX);
    }

    /** Creates a new ID function instance. */
    public static <I, M extends Message> GetTargetIdFromCommand<I, M> newInstance() {
        return new GetTargetIdFromCommand<>();
    }

    /**
     * Tries to obtain a target ID from the passed command message.
     *
     * @param commandMessage a message to get ID from
     * @return an {@link Optional} of the ID or {@code Optional.absent()}
     * if {@link GetTargetIdFromCommand#apply(Message, Message)} throws an exception
     * (in the case if the command is not for an entity)
     */
    public static <I> Optional<I> asOptional(Message commandMessage) {
        try {
            final GetTargetIdFromCommand<I, Message> function = newInstance();
            final I id = function.apply(commandMessage, CommandContext.getDefaultInstance());
            return Optional.of(id);
        } catch (MissingEntityIdException | ClassCastException ignored) {
            return Optional.absent();
        }
    }
}
