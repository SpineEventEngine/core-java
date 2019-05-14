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

import io.spine.base.CommandMessage;
import io.spine.core.CommandContext;
import io.spine.protobuf.MessageFieldException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Obtains an ID of a command target entity from the first field of the command message.
 *
 * @param <I> the type of target entity IDs
 */
public final class DefaultCommandRoute<I> implements CommandRoute<I, CommandMessage> {

    private static final long serialVersionUID = 0L;

    private final FirstField<I, CommandMessage, CommandContext> field;

    private DefaultCommandRoute(Class<I> cls) {
        this.field = new FirstField<>(cls);
    }

    /**
     * Creates a new instance.
     *
     * @param idClass
     *         the class of identifiers used for the routing
     */
    public static <I> DefaultCommandRoute<I> newInstance(Class<I> idClass) {
        checkNotNull(idClass);
        return new DefaultCommandRoute<>(idClass);
    }

    @Override
    public I apply(CommandMessage message, CommandContext ignored) throws MessageFieldException {
        checkNotNull(message);
        I result = field.apply(message, ignored);
        return result;
    }

    /**
     * Verifies of the passed command message potentially has a field with an entity ID.
     */
    public static boolean exists(CommandMessage commandMessage) {
        boolean hasAtLeastOneField =
                !commandMessage.getDescriptorForType()
                               .getFields()
                               .isEmpty();
        return hasAtLeastOneField;
    }
}
