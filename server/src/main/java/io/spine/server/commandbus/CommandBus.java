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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import io.spine.Identifier;
import io.spine.annotation.Internal;
import io.spine.base.Error;
import io.spine.base.ThrowableMessage;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.core.CommandClass;
import io.spine.core.CommandEnvelope;
import io.spine.core.Rejection;
import io.spine.protobuf.AnyPacker;
import io.spine.server.ServerEnvironment;
import io.spine.server.bus.Bus;
import io.spine.server.bus.BusFilter;
import io.spine.server.bus.DeadMessageTap;
import io.spine.server.bus.EnvelopeValidator;
import io.spine.server.commandstore.CommandStore;
import io.spine.server.rejection.RejectionBus;

import javax.annotation.Nullable;
import java.util.Deque;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Throwables.getRootCause;
import static io.spine.core.Rejections.causedByRejection;
import static io.spine.core.Rejections.toRejection;
import static io.spine.server.bus.Buses.acknowledge;
import static io.spine.server.bus.Buses.reject;
import static io.spine.util.Exceptions.toError;
import static java.lang.String.format;

/**
 * Dispatches the incoming commands to the corresponding handler.
 *
 * @author Alexander Yevsyukov
 * @author Mikhail Melnik
 * @author Alexander Litus
 * @author Alex Tymchenko
 * @author Dmytro Dashenkov
 */
