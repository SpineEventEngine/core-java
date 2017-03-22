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

package org.spine3.server.command;

import com.google.protobuf.Duration;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.junit.Test;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandId;
import org.spine3.base.CommandStatus;
import org.spine3.base.FailureThrowable;
import org.spine3.envelope.CommandEnvelope;
import org.spine3.test.command.AddTask;
import org.spine3.test.command.CreateProject;
import org.spine3.test.command.StartProject;
import org.spine3.test.command.event.ProjectCreated;
import org.spine3.type.CommandClass;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.spine3.base.CommandStatus.SCHEDULED;
import static org.spine3.base.Commands.getId;
import static org.spine3.base.Commands.getMessage;
import static org.spine3.protobuf.Durations2.fromMinutes;
import static org.spine3.protobuf.Values.newStringValue;
import static org.spine3.server.command.CommandExpiredException.commandExpiredError;
import static org.spine3.server.command.CommandScheduler.setSchedule;
import static org.spine3.server.command.Given.Command.addTask;
import static org.spine3.server.command.Given.Command.createProject;
import static org.spine3.server.command.Given.Command.startProject;
import static org.spine3.server.command.Given.CommandMessage.createProjectMessage;
import static org.spine3.test.TimeTests.Past.minutesAgo;

@SuppressWarnings({"ClassWithTooManyMethods", "OverlyCoupledClass"})
public class CommandBusShouldHandleCommandStatus extends AbstractCommandBusTestSuite {

    public CommandBusShouldHandleCommandStatus() {
        super(true);
    }

    @Test
    public void set_command_status_to_OK_when_handler_returns() {
        commandBus.register(createProjectHandler);

        final Command command = createProject();
        commandBus.post(command, responseObserver);

        assertEquals(CommandStatus.OK, commandStore.getStatusCode(command.getContext()
                                                                         .getCommandId()));
    }

    @Test
    public void set_command_status_to_error_when_dispatcher_throws() {
        final ThrowingDispatcher dispatcher = new ThrowingDispatcher();
        commandBus.register(dispatcher);
        final Command command = commandFactory.createCommand(createProjectMessage());

        commandBus.post(command, responseObserver);

        // Check that the logging was called.
        final CommandEnvelope envelope = CommandEnvelope.of(command);
        verify(log).errorHandling(dispatcher.exception,
                                  envelope.getMessage(),
                                  envelope.getCommandId());

        // Check that the command status has the correct code,
        // and the error matches the thrown exception.
        assertHasErrorStatusWithMessage(envelope.getCommandId(), dispatcher.exception.getMessage());
    }

    @Test
    public void set_command_status_to_failure_when_handler_throws_failure() {
        final TestFailure failure = new TestFailure();
        final Command command = givenThrowingHandler(failure);
        final CommandId commandId = getId(command);
        final Message commandMessage = getMessage(command);

        commandBus.post(command, responseObserver);

        // Check that the logging was called.
        verify(log).failureHandling(eq(failure), eq(commandMessage), eq(commandId));

        // Check that the status has the correct code,
        // and the failure matches the thrown failure.
        final ProcessingStatus status = commandStore.getStatus(commandId);
        assertEquals(CommandStatus.FAILURE, status.getCode());

        assertEquals(failure.toFailure()
                            .getMessage(), status.getFailure()
                                                 .getMessage());
    }

    @Test
    public void set_command_status_to_error_when_handler_throws_exception() {
        final RuntimeException exception = new IllegalStateException("handler throws");
        final Command command = givenThrowingHandler(exception);
        final CommandId commandId = getId(command);
        final Message commandMessage = getMessage(command);

        commandBus.post(command, responseObserver);

        // Check that the logging was called.
        verify(log).errorHandling(eq(exception), eq(commandMessage), eq(commandId));

        final String errorMessage = exception.getMessage();
        assertHasErrorStatusWithMessage(commandId, errorMessage);

    }

