/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import io.spine.base.Error;
import io.spine.base.ThrowableMessage;
import io.spine.core.Command;
import io.spine.core.CommandClass;
import io.spine.core.CommandContext;
import io.spine.core.CommandEnvelope;
import io.spine.core.CommandId;
import io.spine.core.CommandStatus;
import io.spine.core.TenantId;
import io.spine.protobuf.TypeConverter;
import io.spine.server.command.Assign;
import io.spine.server.command.CommandHandler;
import io.spine.server.model.ModelTests;
import io.spine.server.tenant.TenantAwareFunction;
import io.spine.test.TimeTests;
import io.spine.test.command.CmdAddTask;
import io.spine.test.command.CmdCreateProject;
import io.spine.test.command.CmdStartProject;
import io.spine.test.command.event.CmdProjectCreated;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static io.spine.core.Commands.getMessage;
import static io.spine.core.Rejections.toRejection;
import static io.spine.server.commandbus.Given.ACommand.addTask;
import static io.spine.server.commandbus.Given.ACommand.createProject;
import static io.spine.server.commandbus.Given.ACommand.startProject;
import static io.spine.server.commandbus.Given.CommandMessage.createProjectMessage;
import static io.spine.time.Durations2.fromMinutes;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;

public abstract class CommandStoreShould extends AbstractCommandBusTestSuite {

    CommandStoreShould(boolean multitenant) {
        super(multitenant);
    }

    @Test
    public void set_command_status_to_OK_when_handler_returns() {
        commandBus.register(createProjectHandler);

        final Command command = requestFactory.command()
                                              .create(createProjectMessage());
        commandBus.post(command, observer);

        final TenantId tenantId = command.getContext()
                                         .getActorContext()
                                         .getTenantId();
        final CommandId commandId = command.getId();
        final ProcessingStatus status = getStatus(commandId, tenantId);

        assertEquals(CommandStatus.OK, status.getCode());
    }

    @Test
    public void set_command_status_to_error_when_dispatcher_throws() {
        final ThrowingDispatcher dispatcher = new ThrowingDispatcher();
        commandBus.register(dispatcher);
        final Command command = requestFactory.command()
                                              .create(createProjectMessage());

        commandBus.post(command, observer);

        // Check that the logging was called.
        final CommandEnvelope envelope = CommandEnvelope.of(command);
        verify(log).errorHandling(dispatcher.exception,
                                  envelope.getMessage(),
                                  envelope.getId());

        // Check that the command status has the correct code,
        // and the error matches the thrown exception.
        assertHasErrorStatusWithMessage(envelope, dispatcher.exception.getMessage());
    }

    @Test
    public void set_command_status_to_rejection_when_handler_throws_rejection() {
        ModelTests.clearModel();

        final TestRejection rejection = new TestRejection();
        final Command command = givenThrowingHandler(rejection);
        final CommandId commandId = command.getId();
        final Message commandMessage = getMessage(command);

        commandBus.post(command, observer);

        // Check that the logging was called.
        verify(log).rejectedWith(eq(rejection), eq(commandMessage), eq(commandId));

        // Check that the status has the correct code,
        // and the rejection matches the thrown rejection.
        final TenantId tenantId = command.getContext()
                                         .getActorContext()
                                         .getTenantId();
        final ProcessingStatus status = getStatus(commandId, tenantId);

        assertEquals(CommandStatus.REJECTED, status.getCode());
        assertEquals(toRejection(rejection, command).getMessage(),
                     status.getRejection()
                           .getMessage());
    }

    private ProcessingStatus getStatus(CommandId commandId, final TenantId tenantId) {
        final TenantAwareFunction<CommandId, ProcessingStatus> func =
                new TenantAwareFunction<CommandId, ProcessingStatus>(tenantId) {
                    @Nullable
                    @Override
                    public ProcessingStatus apply(@Nullable CommandId input) {
                        return commandStore.getStatus(checkNotNull(input));
                    }
                };
        return func.execute(commandId);
    }

    @Test
    public void set_command_status_to_error_when_handler_throws_exception() {
        ModelTests.clearModel();

        final RuntimeException exception = new IllegalStateException("handler throws");
        final Command command = givenThrowingHandler(exception);
        final CommandEnvelope envelope = CommandEnvelope.of(command);

        commandBus.post(command, observer);

        // Check that the logging was called.
        verify(log).errorHandling(eq(exception),
                                  eq(envelope.getMessage()),
                                  eq(envelope.getId()));

        final String errorMessage = exception.getMessage();
        assertHasErrorStatusWithMessage(envelope, errorMessage);
    }

