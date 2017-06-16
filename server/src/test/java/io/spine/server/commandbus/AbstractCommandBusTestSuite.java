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

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import io.spine.base.ActorContext;
import io.spine.base.Command;
import io.spine.base.CommandContext;
import io.spine.base.CommandValidationError;
import io.spine.base.Error;
import io.spine.client.ActorRequestFactory;
import io.spine.envelope.CommandEnvelope;
import io.spine.io.StreamObservers;
import io.spine.io.StreamObservers.MemoizingObserver;
import io.spine.server.command.Assign;
import io.spine.server.command.CommandHandler;
import io.spine.server.commandstore.CommandStore;
import io.spine.server.event.EventBus;
import io.spine.server.failure.FailureBus;
import io.spine.server.storage.memory.InMemoryStorageFactory;
import io.spine.server.tenant.TenantAwareTest;
import io.spine.server.tenant.TenantIndex;
import io.spine.test.TestActorRequestFactory;
import io.spine.test.command.CreateProject;
import io.spine.test.command.event.ProjectCreated;
import io.spine.users.TenantId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.annotation.Nullable;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static io.spine.base.CommandStatus.SCHEDULED;
import static io.spine.base.CommandValidationError.INVALID_COMMAND;
import static io.spine.server.commandbus.CommandScheduler.setSchedule;
import static io.spine.server.commandbus.Given.Command.createProject;
import static io.spine.test.Values.newTenantUuid;
import static io.spine.test.Verify.assertContainsAll;
import static io.spine.test.Verify.assertSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Abstract base for test suites of {@code CommandBus}.
 *
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("ProtectedField") // OK for brevity of derived tests.
public abstract class AbstractCommandBusTestSuite {

    private final boolean multitenant;

    protected ActorRequestFactory requestFactory;

    protected CommandBus commandBus;
    protected CommandStore commandStore;
    protected Log log;
    protected EventBus eventBus;
    protected FailureBus failureBus;
    protected ExecutorCommandScheduler scheduler;
    protected CreateProjectHandler createProjectHandler;
    protected MemoizingObserver<Command> observer;

    /**
     * A public constructor for derived test cases.
     *
     * @param multitenant the multi-tenancy status of the {@code CommandBus} under tests
     */
    AbstractCommandBusTestSuite(boolean multitenant) {
        this.multitenant = multitenant;
    }

    static Command newCommandWithoutContext() {
        final Command cmd = createProject();
        final Command invalidCmd = cmd.toBuilder()
                                      .setContext(CommandContext.getDefaultInstance())
                                      .build();
        return invalidCmd;
    }

    static <E extends CommandException>
    void checkCommandError(@Nullable Throwable throwable,
                           CommandValidationError validationError,
                           Class<E> exceptionClass,
                           Command cmd) {
        assertNotNull(throwable);
        final Throwable cause = throwable.getCause();
        assertEquals(exceptionClass, cause.getClass());
        @SuppressWarnings("unchecked")
        final E exception = (E) cause;
        assertEquals(cmd, exception.getCommand());
        final Error error = exception.getError();
        assertEquals(CommandValidationError.getDescriptor()
                                           .getFullName(), error.getType());
        assertEquals(validationError.getNumber(), error.getCode());
        assertFalse(error.getMessage()
                         .isEmpty());
        if (validationError == INVALID_COMMAND) {
            assertFalse(error.getValidationError()
                             .getConstraintViolationList()
                             .isEmpty());
        }
    }

    protected static Command newCommandWithoutTenantId() {
        final Command cmd = createProject();
        final ActorContext.Builder withNoTenant =
                ActorContext.newBuilder()
                            .setTenantId(TenantId.getDefaultInstance());
        final Command invalidCmd =
                cmd.toBuilder()
                   .setContext(cmd.getContext()
                                  .toBuilder()
                                  .setActorContext(withNoTenant))
                   .build();
        return invalidCmd;
    }

    protected static Command clearTenantId(Command cmd) {
        final ActorContext.Builder withNoTenant =
                ActorContext.newBuilder()
                            .setTenantId(TenantId.getDefaultInstance());
        final Command result = cmd.toBuilder()
                                  .setContext(cmd.getContext()
                                                 .toBuilder()
                                                 .setActorContext(withNoTenant))
                                  .build();
        return result;
    }