public class CommandBus extends Bus<Command,
                                    CommandEnvelope,
                                    CommandClass,
                                    CommandDispatcher<?>> {

    private final CommandStore commandStore;

    private final Deque<BusFilter<CommandEnvelope>> filterChain;

    private final CommandScheduler scheduler;

    private final RejectionBus rejectionBus;

    private final Log log;

    /**
     * Is true, if the {@code BoundedContext} (to which this {@code CommandBus} belongs)
     * is multi-tenant.
     *
     * <p>If the {@code CommandBus} is multi-tenant, the commands posted must have the
     * {@code tenant_id} attribute defined.
     */
    private final boolean multitenant;

    /**
     * Determines whether the manual thread spawning is allowed within current runtime environment.
     *
     * <p>If set to {@code true}, {@code CommandBus} will be running some of internal processing in
     * parallel to improve performance.
     */
    private final boolean isThreadSpawnAllowed;

    private final DeadCommandTap deadCommandHandler;

    /**
     * Tha validator for the commands posted into this bus.
     *
     * <p>The value is effectively final, though should be initialized lazily.
     *
     * @see #getValidator() to getreive the non-null value of the validator
     */
    @Nullable
    private CommandValidator commandValidator;

    /**
     * Creates new instance according to the passed {@link Builder}.
     */
    @SuppressWarnings("ThisEscapedInObjectConstruction") // OK as nested objects only
    private CommandBus(Builder builder) {
        super();
        this.multitenant = builder.multitenant != null
                ? builder.multitenant
                : false;
        this.commandStore = builder.commandStore;
        this.scheduler = builder.commandScheduler;
        this.log = builder.log;
        this.isThreadSpawnAllowed = builder.threadSpawnAllowed;
        this.rejectionBus = builder.rejectionBus;
        this.filterChain = builder.getFilters();
        this.deadCommandHandler = new DeadCommandTap();
    }

    /**
     * Initializes the instance by rescheduling commands.
     */
    @VisibleForTesting
    void rescheduleCommands() {
        scheduler.rescheduleCommands();
    }

    /**
     * Creates a new {@link Builder} for the {@code CommandBus}.
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    @Internal
    @VisibleForTesting
    public boolean isMultitenant() {
        return multitenant;
    }

    boolean isThreadSpawnAllowed() {
        return isThreadSpawnAllowed;
    }

    @VisibleForTesting
    public CommandStore commandStore() {
        return commandStore;
    }

    Log problemLog() {
        return log;
    }

    @VisibleForTesting
    CommandScheduler scheduler() {
        return scheduler;
    }

    /**
     * Exposes the {@code RejectionBus} instance for this {@code CommandBus}.
     *
     * <p>This method is designed for internal use only. Client code should use
     * {@link io.spine.server.BoundedContext#getRejectionBus() BoundedContext.getRejectionBus()}
     * instead.
     */
    @Internal
    public RejectionBus rejectionBus() {
        return this.rejectionBus;
    }

    @Override
    protected CommandDispatcherRegistry createRegistry() {
        return new CommandDispatcherRegistry();
    }

    @SuppressWarnings("ReturnOfCollectionOrArrayField") // OK for a protected factory method
    @Override
    protected Deque<BusFilter<CommandEnvelope>> createFilterChain() {
        filterChain.push(scheduler);
        return filterChain;
    }

    @Override
    protected CommandEnvelope toEnvelope(Command message) {
        return CommandEnvelope.of(message);
    }

    @Override
    protected Ack doPost(CommandEnvelope envelope) {
        final CommandDispatcher<?> dispatcher = getDispatcher(envelope);
        Ack result;
        try {
            dispatcher.dispatch(envelope);
            commandStore.setCommandStatusOk(envelope);
            result = acknowledge(envelope.getId());
        } catch (RuntimeException e) {
            final Throwable cause = getRootCause(e);
            commandStore.updateCommandStatus(envelope, cause, log);

            if (causedByRejection(e)) {
                final ThrowableMessage throwableMessage = (ThrowableMessage) cause;
                final Rejection rejection = toRejection(throwableMessage, envelope.getCommand());
                final Class<?> rejectionClass = AnyPacker.unpack(rejection.getMessage())
                                                         .getClass();
                Log.log().trace("Posting rejection {} to RejectionBus.", rejectionClass.getName());
                rejectionBus().post(rejection);
                result = reject(envelope.getId(), rejection);
            } else {
                final Error error = toError(cause);
                result = reject(envelope.getId(), error);
            }
        }
        return result;
    }

    /**
     * Obtains the view {@code Set} of commands that are known to this {@code CommandBus}.
     *
     * <p>This set is changed when command dispatchers or handlers are registered or un-registered.
     *
     * @return a set of classes of supported commands
     */
    public Set<CommandClass> getRegisteredCommandClasses() {
        return registry().getRegisteredMessageClasses();
    }

    private Optional<? extends CommandDispatcher<?>> getDispatcher(CommandClass commandClass) {
        return registry().getDispatcher(commandClass);
    }

    @Override
    protected DeadMessageTap<CommandEnvelope> getDeadMessageHandler() {
        return deadCommandHandler;
    }

    @Override
    protected EnvelopeValidator<CommandEnvelope> getValidator() {
        if (commandValidator == null) {
            commandValidator = new CommandValidator(this);
        }
        return commandValidator;
    }

    /**
     * Passes a previously scheduled command to the corresponding dispatcher.
     */
    void postPreviouslyScheduled(Command command) {
        final CommandEnvelope commandEnvelope = CommandEnvelope.of(command);
        doPost(commandEnvelope);
    }

    private static IllegalStateException noDispatcherFound(CommandEnvelope commandEnvelope) {
        final String idStr = Identifier.toString(commandEnvelope.getId());
        final String msg = format("No dispatcher found for the command (class: %s id: %s).",
                                  commandEnvelope.getMessageClass()
                                                 .getClassName(),
                                  idStr);
        throw new IllegalStateException(msg);
    }

    @Override
    protected void store(Iterable<Command> commands) {
        for (Command command : commands) {
            commandStore().store(command);
        }
    }

    private CommandDispatcher<?> getDispatcher(CommandEnvelope commandEnvelope) {
        final Optional<? extends CommandDispatcher<?>> dispatcher = getDispatcher(
                commandEnvelope.getMessageClass()
        );
        if (!dispatcher.isPresent()) {
            throw noDispatcherFound(commandEnvelope);
        }
        return dispatcher.get();
    }

    /**
     * Closes the instance, preventing any for further posting of commands.
     *
     * <p>The following operations are performed:
     * <ol>
     * <li>All command dispatchers are un-registered.
     * <li>{@code CommandStore} is closed.
     * <li>{@code CommandScheduler} is shut down.
     * </ol>
     *
     * @throws Exception if closing the {@code CommandStore} cases an exception
     */
    @Override
    public void close() throws Exception {
        super.close();
        commandStore.close();
        rejectionBus.close();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides for return type covariance.
     */
    @Override
    protected CommandDispatcherRegistry registry() {
        return (CommandDispatcherRegistry) super.registry();
    }

    /**
     * The {@code Builder} for {@code CommandBus}.
     */
    public static class Builder extends AbstractBuilder<CommandEnvelope, Command, Builder> {

        /**
         * The multi-tenancy flag for the {@code CommandBus} to build.
         *
         * <p>The value of this field should be equal to that of corresponding
         * {@linkplain io.spine.server.BoundedContext.Builder BoundedContext.Builder} and is not
         * supposed to be {@linkplain #setMultitenant(Boolean) set directly}.
         *
         * <p>If set directly, the value would be matched to the multi-tenancy flag of
         * {@code BoundedContext}.
         */
        @Nullable
        private Boolean multitenant;

        private CommandStore commandStore;

        private Log log;

        /**
         * Optional field for the {@code CommandBus}.
         *
         * <p>If unset, the default {@link ExecutorCommandScheduler} implementation is used.
         */
        private CommandScheduler commandScheduler;

        /** @see #setThreadSpawnAllowed(boolean) */
        private boolean threadSpawnAllowed = detectThreadsAllowed();

        /** @see #setAutoReschedule(boolean) */
        private boolean autoReschedule;

        private RejectionBus rejectionBus;

        /**
         * Checks whether the manual {@link Thread} spawning is allowed within
         * the current runtime environment.
         */
        private static boolean detectThreadsAllowed() {
            final boolean appEngine = ServerEnvironment.isAppEngine();
            return !appEngine;
        }

        @Internal
        @Nullable
        public Boolean isMultitenant() {
            return multitenant;
        }

        @Internal
        public Builder setMultitenant(@Nullable Boolean multitenant) {
            this.multitenant = multitenant;
            return this;
        }

        public boolean isThreadSpawnAllowed() {
            return threadSpawnAllowed;
        }

        public CommandStore getCommandStore() {
            return commandStore;
        }

        public Optional<CommandScheduler> getCommandScheduler() {
            return Optional.fromNullable(commandScheduler);
        }

        public Optional<RejectionBus> getRejectionBus() {
            return Optional.fromNullable(rejectionBus);
        }

        public Builder setCommandStore(CommandStore commandStore) {
            checkNotNull(commandStore);
            this.commandStore = commandStore;
            return this;
        }

        public Builder setCommandScheduler(CommandScheduler commandScheduler) {
            checkNotNull(commandScheduler);
            this.commandScheduler = commandScheduler;
            return this;
        }

        public Builder setRejectionBus(RejectionBus rejectionBus) {
            checkNotNull(rejectionBus);
            this.rejectionBus = rejectionBus;
            return this;
        }

        /**
         * Enables or disables creating threads for {@code CommandBus} operations.
         *
         * <p>If set to {@code true}, the {@code CommandBus} will be creating instances of
         * {@link Thread} for potentially time consuming operation.
         *
         * <p>However, some runtime environments, such as Google AppEngine Standard,
         * do not allow manual thread spawning. In this case, this flag should be set
         * to {@code false}.
         *
         * <p>If not set explicitly, the default value of this flag is set upon the best guess,
         * based on current {@link io.spine.server.ServerEnvironment server environment}.
         */
        public Builder setThreadSpawnAllowed(boolean threadSpawnAllowed) {
            this.threadSpawnAllowed = threadSpawnAllowed;
            return this;
        }

        /**
         * Sets the log for logging errors.
         */
        @VisibleForTesting
        Builder setLog(Log log) {
            this.log = log;
            return this;
        }

        /**
         * If not set the builder will not call {@link CommandBus#rescheduleCommands()}.
         *
         * <p>One of the applications of this flag is to disable rescheduling of commands in tests.
         */
        @VisibleForTesting
        Builder setAutoReschedule(boolean autoReschedule) {
            this.autoReschedule = autoReschedule;
            return this;
        }

        private Builder() {
            super();
            // Do not allow creating builder instances directly.
        }

        /**
         * Builds an instance of {@link CommandBus}.
         *
         * <p>This method is supposed to be called internally when building an enclosing
         * {@code BoundedContext}.
         */
        @Override
        @Internal
        public CommandBus build() {
            checkState(
                    commandStore != null,
                    "CommandStore must be set. Please call CommandBus.Builder.setCommandStore()."
            );

            if (commandScheduler == null) {
                commandScheduler = new ExecutorCommandScheduler();
            }

            if (log == null) {
                log = new Log();
            }

            if (rejectionBus == null) {
                rejectionBus = RejectionBus.newBuilder()
                                           .build();
            }

            final CommandBus commandBus = createCommandBus();

            commandScheduler.setCommandBus(commandBus);

            if (autoReschedule) {
                commandBus.rescheduleCommands();
            }

            return commandBus;
        }

        @Override
        protected Builder self() {
            return this;
        }

        private CommandBus createCommandBus() {
            final CommandBus commandBus = new CommandBus(this);

            // Enforce creating the registry to make spying for CommandBus-es in tests work.
            commandBus.registry();

            return commandBus;
        }
    }

    /**
     * Produces an {@link UnsupportedCommandException} upon a dead command.
     */
    private class DeadCommandTap implements DeadMessageTap<CommandEnvelope> {

        @Override
        public UnsupportedCommandException capture(CommandEnvelope message) {
            final Command command = message.getCommand();
            final UnsupportedCommandException exception = new UnsupportedCommandException(command);
            commandStore().storeWithError(command, exception);
            return exception;
        }
    }
}
