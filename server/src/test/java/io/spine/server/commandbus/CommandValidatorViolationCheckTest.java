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

package io.spine.server.commandbus;

import com.google.protobuf.Any;
import io.spine.base.Time;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.CommandEnvelope;
import io.spine.core.CommandId;
import io.spine.protobuf.AnyPacker;
import io.spine.test.command.CmdCreateProject;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.validate.ConstraintViolation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.spine.core.Commands.generateId;
import static io.spine.server.commandbus.CommandValidator.inspect;
import static io.spine.server.commandbus.Given.CommandMessage.createProjectMessage;
import static io.spine.testing.core.given.GivenCommandContext.withRandomActor;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Alexander Litus
 */
@DisplayName("CommandValidator violation check should")
class CommandValidatorViolationCheckTest {

    @Test
    @DisplayName("validate command and return empty violations list if command is valid")
    void returnNothingForValidCmd() {
        Command cmd = Given.ACommand.createProject();

        List<ConstraintViolation> violations = inspect(CommandEnvelope.of(cmd));

        assertEquals(0, violations.size());
    }

    @Test
    @DisplayName("not allow commands without IDs")
    void notAllowDefaultId() {
        Command cmd = Given.ACommand.createProject();
        Command unidentifiableCommand = cmd.toBuilder()
                                           .setId(CommandId.getDefaultInstance())
                                           .build();
        List<ConstraintViolation> violations =
                inspect(CommandEnvelope.of(unidentifiableCommand));

        assertEquals(1, violations.size());
    }

    @Test
    @DisplayName("return violations if command has invalid Message")
    void notAllowInvalidMessage() {
        Any invalidMessagePacked = AnyPacker.pack(CmdCreateProject.getDefaultInstance());
        Command commandWithEmptyMessage = Command.newBuilder()
                                                 .setId(generateId())
                                                 .setMessage(invalidMessagePacked)
                                                 .setContext(withRandomActor())
                                                 .build();

        List<ConstraintViolation> violations =
                inspect(CommandEnvelope.of(commandWithEmptyMessage));

        assertEquals(3, violations.size());
    }

    @Test
    @DisplayName("return violations if command has invalid context")
    void notAllowInvalidContext() {
        Command command = TestActorRequestFactory.newInstance(getClass())
                                                 .createCommand(createProjectMessage(),
                                                                Time.getCurrentTime());
        Command commandWithoutContext =
                command.toBuilder()
                       .setContext(CommandContext.getDefaultInstance())
                       .build();

        List<ConstraintViolation> violations =
                inspect(CommandEnvelope.of(commandWithoutContext));

        assertEquals(1, violations.size());
    }
}
