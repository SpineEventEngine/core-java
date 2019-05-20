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

package io.spine.server.commandbus;

import com.google.protobuf.Any;
import io.spine.base.Time;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.CommandId;
import io.spine.protobuf.AnyPacker;
import io.spine.server.type.CommandEnvelope;
import io.spine.test.command.CmdEmpty;
import io.spine.test.commandbus.command.CmdBusCreateProject;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.validate.ConstraintViolation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.server.commandbus.CommandValidator.inspect;
import static io.spine.server.commandbus.Given.CommandMessage.createProjectMessage;
import static io.spine.testing.core.given.GivenCommandContext.withRandomActor;
import static java.util.stream.Collectors.toList;

@DisplayName("CommandValidator violation check should")
class CommandValidatorViolationCheckTest {

    @Test
    @DisplayName("validate command and return empty violations list if command is valid")
    void returnNothingForValidCmd() {
        Command cmd = Given.ACommand.createProject();

        List<ConstraintViolation> violations = inspectCommand(cmd);

        assertThat(violations)
                .isEmpty();
    }

    @Test
    @DisplayName("not allow commands without IDs")
    void notAllowDefaultId() {
        Command cmd = Given.ACommand.createProject();
        Command unidentifiableCommand = cmd
                .toBuilder()
                .setId(CommandId.getDefaultInstance())
                .build();
        List<ConstraintViolation> violations = inspectCommand(unidentifiableCommand);

        assertThat(violations)
                .hasSize(1);
    }

    @Test
    @DisplayName("return violations if command has invalid Message")
    void notAllowInvalidMessage() {
        Any invalidMessagePacked = AnyPacker.pack(CmdBusCreateProject.getDefaultInstance());
        Command commandWithEmptyMessage = Command
                .newBuilder()
                .setId(CommandId.generate())
                .setMessage(invalidMessagePacked)
                .setContext(withRandomActor())
                .build();

        List<ConstraintViolation> violations = inspectCommand(commandWithEmptyMessage);

        assertThat(violations)
                .hasSize(3);
    }

    @Test
    @DisplayName("return violations if command has invalid context")
    void notAllowInvalidContext() {
        TestActorRequestFactory factory = new TestActorRequestFactory(getClass());
        Command command = factory.createCommand(createProjectMessage(), Time.currentTime());
        Command commandWithoutContext = command
                .toBuilder()
                .setContext(CommandContext.getDefaultInstance())
                .build();

        List<ConstraintViolation> violations = inspectCommand(commandWithoutContext);

        assertThat(violations)
                .hasSize(1);
    }

    @Test
    @DisplayName("return violation for an empty command message")
    void emptyMessage() {
        TestActorRequestFactory factory = new TestActorRequestFactory(getClass());
        Command emptyCommand = factory.createCommand(CmdEmpty.newBuilder().build());

        List<ConstraintViolation> violations = inspectCommand(emptyCommand);

        boolean hasViolationOnCommandTargetId =
                !violations.stream()
                           .filter(v -> v.getMsgFormat()
                                          .contains("command target ID"))
                           .collect(toList())
                           .isEmpty();

        assertThat(hasViolationOnCommandTargetId)
                .isTrue();
    }

    private static List<ConstraintViolation> inspectCommand(Command command) {
        return inspect(CommandEnvelope.of(command));
    }
}
