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

package io.spine.server.commandstore;

import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.CommandId;
import io.spine.core.CommandStatus;
import io.spine.server.commandbus.CommandRecord;
import io.spine.test.command.CmdCreateProject;

import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.validate.Validate.isDefault;
import static io.spine.validate.Validate.isNotDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Utilities for testing command management.
 *
 * @author Alexander Yevsyukov
 */
class CommandTestUtil {

    private CommandTestUtil() {
        // Prevent instantiation of this utility class.
    }

    static void checkRecord(CommandRecord record,
                            Command cmd,
                            CommandStatus statusExpected) {
        CommandContext context = cmd.getContext();
        CommandId commandId = cmd.getId();
        CmdCreateProject message = unpack(cmd.getMessage());
        assertEquals(cmd.getMessage(), record.getCommand()
                                             .getMessage());
        assertTrue(record.getTimestamp()
                         .getSeconds() > 0);
        assertEquals(message.getClass()
                            .getSimpleName(), record.getCommandType());
        assertEquals(commandId, record.getCommandId());
        assertEquals(statusExpected, record.getStatus()
                                           .getCode());
        assertEquals(context, record.getCommand()
                                    .getContext());
        switch (statusExpected) {
            case RECEIVED:
            case OK:
            case SCHEDULED:
                assertTrue(isDefault(record.getStatus()
                                           .getError()));
                assertTrue(isDefault(record.getStatus()
                                           .getRejection()));
                break;
            case ERROR:
                assertTrue(isNotDefault(record.getStatus()
                                              .getError()));
                break;
            case REJECTED:
                assertTrue(isNotDefault(record.getStatus()
                                              .getRejection()));
                break;
            case UNDEFINED:
            case UNRECOGNIZED:
                break;
        }
    }
}
