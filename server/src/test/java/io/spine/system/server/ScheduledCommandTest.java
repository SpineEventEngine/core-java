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

package io.spine.system.server;

import com.google.protobuf.Duration;
import com.google.protobuf.Message;
import com.google.protobuf.util.Durations;
import io.spine.client.CommandFactory;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.CommandContext.Schedule;
import io.spine.core.CommandId;
import io.spine.server.BoundedContext;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.entity.Repository;
import io.spine.system.server.given.CommandLifecycleTestEnv.TestAggregateRepository;
import io.spine.system.server.given.ScheduledCommandTestEnv.TestCommandScheduler;
import io.spine.testing.client.TestActorRequestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static io.spine.base.Identifier.newUuid;
import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.server.SystemBoundedContexts.systemOf;
import static io.spine.validate.Validate.isDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Dmytro Dashenkov
 */
@DisplayName("ScheduledCommand should")
class ScheduledCommandTest {

    private static final CommandFactory commandFactory =
            TestActorRequestFactory.newInstance(ScheduledCommandTest.class)
                                   .command();

    private Repository<CommandId, ScheduledCommand> repository;
    private BoundedContext context;
    private TestCommandScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new TestCommandScheduler();
        CommandBus.Builder commandBus = CommandBus.newBuilder()
                                                  .setThreadSpawnAllowed(false)
                                                  .setCommandScheduler(scheduler);
        context = BoundedContext.newBuilder()
                                .setCommandBus(commandBus)
                                .build();
        context.register(new TestAggregateRepository());
        BoundedContext system = systemOf(this.context);
        Optional<Repository> found = system.findRepository(ScheduledCommandRecord.class);
        assertTrue(found.isPresent());
        @SuppressWarnings("unchecked") // @Internal API. OK for tests.
        Repository<CommandId, ScheduledCommand> repository = found.get();
        this.repository = repository;
    }

    @Test
    @DisplayName("be created when a command is scheduled")
    void created() {
        Command raw = createCommand();
        Schedule schedule = createSchedule(5_000);
        Command scheduled = schedule(raw, schedule);
        post(scheduled);

        Optional<ScheduledCommand> found = repository.find(raw.getId());
        assertTrue(found.isPresent());
        ScheduledCommandRecord scheduledCommand = found.get().getState();

        assertFalse(isDefault(scheduledCommand.getSchedulingTime()));
        Command savedCommand = scheduledCommand.getCommand();
        assertEquals(scheduled, savedCommand);
        scheduler.assertScheduled(savedCommand);
    }

    private static Schedule createSchedule(long delayMillis) {
        Duration delay = Durations.fromMillis(delayMillis);
        Schedule result = Schedule
                .newBuilder()
                .setDelay(delay)
                .build();
        return result;
    }

    private static Command schedule(Command command, Schedule schedule) {
        CommandContext sourceContext = command.getContext();
        CommandContext newContext = sourceContext.toBuilder()
                                                 .setSchedule(schedule)
                                                 .build();
        Command result = command.toBuilder()
                                .setContext(newContext)
                                .build();
        return result;
    }

    private void post(Command command) {
        context.getCommandBus()
               .post(command, noOpObserver());
    }

    private static Command createCommand() {
        Message commandMessage = createCommandMessage();
        Command command = commandFactory.create(commandMessage);
        return command;
    }

    private static Message createCommandMessage() {
        CompanyId id = CompanyId
                .newBuilder()
                .setUuid(newUuid())
                .build();
        String name = ScheduledCommandTest.class.getSimpleName();
        EstablishCompany result = EstablishCompany.newBuilder()
                                                  .setId(id)
                                                  .setFinalName(name)
                                                  .build();
        return result;
    }
}
