/*
 * Copyright 2021, TeamDev. All rights reserved.
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

package io.spine.core;

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import io.spine.base.CommandMessage;
import io.spine.base.Identifier;
import io.spine.test.commands.CmdCreateProject;
import io.spine.test.commands.CmdStartProject;
import io.spine.test.commands.CmdStopProject;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.client.command.TestCommandMessage;
import io.spine.testing.core.given.GivenCommandContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.protobuf.Durations2.seconds;
import static io.spine.time.testing.Future.secondsFromNow;
import static io.spine.time.testing.Past.minutesAgo;
import static io.spine.time.testing.Past.secondsAgo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for the {@link Command} class, which implements {@link CommandMixin}.
 *
 * <p>The test suite is located under the "client" module since actor request generation
 * is required. So we want to avoid circular dependencies between "core" and "client" modules.
 */
@SuppressWarnings("MethodOnlyUsedFromInnerClass" /* to simplify the structure, allow for re-use */)
@DisplayName("`Command` should")
class CommandTest {

    private final TestActorRequestFactory requestFactory =
            new TestActorRequestFactory(CommandTest.class);

    private CmdCreateProject createProject;
    private CmdStartProject startProject;
    private CmdStopProject stopProject;

    @BeforeEach
    void createCommands() {
        var projectId = Identifier.newUuid();
        createProject = CmdCreateProject.newBuilder()
                .setId(projectId)
                .build();
        startProject = CmdStartProject.newBuilder()
                .setId(projectId)
                .build();
        stopProject = CmdStopProject.newBuilder()
                .setId(projectId)
                .build();
    }

    @Test
    @DisplayName("extract message from given command")
    void extractMessage() {
        CommandMessage message = TestCommandMessage.newBuilder()
                .setId(Identifier.newUuid())
                .build();
        var command = command(message);
        assertThat(command.enclosedMessage())
                .isEqualTo(message);
    }

    @Nested
    @DisplayName("Compare creation time if")
    class TimeCheck {

        @Test
        @DisplayName("after given time")
        void after() {
            var command = command(stopProject);
            assertThat(command.isAfter(secondsAgo(5)))
                 .isTrue();
        }

        @Test
        @DisplayName("before given time")
        void before() {
            var command = command(startProject);
            assertThat(command.isBefore(secondsFromNow(10)))
                    .isTrue();
        }

        @Test
        @DisplayName("withing time range")
        void between() {
            var fiveMinsAgo = command(createProject, minutesAgo(5));
            var twoMinsAgo = command(startProject, minutesAgo(2));
            var thirtySecondsAgo = command(stopProject, secondsAgo(30));
            var twentySecondsAgo = command(stopProject, secondsAgo(20));
            var fiveSecondsAgo = command(stopProject, secondsAgo(5));

            var filteredCommands =
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
        var cmd = commandWithDelay(createProject, seconds(10));
        assertThat(cmd.isScheduled())
                .isTrue();
    }

    @Test
    @DisplayName("consider command not scheduled when no scheduling options are present")
    void recognizeNotScheduled() {
        var cmd = command(createProject);
        assertThat(cmd.isScheduled())
                .isFalse();
    }

    @Test
    @DisplayName("throw exception when command delay set to negative")
    void throwOnNegativeDelay() {
        var cmd = commandWithDelay(createProject, seconds(-10));
        assertThrows(IllegalStateException.class, cmd::isScheduled);
    }

    private Command command(CommandMessage msg, Timestamp timestamp) {
        return requestFactory.createCommand(msg, timestamp);
    }

    private Command command(CommandMessage msg) {
        return requestFactory.command().create(msg);
    }

    private Command commandWithDelay(CommandMessage msg, Duration delay) {
        var context = GivenCommandContext.withScheduledDelayOf(delay);
        var result = requestFactory.command()
                                   .createBasedOnContext(msg, context);
        return result;
    }
}
