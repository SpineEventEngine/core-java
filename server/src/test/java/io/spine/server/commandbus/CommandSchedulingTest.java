/*
 * Copyright 2023, TeamDev. All rights reserved.
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

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import io.spine.base.CommandMessage;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.test.commandbus.ProjectId;
import io.spine.test.commandbus.command.CmdBusStartProject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.base.Identifier.newUuid;
import static io.spine.base.Time.currentTime;
import static io.spine.protobuf.Durations2.minutes;
import static io.spine.protobuf.TypeConverter.toMessage;
import static io.spine.server.commandbus.CommandScheduler.setSchedule;
import static io.spine.server.commandbus.Given.ACommand.createProject;
import static io.spine.time.testing.TimeTests.Past.minutesAgo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("DuplicateStringLiteralInspection") // Common test display names.
@DisplayName("Command scheduling mechanism should")
class CommandSchedulingTest extends AbstractCommandBusTestSuite {

    CommandSchedulingTest() {
        super(true);
    }

    @Test
    @DisplayName("store scheduled command to CommandStore and return `OK`")
    void storeScheduledCommand() {
        commandBus.register(createProjectHandler);
        Command cmd = createProject(/*delay=*/minutes(1));

        commandBus.post(cmd, observer);

        checkResult(cmd);
    }

    @Test
    @DisplayName("schedule command if delay is set")
    void scheduleIfDelayIsSet() {
        commandBus.register(createProjectHandler);
        Command cmd = createProject(/*delay=*/minutes(1));

        commandBus.post(cmd, observer);
    }

    @Test
    @DisplayName("not schedule command if no scheduling options are set")
    void notScheduleWithoutOptions() {
        CreateProjectHandler handler = new CreateProjectHandler();
        handler.registerWith(BoundedContextBuilder.assumingTests().build());
        commandBus.register(handler);

        Command command = createProject();
        commandBus.post(command, observer);

        checkResult(command);
    }

    @Test
    @DisplayName("post previously scheduled command")
    void postPreviouslyScheduled() {
        commandBus.register(createProjectHandler);
        Command command = createScheduledCommand();

        commandBus.postPreviouslyScheduled(command);

        assertThat(watcher.dispatched())
                .containsExactly(command);
    }

    @Test
    @DisplayName("reject previously scheduled command if no endpoint is found")
    void rejectPreviouslyScheduledWithoutEndpoint() {
        Command command = createScheduledCommand();
        assertThrows(IllegalStateException.class,
                     () -> commandBus.postPreviouslyScheduled(command));
    }

    @Nested
    @DisplayName("allow updating")
    class Update {

        @Test
        @DisplayName("scheduling options")
        void schedulingOptions() {
            Command cmd = createCommand();
            Timestamp schedulingTime = currentTime();
            Duration delay = minutes(5);

            Command cmdUpdated = setSchedule(cmd, delay, schedulingTime);
            CommandContext.Schedule schedule = cmdUpdated.context()
                                                         .getSchedule();

            assertEquals(delay, schedule.getDelay());
            assertEquals(schedulingTime, cmdUpdated.getSystemProperties()
                                                   .getSchedulingTime());
        }

        @Test
        @DisplayName("scheduling time")
        void schedulingTime() {
            Command cmd = createCommand();
            Timestamp schedulingTime = currentTime();

            Command cmdUpdated = CommandScheduler.setSchedulingTime(cmd, schedulingTime);

            assertEquals(schedulingTime, cmdUpdated.getSystemProperties()
                                                   .getSchedulingTime());
        }

        private Command createCommand() {
            ProjectId id = ProjectId
                    .newBuilder()
                    .setId(newUuid())
                    .build();
            CmdBusStartProject command = CmdBusStartProject
                    .newBuilder()
                    .setProjectId(id)
                    .build();
            CommandMessage commandMessage = toMessage(command, CommandMessage.class);
            Command cmd = requestFactory.command()
                                        .create(commandMessage);
            return cmd;
        }
    }

    private static Command createScheduledCommand() {
        Timestamp schedulingTime = minutesAgo(3);
        Duration delayPrimary = minutes(5);
        return setSchedule(createProject(), delayPrimary, schedulingTime);
    }
}
