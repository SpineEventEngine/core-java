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

package io.spine.server.commandbus.given;

import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.base.ThrowableMessage;
import io.spine.client.ActorRequestFactory;
import io.spine.core.Command;
import io.spine.core.CommandClass;
import io.spine.core.CommandContext;
import io.spine.core.CommandEnvelope;
import io.spine.core.CommandId;
import io.spine.core.TenantId;
import io.spine.protobuf.TypeConverter;
import io.spine.server.command.Assign;
import io.spine.server.command.CommandHandler;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.commandbus.CommandDispatcher;
import io.spine.server.commandbus.ProcessingStatus;
import io.spine.server.commandstore.CommandStore;
import io.spine.server.event.EventBus;
import io.spine.server.tenant.TenantAwareFunction;
import io.spine.test.command.CmdAddTask;
import io.spine.test.command.CmdCreateProject;
import io.spine.test.command.CmdStartProject;
import io.spine.test.command.event.CmdProjectCreated;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.annotation.Nonnull;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.commandbus.Given.CommandMessage.createProjectMessage;

public class CommandStoreTestEnv {

    /** Prevents instantiation of this utility class. */
    private CommandStoreTestEnv() {
    }

    @SuppressWarnings("PublicField") // OK for brevity.
    public static class CommandStoreTestAssets {
        public final EventBus eventBus;
        public final CommandBus commandBus;
        public final ActorRequestFactory requestFactory;

        public CommandStoreTestAssets(EventBus eventBus,
                                      CommandBus commandBus,
                                      ActorRequestFactory requestFactory) {
            this.eventBus = eventBus;
            this.commandBus = commandBus;
            this.requestFactory = requestFactory;
        }
    }

    public static ProcessingStatus getStatus(CommandId commandId,
                                             TenantId tenantId,
                                             CommandStore commandStore) {
        TenantAwareFunction<CommandId, ProcessingStatus> func =
                new TenantAwareFunction<CommandId, ProcessingStatus>(tenantId) {
                    @Override
                    public @Nullable ProcessingStatus apply(@Nullable CommandId input) {
                        return commandStore.getStatus(checkNotNull(input));
                    }
                };
        return func.execute(commandId);
    }

    /**
     * A stub handler that throws passed `ThrowableMessage` in the command handler method,
     * rejecting the command.
     *
     * @see io.spine.server.commandbus.CommandStoreTest.SetCommandStatusTo#rejectionForHandlerRejection()
     */
    private static class RejectingCreateProjectHandler extends CommandHandler {

        @Nonnull
        private final ThrowableMessage throwable;

        protected RejectingCreateProjectHandler(@Nonnull ThrowableMessage throwable,
                                                EventBus eventBus) {
            super(eventBus);
            this.throwable = throwable;
        }

        @Assign
        @SuppressWarnings("unused")
            // Reflective access.
        CmdProjectCreated handle(CmdCreateProject msg,
                                 CommandContext context) throws ThrowableMessage {
            throw throwable;
        }
    }

    public static <E extends ThrowableMessage> Command
    givenRejectingHandler(E throwable, CommandStoreTestAssets assets) {
        CommandHandler handler = new RejectingCreateProjectHandler(throwable,
                                                                   assets.eventBus);
        assets.commandBus.register(handler);
        CmdCreateProject msg = createProjectMessage();
        Command command = assets.requestFactory.command()
                                               .create(msg);
        return command;
    }

    /**
     * A stub handler that throws passed `RuntimeException` in the command handler method.
     *
     * @see io.spine.server.commandbus.CommandStoreTest.SetCommandStatusTo#errorForHandlerException()
     */
    private static class ThrowingCreateProjectHandler extends CommandHandler {

        @Nonnull
        private final RuntimeException exception;

        protected ThrowingCreateProjectHandler(@Nonnull RuntimeException exception,
                                               EventBus eventBus) {
            super(eventBus);
            this.exception = exception;
        }

        @Assign
        @SuppressWarnings("unused")
            // Reflective access.
        CmdProjectCreated handle(CmdCreateProject msg, CommandContext context) {
            throw exception;
        }
    }

    public static <E extends RuntimeException> Command
    givenThrowingHandler(E exception, CommandStoreTestAssets assets) {
        CommandHandler handler = new ThrowingCreateProjectHandler(exception, assets.eventBus);
        assets.commandBus.register(handler);
        CmdCreateProject msg = createProjectMessage();
        Command command = assets.requestFactory.command()
                                               .create(msg);
        return command;
    }

    /**
     * A stub dispatcher that throws `RuntimeException` in the command dispatching method.
     *
     * @see io.spine.server.commandbus.CommandStoreTest.SetCommandStatusTo#errorForDispatcherException()
     */
    public static class ThrowingDispatcher implements CommandDispatcher<Message> {

        @SuppressWarnings("ThrowableInstanceNeverThrown")
        private final RuntimeException exception = new RuntimeException("Dispatching exception.");

        @Override
        public Set<CommandClass> getMessageClasses() {
            return CommandClass.setOf(CmdCreateProject.class, CmdStartProject.class,
                                      CmdAddTask.class);
        }

        @Override
        public Message dispatch(CommandEnvelope envelope) {
            throw exception;
        }

        @Override
        public void onError(CommandEnvelope envelope, RuntimeException exception) {
            // Do nothing.
        }

        public RuntimeException exception() {
            return exception;
        }
    }

    public static class TestRejection extends ThrowableMessage {
        private static final long serialVersionUID = 0L;

        public TestRejection() {
            super(TypeConverter.<String, StringValue>toMessage(TestRejection.class.getName()));
        }
    }
}
