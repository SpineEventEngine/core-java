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

package io.spine.system.server;

import com.google.common.truth.Truth8;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Duration;
import com.google.protobuf.util.Durations;
import io.spine.base.CommandMessage;
import io.spine.base.Identifier;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.CommandContext.Schedule;
import io.spine.core.CommandId;
import io.spine.server.BoundedContext;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.entity.LifecycleFlags;
import io.spine.server.entity.RecordBasedRepository;
import io.spine.server.entity.Repository;
import io.spine.system.server.given.command.CompanyRepository;
import io.spine.system.server.given.schedule.TestCommandScheduler;
import io.spine.testing.client.TestActorRequestFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.system.server.SystemBoundedContexts.systemOf;
import static io.spine.validate.Validate.isDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ScheduledCommand should")
class ScheduledCommandTest {

    private static final TestActorRequestFactory requestFactory =
            new TestActorRequestFactory(ScheduledCommandTest.class);

    private RecordBasedRepository<CommandId, ScheduledCommand, ScheduledCommandRecord> repository;
    private BoundedContext context;
    private TestCommandScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new TestCommandScheduler();
        CommandBus.Builder commandBus = CommandBus.newBuilder()
                                                  .setCommandScheduler(scheduler);
        context = BoundedContext.newBuilder()
                                .setCommandBus(commandBus)
                                .build();
        context.register(new CompanyRepository());
        BoundedContext system = systemOf(this.context);
        Optional<Repository> found = system.findRepository(ScheduledCommandRecord.class);
        assertTrue(found.isPresent());
        @SuppressWarnings("unchecked") // @Internal API. OK for tests.
        RecordBasedRepository<CommandId, ScheduledCommand, ScheduledCommandRecord> repository =
                (RecordBasedRepository<CommandId, ScheduledCommand, ScheduledCommandRecord>)
                        found.get();
        this.repository = repository;
    }

    @AfterEach
    void tearDown() throws Exception {
        context.close();
    }

    @Test
    @DisplayName("be created when a command is scheduled")
    void created() {
        Command scheduled = createAndSchedule();
        checkScheduled(scheduled);
    }

    @Test
    @DisplayName("be deleted when a command is dispatched")
    void deleted() {
        Command scheduled = createAndSchedule();
        checkScheduled(scheduled);

        scheduler.postScheduled();

        Optional<ScheduledCommand> command = repository.findActive(scheduled.getId());
        Truth8.assertThat(command).isEmpty();

        Optional<ScheduledCommand> optional = repository.find(scheduled.getId());
        assertTrue(optional.isPresent());
        ScheduledCommand deletedCommand = optional.get();
        LifecycleFlags flags = deletedCommand.lifecycleFlags();

        assertThat(flags.getDeleted()).isTrue();
        assertThat(flags.getArchived()).isFalse();
    }

    @Test
    @DisplayName("not be created for a non-scheduled command")
    void skipped() {
        Command command = createCommand();
        post(command);

        CommandId commandId = command.getId();

        Optional<ScheduledCommand> found = repository.find(commandId);
        Truth8.assertThat(found)
              .isEmpty();
    }

    private void checkScheduled(Command scheduled) {
        Optional<ScheduledCommand> found = repository.find(scheduled.getId());
        assertTrue(found.isPresent());
        ScheduledCommandRecord scheduledCommand = found.get().state();

        assertFalse(isDefault(scheduledCommand.getSchedulingTime()));
        Command savedCommand = scheduledCommand.getCommand();
        assertEquals(scheduled, savedCommand);
        scheduler.assertScheduled(savedCommand);
    }

    @CanIgnoreReturnValue
    private Command createAndSchedule() {
        Command raw = createCommand();
        Schedule schedule = createSchedule(5_000);
        Command scheduled = schedule(raw, schedule);
        post(scheduled);
        return scheduled;
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
        context.commandBus()
               .post(command, noOpObserver());
    }

    private static Command createCommand() {
        CommandMessage commandMessage = createCommandMessage();
        Command command = requestFactory.command()
                                        .create(commandMessage);
        return command;
    }

    private static CommandMessage createCommandMessage() {
        CompanyId id = Identifier.generate(CompanyId.class);
        String name = ScheduledCommandTest.class.getSimpleName();
        EstablishCompany result = EstablishCompany.newBuilder()
                                                  .setId(id)
                                                  .setFinalName(name)
                                                  .build();
        return result;
    }
}
