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

package io.spine.server.commandstore.given;

import com.google.protobuf.Any;
import io.spine.base.Error;
import io.spine.core.Command;
import io.spine.core.CommandEnvelope;
import io.spine.core.Commands;
import io.spine.core.Rejection;
import io.spine.core.RejectionContext;
import io.spine.core.RejectionId;
import io.spine.core.Rejections;
import io.spine.server.commandbus.CommandRecord;
import io.spine.server.commandbus.Given;
import io.spine.server.commandbus.ProcessingStatus;

import static io.spine.base.Time.getCurrentTime;
import static io.spine.core.CommandStatus.RECEIVED;
import static io.spine.protobuf.TypeConverter.toAny;

public class StorageTestEnv {

    /** Prevents instantiation of this utility class. */
    private StorageTestEnv() {
    }

    public static Error newError() {
        return Error.newBuilder()
                    .setType("error type 123")
                    .setCode(5)
                    .setMessage("error message 123")
                    .setStacktrace("stacktrace")
                    .build();
    }

    public static Rejection newRejection() {
        Any packedMessage = toAny("newRejection");
        RejectionId id = Rejections.generateId(Commands.generateId());
        return Rejection.newBuilder()
                        .setId(id)
                        .setMessage(packedMessage)
                        .setContext(RejectionContext.newBuilder()
                                                    .setStacktrace("rejection stacktrace")
                                                    .setTimestamp(getCurrentTime()))
                        .build();
    }

    public static CommandRecord newStorageRecord() {
        Command command = Given.ACommand.createProject();
        String commandType = CommandEnvelope.of(command)
                                            .getTypeName()
                                            .value();

        CommandRecord.Builder builder =
                CommandRecord.newBuilder()
                             .setCommandType(commandType)
                             .setCommandId(command.getId())
                             .setCommand(command)
                             .setTimestamp(getCurrentTime())
                             .setStatus(ProcessingStatus.newBuilder()
                                                        .setCode(RECEIVED));
        return builder.build();
    }
}
