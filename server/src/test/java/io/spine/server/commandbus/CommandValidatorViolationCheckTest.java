/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import com.google.common.truth.Correspondence;
import com.google.common.truth.IterableSubject;
import com.google.protobuf.Any;
import io.spine.base.Time;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.CommandId;
import io.spine.protobuf.AnyPacker;
import io.spine.server.type.CommandEnvelope;
import io.spine.test.command.CmdEmpty;
import io.spine.test.commandbus.command.CmdBusCreateLabels;
import io.spine.test.commandbus.command.CmdBusCreateProject;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.validate.ConstraintViolation;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.server.commandbus.CommandValidator.inspect;
import static io.spine.server.commandbus.Given.CommandMessage.createProjectMessage;
import static io.spine.testing.core.given.GivenCommandContext.withRandomActor;

@DisplayName("CommandValidator violation check should")
class CommandValidatorViolationCheckTest {

    private static final Correspondence<@NonNull ConstraintViolation, @NonNull String>
            messageFormatContains = Correspondence.from(
                    (actual, expected) -> actual.getMsgFormat().contains(expected),
                    "has message format"
            );

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

        IterableSubject assertViolations = assertThat(violations);
        assertViolations
                .hasSize(2);
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
    @DisplayName("allow empty command messages")
    void emptyMessage() {
        TestActorRequestFactory factory = new TestActorRequestFactory(getClass());
        Command emptyCommand = factory.createCommand(CmdEmpty.getDefaultInstance());
        List<ConstraintViolation> violations = inspectCommand(emptyCommand);
        assertThat(violations)
                .comparingElementsUsing(messageFormatContains)
                .doesNotContain("command target ID");
    }

    @Test
    @DisplayName("allow messages without an ID as a first field")
    void noId() {
        TestActorRequestFactory factory = new TestActorRequestFactory(getClass());
        CmdBusCreateLabels msg = CmdBusCreateLabels
                .newBuilder()
                .addLabel("red")
                .addLabel("green")
                .addLabel("blue")
                .vBuild();
        Command command = factory.createCommand(msg);
        List<ConstraintViolation> violations = inspectCommand(command);
        assertThat(violations)
                .isEmpty();
    }

    private static List<ConstraintViolation> inspectCommand(Command command) {
        return inspect(CommandEnvelope.of(command));
    }
}
