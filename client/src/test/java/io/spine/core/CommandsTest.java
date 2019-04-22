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

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Any;
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import io.spine.base.Identifier;
import io.spine.protobuf.Durations2;
import io.spine.string.Stringifiers;
import io.spine.test.commands.CmdCreateProject;
import io.spine.test.commands.CmdStartProject;
import io.spine.test.commands.CmdStopProject;
import io.spine.testing.UtilityClassTest;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.core.given.GivenUserId;
import io.spine.time.ZoneOffset;
import io.spine.time.ZoneOffsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.truth.Truth.assertThat;
import static com.google.protobuf.Descriptors.FileDescriptor;
import static io.spine.base.Time.currentTime;
import static io.spine.time.testing.TimeTests.Past.minutesAgo;
import static io.spine.time.testing.TimeTests.Past.secondsAgo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Tests for {@linkplain Commands Commands utility class}.
 *
 * <p>The test suite is located under the "client" module since actor request generation
 * is required. So we want to avoid circular dependencies between "core" and "client" modules.
 */
@DisplayName("Commands utility should")
class CommandsTest extends UtilityClassTest<Commands> {

    private static final FileDescriptor DEFAULT_FILE_DESCRIPTOR = Any.getDescriptor()
                                                                     .getFile();

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
            new TestActorRequestFactory(CommandsTest.class);

    CommandsTest() {
        super(Commands.class);
    }

    @Override
    protected void configure(NullPointerTester tester) {
        super.configure(tester);
        tester.setDefault(FileDescriptor.class, DEFAULT_FILE_DESCRIPTOR)
              .setDefault(Timestamp.class, currentTime())
              .setDefault(Duration.class, Durations2.ZERO)
              .setDefault(Command.class, requestFactory.createCommand(createProject, minutesAgo(1)))
              .setDefault(CommandContext.class, requestFactory.createCommandContext())
              .setDefault(ZoneOffset.class, ZoneOffsets.utc())
              .setDefault(UserId.class, GivenUserId.newUuid());
    }

    @Test
    @DisplayName("sort given commands by timestamp")
    void sortByTimestamp() {
        Command cmd1 = requestFactory.createCommand(createProject, minutesAgo(1));
        Command cmd2 = requestFactory.createCommand(startProject, secondsAgo(30));
        Command cmd3 = requestFactory.createCommand(stopProject, secondsAgo(5));
        List<Command> sortedCommands = newArrayList(cmd1, cmd2, cmd3);
        List<Command> commandsToSort = newArrayList(cmd3, cmd1, cmd2);
        assertNotEquals(sortedCommands, commandsToSort);

        Commands.sort(commandsToSort);

        assertEquals(sortedCommands, commandsToSort);
    }

    @Test
    @DisplayName("provide stringifier for command id")
    void provideStringifierForId() {
        CommandId id = CommandId.generate();

        String str = Stringifiers.toString(id);
        CommandId convertedBack = Stringifiers.fromString(str, CommandId.class);
        assertThat(convertedBack)
                .isEqualTo(id);
    }
}
