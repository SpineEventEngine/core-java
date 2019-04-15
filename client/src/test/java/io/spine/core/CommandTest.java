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

package io.spine.core;

import io.spine.base.CommandMessage;
import io.spine.base.Identifier;
import io.spine.test.commands.CmdCreateProject;
import io.spine.test.commands.CmdStartProject;
import io.spine.test.commands.CmdStopProject;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.client.command.TestCommandMessage;
import io.spine.testing.core.given.GivenCommandContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.protobuf.Durations2.seconds;
import static io.spine.time.testing.TimeTests.Past.minutesAgo;
import static io.spine.time.testing.TimeTests.Past.secondsAgo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for the {@link Command} class, which implements {@link CommandMixin}.
 *
 * <p>The test suite is located under the "client" module since actor request generation
 * is required. So we want to avoid circular dependencies between "core" and "client" modules.
 */
@DisplayName("Command  should")
class CommandTest {

    private static final CmdCreateProject createProject = CmdCreateProject
            .newBuilder()
            .setId(Identifier.newUuid())
            .build();
    private static final CmdStartProject startProject = CmdStartProject
            .newBuilder()
            .setId(Identifier.newUuid())
            .build();
    private static final CmdStopProject stopProject = CmdStopProject
            .newBuilder()
            .setId(Identifier.newUuid())
            .build();

    private final TestActorRequestFactory requestFactory =
            new TestActorRequestFactory(CommandTest.class);

    @Test
    @DisplayName("extract message from given command")
    void extractMessage() {
        CommandMessage message = TestCommandMessage
                .newBuilder()
                .setId(Identifier.newUuid())
                .build();
        Command command = requestFactory.createCommand(message);
        assertThat(command.enclosedMessage())
                .isEqualTo(message);
    }

    @Nested
    @DisplayName("Compare creation time if")
    class TimeCheck {

        @Test
        @DisplayName("created after given time")
        void wereAfter() {
            Command command = requestFactory.command()
                                            .create(stopProject);
            assertThat(command.isAfter(secondsAgo(5)))
                 .isTrue();
        }

        @Test
        @DisplayName("created withing time range")
        void wereBetween() {
            Command fiveMinsAgo = requestFactory.createCommand(createProject, minutesAgo(5));
            Command twoMinsAgo = requestFactory.createCommand(startProject, minutesAgo(2));
            Command thirtySecondsAgo = requestFactory.createCommand(stopProject, secondsAgo(30));
            Command twentySecondsAgo = requestFactory.createCommand(stopProject, secondsAgo(20));
            Command fiveSecondsAgo = requestFactory.createCommand(stopProject, secondsAgo(5));

            long filteredCommands =
                    Stream.of(fiveMinsAgo,
                              twoMinsAgo,
                              thirtySecondsAgo,
                              twentySecondsAgo,
                              fiveSecondsAgo)
                          .filter(c -> c.isBetween(minutesAgo(3), secondsAgo(10)))
                          .count();
            assertEquals(3, filteredCommands);
        }
    }

    @Test
    @DisplayName("consider command scheduled when command delay is set")
    void recognizeScheduled() {
        CommandContext context = GivenCommandContext.withScheduledDelayOf(seconds(10));
        Command cmd = requestFactory.command()
                                    .createBasedOnContext(createProject, context);
        assertThat(cmd.isScheduled())
                .isTrue();
    }

    @Test
    @DisplayName("consider command not scheduled when no scheduling options are present")
    void recognizeNotScheduled() {
        Command cmd = requestFactory.createCommand(createProject);
        assertThat(cmd.isScheduled())
                .isFalse();
    }

    @Test
    @DisplayName("throw exception when command delay set to negative")
    void throwOnNegativeDelay() {
        CommandContext context = GivenCommandContext.withScheduledDelayOf(seconds(-10));
        Command cmd =
                requestFactory.command()
                              .createBasedOnContext(createProject, context);
        assertThrows(IllegalStateException.class, cmd::isScheduled);
    }
}
