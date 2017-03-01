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

import org.junit.Test;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.Commands;
import org.spine3.test.command.CreateProject;
import org.spine3.validate.ConstraintViolation;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.spine3.testdata.TestCommandContextFactory.createCommandContext;

/**
 * @author Alexander Litus
 */
public class CommandValidatorShould {

    private final CommandValidator validator = CommandValidator.getInstance();

    @Test
    public void validate_command_and_return_nothing_if_it_is_valid() {
        final Command cmd = Given.Command.createProject();

        final List<ConstraintViolation> violations = validator.validate(cmd);

        assertEquals(0, violations.size());
    }

    @Test
    public void validate_command_and_return_violations_if_message_is_NOT_valid() {
        final Command cmd = Commands.createCommand(CreateProject.getDefaultInstance(), createCommandContext());

        final List<ConstraintViolation> violations = validator.validate(cmd);

        assertEquals(3, violations.size());
    }

    @Test
    public void validate_command_and_return_violations_if_context_is_NOT_valid() {
        final Command cmd = Commands.createCommand(Given.CommandMessage.createProjectMessage(), CommandContext.getDefaultInstance());

        final List<ConstraintViolation> violations = validator.validate(cmd);

        assertEquals(2, violations.size());
    }

    @Test
    public void check_command_and_do_not_throw_exception_if_it_is_valid() {
        final Command cmd = Given.Command.createProject();

        CommandValidator.checkCommand(cmd);
    }

    @Test(expected = IllegalArgumentException.class)
    public void check_command_and_throw_exception_if_message_is_NOT_valid() {
        final Command cmd = Commands.createCommand(CreateProject.getDefaultInstance(), createCommandContext());

        CommandValidator.checkCommand(cmd);
    }

    @Test(expected = IllegalArgumentException.class)
    public void check_command_and_throw_exception_if_context_is_NOT_valid() {
        final Command cmd = Commands.createCommand(Given.CommandMessage.createProjectMessage(), CommandContext.getDefaultInstance());

        CommandValidator.checkCommand(cmd);
    }
}