    /**
     * Checks that the command status has the correct code, and the stored error message matches the
     * passed message.
     *
     * <p>The check is performed as a tenant-aware function using the tenant ID from the command.
     */
    private void assertHasErrorStatusWithMessage(CommandEnvelope commandEnvelope,
                                                 String errorMessage) {
        final ProcessingStatus status = getProcessingStatus(commandEnvelope);
        assertEquals(CommandStatus.ERROR, status.getCode());
        assertEquals(errorMessage, status.getError()
                                         .getMessage());
    }

    private ProcessingStatus getProcessingStatus(CommandEnvelope commandEnvelope) {
        final TenantId tenantId = commandEnvelope.getCommandContext()
                                                 .getActorContext()
                                                 .getTenantId();
        final TenantAwareFunction<CommandId, ProcessingStatus> func =
                new TenantAwareFunction<CommandId, ProcessingStatus>(tenantId) {
                    @Override
                    public ProcessingStatus apply(@Nullable CommandId input) {
                        return commandStore.getStatus(checkNotNull(input));
                    }
                };
        final ProcessingStatus result = func.execute(commandEnvelope.getId());
        return result;
    }

    @Test
    public void set_command_status_to_error_when_handler_throws_unknown_Throwable()
            throws TestRejection, TestThrowable {
        ModelTests.clearModel();

        final Throwable throwable = new TestThrowable("Unexpected Throwable");
        final Command command = givenThrowingHandler(throwable);
        final CommandEnvelope envelope = CommandEnvelope.of(command);

        commandBus.post(command, observer);

        // Check that the logging was called.
        verify(log).errorHandlingUnknown(eq(throwable),
                                         eq(envelope.getMessage()),
                                         eq(envelope.getId()));

        // Check that the status and message.
        assertHasErrorStatusWithMessage(envelope, throwable.getMessage());
    }

    @Test
    public void set_expired_scheduled_command_status_to_error_if_time_to_post_them_passed() {
        final List<Command> commands = newArrayList(createProject(),
                                                    addTask(),
                                                    startProject());
        final Duration delay = fromMinutes(5);
        final Timestamp schedulingTime = TimeTests.Past.minutesAgo(10); // time to post passed
        storeAsScheduled(commands, delay, schedulingTime);

        commandBus.rescheduleCommands();

        for (Command cmd : commands) {
            final CommandEnvelope envelope = CommandEnvelope.of(cmd);
            final Message msg = envelope.getMessage();
            final CommandId id = envelope.getId();

            // Check the expired status error was set.
            final ProcessingStatus status = getProcessingStatus(envelope);

            // Check that the logging was called.
            verify(log).errorExpiredCommand(msg, id);

            final Error expected = CommandExpiredException.commandExpired(cmd);
            assertEquals(expected, status.getError());
        }
    }

    /**
     * A stub handler that throws passed `Throwable` in the command handler method.
     *
     * @see #set_command_status_to_rejection_when_handler_throws_rejection()
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
        CmdProjectCreated handle(CmdCreateProject msg, CommandContext context) throws Throwable {
            throw throwable;
        }
    }

    private <E extends Throwable> Command givenThrowingHandler(E throwable) {
        final CommandHandler handler = new ThrowingCreateProjectHandler(throwable);
        commandBus.register(handler);
        final CmdCreateProject msg = createProjectMessage();
        final Command command = requestFactory.command()
                                              .create(msg);
        return command;
    }

    /*
     * Throwables
     ********************/

    private static class TestRejection extends ThrowableMessage {
        private static final long serialVersionUID = 0L;

        private TestRejection() {
            super(TypeConverter.<String, StringValue>toMessage(TestRejection.class.getName()));
        }
    }

    @SuppressWarnings("serial")
    private static class TestThrowable extends Throwable {
        private TestThrowable(String message) {
            super(message);
        }
    }

    private static class ThrowingDispatcher implements CommandDispatcher<Message> {

        @SuppressWarnings("ThrowableInstanceNeverThrown")
        private final RuntimeException exception = new RuntimeException("Dispatching exception.");

        @Override
        public Set<CommandClass> getMessageClasses() {
            return CommandClass.setOf(CmdCreateProject.class, CmdStartProject.class, CmdAddTask.class);
        }

        @Override
        public Message dispatch(CommandEnvelope envelope) {
            throw exception;
        }

        @Override
        public void onError(CommandEnvelope envelope, RuntimeException exception) {
            // Do nothing.
        }
    }
}
