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

package org.spine3.server.command;

import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandId;
import org.spine3.base.CommandStatus;
import org.spine3.test.command.CreateProject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.spine3.protobuf.AnyPacker.unpack;
import static org.spine3.validate.Validate.isDefault;
import static org.spine3.validate.Validate.isNotDefault;

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
        final CommandContext context = cmd.getContext();
        final CommandId commandId = context.getCommandId();
        final CreateProject message = unpack(cmd.getMessage());
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
                                           .getFailure()));
                break;
            case ERROR:
                assertTrue(isNotDefault(record.getStatus()
                                              .getError()));
                break;
            case FAILURE:
                assertTrue(isNotDefault(record.getStatus()
                                              .getFailure()));
                break;
            case UNDEFINED:
            case UNRECOGNIZED:
                break;
        }
    }
}
