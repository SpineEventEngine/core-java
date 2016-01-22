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

package org.spine3.server.util;

import com.google.common.base.Function;
import org.spine3.base.CommandId;

import javax.annotation.Nullable;

/**
 * Utilities for working with {@code CommandId}s.
 */
public class CommandIdentifiers {

    static {
        Identifiers.IdConverterRegistry.getInstance().register(CommandId.class, new CommandIdToStringConverter());
    }

    /**
     * Creates string representation of the passed command ID.
     *
     * @param commandId the ID to convert
     * @return string value, with the format defined by {@link Identifiers#idToString(Object)}
     * @see Identifiers#idToString(Object)
     */
    public static String idToString(CommandId commandId) {
        final String result = Identifiers.idToString(commandId);
        return result;
    }

    @SuppressWarnings("StringBufferWithoutInitialCapacity")
    public static class CommandIdToStringConverter implements Function<CommandId, String> {
        @Override
        public String apply(@Nullable CommandId commandId) {
            if (commandId == null) {
                return Identifiers.NULL_ID_OR_FIELD;
            }

            return commandId.getUuid();
        }
    }

    private CommandIdentifiers() {}
}