    @Before
    public void setUp() {
        final InMemoryStorageFactory storageFactory =
                InMemoryStorageFactory.newInstance(getClass().getSimpleName(), this.multitenant);
        final TenantIndex tenantIndex = TenantAwareTest.createTenantIndex(this.multitenant,
                                                                          storageFactory);
        commandStore = spy(new CommandStore(storageFactory, tenantIndex));
        scheduler = spy(new ExecutorCommandScheduler());
        log = spy(new Log());
        failureBus = spy(FailureBus.newBuilder()
                                   .build());
        commandBus = CommandBus.newBuilder()
                               .setMultitenant(this.multitenant)
                               .setCommandStore(commandStore)
                               .setCommandScheduler(scheduler)
                               .setFailureBus(failureBus)
                               .setThreadSpawnAllowed(false)
                               .setLog(log)
                               .setAutoReschedule(false)
                               .build();
        eventBus = EventBus.newBuilder()
                           .setStorageFactory(storageFactory)
                           .build();
        requestFactory = this.multitenant
                            ? TestActorRequestFactory.newInstance(getClass(), newTenantUuid())
                            : TestActorRequestFactory.newInstance(getClass());
        createProjectHandler = new CreateProjectHandler();
        observer = StreamObservers.memoizingObserver();
    }

    @After
    public void tearDown() throws Exception {
        if (commandStore.isOpen()) { // then CommandBus is opened, too
            commandBus.close();
        }
        eventBus.close();
    }

    @Test
    public void post_commands_in_bulk() {
        final Command first = newCommand();
        final Command second = newCommand();
        final List<Command> commands = newArrayList(first, second);

        // Some derived test suite classes may register the handler in setUp().
        // This prevents the repeating registration (which is an illegal operation).
        commandBus.unregister(createProjectHandler);
        commandBus.register(createProjectHandler);

        final CommandBus spy = spy(commandBus);
        spy.post(commands, StreamObservers.<Command>memoizingObserver());

        @SuppressWarnings("unchecked")
        final ArgumentCaptor<Iterable<Command>> storingCaptor = forClass(Iterable.class);
        verify(spy).store(storingCaptor.capture());
        final Iterable<Command> storingArgs = storingCaptor.getValue();
        assertSize(commands.size(), storingArgs);
        assertContainsAll(storingArgs, first, second);

        final ArgumentCaptor<CommandEnvelope> postingCaptor = forClass(CommandEnvelope.class);
        verify(spy, times(2)).doPost(postingCaptor.capture(), any(StreamObserver.class));
        final List<CommandEnvelope> postingArgs = postingCaptor.getAllValues();
        assertSize(commands.size(), postingArgs);
        assertEquals(commands.get(0), postingArgs.get(0).getCommand());
        assertEquals(commands.get(1), postingArgs.get(1).getCommand());

        commandBus.unregister(createProjectHandler);
    }

    protected Command newCommand() {
        return Given.Command.createProject();
    }

    protected void checkResult(Command cmd) {
        assertNull(observer.getError());
        assertTrue(observer.isCompleted());
        assertEquals(cmd, observer.firstResponse());
    }

    void storeAsScheduled(Iterable<Command> commands,
                          Duration delay,
                          Timestamp schedulingTime) {
        for (Command cmd : commands) {
            final Command cmdWithSchedule = setSchedule(cmd, delay, schedulingTime);
            commandStore.store(cmdWithSchedule, SCHEDULED);
        }
    }

    /**
     * A sample command handler that tells whether a handler was invoked.
     */
    class CreateProjectHandler extends CommandHandler {

        private boolean handlerInvoked = false;

        CreateProjectHandler() {
            super(eventBus);
        }

        @Assign
        ProjectCreated handle(CreateProject command, CommandContext ctx) {
            handlerInvoked = true;
            return ProjectCreated.getDefaultInstance();
        }

        boolean wasHandlerInvoked() {
            return handlerInvoked;
        }
    }
}