    private void assertHasErrorStatusWithMessage(CommandId commandId, String errorMessage) {
        // Check that the command status has the correct code,
        // and the error matches the thrown exception.
        final ProcessingStatus status = commandStore.getStatus(commandId);
        assertEquals(CommandStatus.ERROR, status.getCode());
        assertEquals(errorMessage, status.getError()
                                         .getMessage());
    }

    @Test
    public void set_command_status_to_error_when_handler_throws_unknown_Throwable()
            throws TestFailure, TestThrowable {
        final Throwable throwable = new TestThrowable("Unexpected Throwable");
        final Command command = givenThrowingHandler(throwable);
        final CommandId commandId = getId(command);
        final Message commandMessage = getMessage(command);

        commandBus.post(command, responseObserver);

        // Check that the logging was called.
        verify(log).errorHandlingUnknown(eq(throwable), eq(commandMessage), eq(commandId));

        // Check that the status and message.
        assertHasErrorStatusWithMessage(commandId, throwable.getMessage());
    }

    @Test
    public void set_expired_scheduled_command_status_to_error_if_time_to_post_them_passed() {
        final List<Command> commands = newArrayList(createProject(),
                                                    addTask(),
                                                    startProject());
        final Duration delay = fromMinutes(5);
        final Timestamp schedulingTime = minutesAgo(10); // time to post passed
        storeAsScheduled(commands, delay, schedulingTime);

        commandBus.rescheduler()
                  .doRescheduleCommands();

        for (Command cmd : commands) {
            final Message msg = getMessage(cmd);
            final CommandId id = getId(cmd);

            // Check the expired status error was set.
            final ProcessingStatus status = commandStore.getStatus(id);
            assertEquals(status.getError(), commandExpiredError(msg));

            // Check that the logging was called.
            verify(log).errorExpiredCommand(msg, id);
        }
    }

    private void storeAsScheduled(Iterable<Command> commands,
                                  Duration delay,
                                  Timestamp schedulingTime) {
        for (Command cmd : commands) {
            final Command cmdWithSchedule = setSchedule(cmd, delay, schedulingTime);
            commandStore.store(cmdWithSchedule, SCHEDULED);
        }
    }

    /**
     * A stub handler that throws passed `Throwable` in the command handler method.
     *
     * @see #set_command_status_to_failure_when_handler_throws_failure
     * @see #set_command_status_to_error_when_handler_throws_exception
     * @see #set_command_status_to_error_when_handler_throws_unknown_Throwable
     */
    private class ThrowingCreateProjectHandler extends CommandHandler {

        @Nonnull
        private final Throwable throwable;

        protected ThrowingCreateProjectHandler(@Nonnull Throwable throwable) {
            super(eventBus);
            this.throwable = throwable;
        }

        @Assign
        @SuppressWarnings({"unused", "ProhibitedExceptionThrown"})
            // Throwing is the purpose of this method.
        ProjectCreated handle(CreateProject msg, CommandContext context) throws Throwable {
            throw throwable;
        }
    }

    private <E extends Throwable> Command givenThrowingHandler(E throwable) {
        final CommandHandler handler = new ThrowingCreateProjectHandler(throwable);
        commandBus.register(handler);
        final CreateProject msg = createProjectMessage();
        final Command command = commandFactory.createCommand(msg);
        return command;
    }

    /*
     * Throwables
     ********************/

    private static class TestFailure extends FailureThrowable {
        private static final long serialVersionUID = 1L;

        private TestFailure() {
            super(newStringValue("some Command message"),
                  CommandContext.getDefaultInstance(),
                  newStringValue(TestFailure.class.getName()));
        }
    }

    @SuppressWarnings("serial")
    private static class TestThrowable extends Throwable {
        private TestThrowable(String message) {
            super(message);
        }
    }

    private static class ThrowingDispatcher implements CommandDispatcher {

        @SuppressWarnings("ThrowableInstanceNeverThrown")
        private final RuntimeException exception = new RuntimeException("Dispatching exception.");

        @Override
        public Set<CommandClass> getMessageClasses() {
            return CommandClass.setOf(CreateProject.class, StartProject.class, AddTask.class);
        }

        @Override
        public void dispatch(CommandEnvelope envelope) {
            throw exception;
        }
    }
}
