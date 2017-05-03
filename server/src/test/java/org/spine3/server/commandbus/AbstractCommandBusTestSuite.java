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

package org.spine3.server.commandbus;

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import org.junit.After;
import org.junit.Before;
import org.spine3.base.ActorContext;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandValidationError;
import org.spine3.base.Error;
import org.spine3.client.ActorRequestFactory;
import org.spine3.server.command.Assign;
import org.spine3.server.command.CommandHandler;
import org.spine3.server.commandstore.CommandStore;
import org.spine3.server.event.EventBus;
import org.spine3.server.failure.FailureBus;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.server.tenant.TenantAwareTest;
import org.spine3.server.tenant.TenantIndex;
import org.spine3.test.TestActorRequestFactory;
import org.spine3.test.command.CreateProject;
import org.spine3.test.command.event.ProjectCreated;
import org.spine3.testdata.TestEventBusFactory;
import org.spine3.testdata.TestFailureBusFactory;
import org.spine3.users.TenantId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.spy;
import static org.spine3.base.CommandStatus.SCHEDULED;
import static org.spine3.base.CommandValidationError.INVALID_COMMAND;
import static org.spine3.server.commandbus.CommandScheduler.setSchedule;
import static org.spine3.server.commandbus.Given.Command.createProject;
import static org.spine3.test.Tests.newTenantUuid;

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
    protected TestResponseObserver responseObserver;

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
    void checkCommandError(Throwable throwable,
                           CommandValidationError validationError,
                           Class<E> exceptionClass,
                           Command cmd) {
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
                InMemoryStorageFactory.getInstance(this.multitenant);
        final TenantIndex tenantIndex = TenantAwareTest.createTenantIndex(this.multitenant,
                                                                          storageFactory);
        commandStore = spy(new CommandStore(storageFactory, tenantIndex));
        scheduler = spy(new ExecutorCommandScheduler());
        log = spy(new Log());
        failureBus = spy(TestFailureBusFactory.create());
        commandBus = CommandBus.newBuilder()
                               .setMultitenant(this.multitenant)
                               .setCommandStore(commandStore)
                               .setCommandScheduler(scheduler)
                               .setFailureBus(failureBus)
                               .setThreadSpawnAllowed(false)
                               .setLog(log)
                               .setAutoReschedule(false)
                               .build();
        eventBus = TestEventBusFactory.create(storageFactory);
        requestFactory = this.multitenant
                            ? TestActorRequestFactory.newInstance(getClass(), newTenantUuid())
                            : TestActorRequestFactory.newInstance(getClass());
        createProjectHandler = new CreateProjectHandler();
        responseObserver = new TestResponseObserver();
    }

    @After
    public void tearDown() throws Exception {
        if (commandStore.isOpen()) { // then CommandBus is opened, too
            commandBus.close();
        }
        eventBus.close();
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
