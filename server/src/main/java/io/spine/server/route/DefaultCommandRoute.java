/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server.route;

import com.google.protobuf.Message;
import io.spine.base.CommandMessage;
import io.spine.core.CommandContext;
import io.spine.protobuf.MessageFieldException;

import java.util.Optional;

/**
 * Obtains an ID of a command target entity from the first field of the command message.
 *
 * @param <I> the type of target entity IDs
 */
public class DefaultCommandRoute<I> extends FieldAtIndex<I, CommandMessage, CommandContext>
        implements CommandRoute<I, CommandMessage> {

    private static final long serialVersionUID = 0L;
    private static final int ID_FIELD_INDEX = 0;

    private DefaultCommandRoute() {
        super(ID_FIELD_INDEX);
    }

    /** Creates a new instance. */
    public static <I> DefaultCommandRoute<I> newInstance() {
        return new DefaultCommandRoute<>();
    }

    /**
     * Tries to obtain a target ID from the passed command message.
     *
     * @param commandMessage the message to get ID from
     * @return an {@link Optional} of the ID or {@code Optional.empty()}
     * if {@link DefaultCommandRoute#apply(Message, Message)} throws an exception
     * if the command is not for an entity
     */
    public static <I> Optional<I> asOptional(CommandMessage commandMessage) {
        try {
            DefaultCommandRoute<I> function = newInstance();
            I id = function.apply(commandMessage, CommandContext.getDefaultInstance());
            return Optional.of(id);
        } catch (MessageFieldException | ClassCastException ignored) {
            return Optional.empty();
        }
    }
}
