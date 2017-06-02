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

package io.spine.server.commandbus;

import com.google.protobuf.Any;
import io.spine.base.Command;
import io.spine.base.CommandContext;
import io.spine.envelope.CommandEnvelope;
import io.spine.protobuf.AnyPacker;
import io.spine.test.TestActorRequestFactory;
import io.spine.test.command.CreateProject;
import io.spine.time.Time;
import io.spine.validate.ConstraintViolation;
import org.junit.Test;

import java.util.List;

import static io.spine.base.Commands.generateId;
import static io.spine.server.commandbus.Given.CommandMessage.createProjectMessage;
import static io.spine.testdata.TestCommandContextFactory.createCommandContext;
import static org.junit.Assert.assertEquals;

/**
 * @author Alexander Litus
 */
public class ValidatorShould {

    private final Validator validator = Validator.getInstance();

    @Test
    public void validate_command_and_return_nothing_if_it_is_valid() {
        final Command cmd = Given.Command.createProject();

        final List<ConstraintViolation> violations = validator.validate(CommandEnvelope.of(cmd));

        assertEquals(0, violations.size());
    }

    @Test
    public void validate_command_and_return_violations_if_message_is_NOT_valid() {
        final Any invalidMessagePacked = AnyPacker.pack(CreateProject.getDefaultInstance());
        final Command commandWithEmptyMessage = Command.newBuilder()
                                                       .setId(generateId())
                                                       .setMessage(invalidMessagePacked)
                                                       .setContext(createCommandContext())
                                                       .build();

        final List<ConstraintViolation> violations =
                validator.validate(CommandEnvelope.of(commandWithEmptyMessage));

        assertEquals(3, violations.size());
    }

    @Test
    public void validate_command_and_return_violations_if_context_is_NOT_valid() {
        final Command command = TestActorRequestFactory.newInstance(ValidatorShould.class)
                                                       .createCommand(createProjectMessage(),
                                                                      Time.getCurrentTime());
        final Command commandWithoutContext = command.toBuilder()
                                     .setContext(CommandContext.getDefaultInstance())
                                     .build();

        final List<ConstraintViolation> violations =
                validator.validate(CommandEnvelope.of(commandWithoutContext));

        assertEquals(1, violations.size());
    }
}
