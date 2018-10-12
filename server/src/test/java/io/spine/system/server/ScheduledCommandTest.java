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

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Duration;
import com.google.protobuf.FieldMask;
import com.google.protobuf.util.Durations;
import io.spine.base.CommandMessage;
import io.spine.client.EntityFilters;
import io.spine.client.OrderBy;
import io.spine.client.Pagination;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.CommandContext.Schedule;
import io.spine.core.CommandId;
import io.spine.server.BoundedContext;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.entity.RecordBasedRepository;
import io.spine.server.entity.Repository;
import io.spine.system.server.given.command.CompanyRepository;
import io.spine.system.server.given.schedule.TestCommandScheduler;
import io.spine.testing.client.TestActorRequestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.Optional;

import static io.spine.base.Identifier.newUuid;
import static io.spine.client.ColumnFilters.eq;
import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.server.storage.LifecycleFlagField.deleted;
import static io.spine.system.server.SystemBoundedContexts.systemOf;
import static io.spine.validate.Validate.isDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Dmytro Dashenkov
 */
@DisplayName("ScheduledCommand should")
class ScheduledCommandTest {

    private static final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(ScheduledCommandTest.class);

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

        Optional<ScheduledCommand> command = repository.find(scheduled.getId());
        assertFalse(command.isPresent());

        ScheduledCommand deletedCommand = findDeleted(scheduled.getId());
        assertTrue(deletedCommand.getLifecycleFlags().getDeleted());
        assertFalse(deletedCommand.getLifecycleFlags().getArchived());
    }

    @Test
    @DisplayName("not be created for a non-scheduled command")
    void skipped() {
        Command command = createCommand();
        post(command);

        CommandId commandId = command.getId();

        Optional<ScheduledCommand> found = repository.find(commandId);
        assertFalse(found.isPresent());

        Iterator<ScheduledCommand> foundDeleted = findAllDeleted(commandId);
        assertFalse(foundDeleted.hasNext());
    }

    private ScheduledCommand findDeleted(CommandId id) {
        Iterator<ScheduledCommand> commands = findAllDeleted(id);
        assertTrue(commands.hasNext());
        ScheduledCommand result = commands.next();
        assertFalse(commands.hasNext());
        return result;
    }

    private Iterator<ScheduledCommand> findAllDeleted(CommandId id) {
        EntityFilters filters = requestFactory.query()
                                              .select(ScheduledCommandRecord.class)
                                              .byId(id)
                                              .where(eq(deleted.name(), true))
                                              .build()
                                              .getTarget()
                                              .getFilters();
        Iterator<ScheduledCommand> commands = repository.find(filters, 
                                                              OrderBy.getDefaultInstance(),
                                                              Pagination.getDefaultInstance(),
                                                              FieldMask.getDefaultInstance());
        return commands;
    }

    private void checkScheduled(Command scheduled) {
        Optional<ScheduledCommand> found = repository.find(scheduled.getId());
        assertTrue(found.isPresent());
        ScheduledCommandRecord scheduledCommand = found.get().getState();

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
        context.getCommandBus()
               .post(command, noOpObserver());
    }

    private static Command createCommand() {
        CommandMessage commandMessage = createCommandMessage();
        Command command = requestFactory.command()
                                        .create(commandMessage);
        return command;
    }

    private static CommandMessage createCommandMessage() {
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
