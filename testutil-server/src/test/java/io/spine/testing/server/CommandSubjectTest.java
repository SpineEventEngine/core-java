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

package io.spine.testing.server;

import com.google.common.truth.Subject;
import io.spine.base.CommandMessage;
import io.spine.client.CommandFactory;
import io.spine.core.Command;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.server.given.entity.TuTaskId;
import io.spine.testing.server.given.entity.command.TuAddComment;
import io.spine.testing.server.given.entity.command.TuCreateTask;
import org.junit.jupiter.api.DisplayName;

import static io.spine.base.Identifier.newUuid;
import static io.spine.testing.server.CommandSubject.commands;

@DisplayName("CommandSubject should")
class CommandSubjectTest
        extends EmittedMessageSubjectTest<CommandSubject, Command, CommandMessage> {

    private static final CommandFactory commands =
            new TestActorRequestFactory(CommandSubjectTest.class).command();

    @Override
    protected Subject.Factory<CommandSubject, Iterable<Command>> subjectFactory() {
        return commands();
    }

    @Override
    CommandSubject assertWithSubjectThat(Iterable<Command> messages) {
        return CommandSubject.assertThat(messages);
    }

    @Override
    Command createMessage() {
        return newCommand(
                TuAddComment
                        .vBuilder()
                        .setId(generateTaskId())
                        .build()
        );
    }

    @Override
    Command createAnotherMessage() {
        return newCommand(
                TuCreateTask
                        .vBuilder()
                        .setId(generateTaskId())
                        .build()
        );
    }

    private static Command newCommand(CommandMessage msg) {
        return commands.create(msg);
    }

    private static TuTaskId generateTaskId() {
        return TuTaskId
                .vBuilder()
                .setValue(newUuid())
                .build();
    }
}
