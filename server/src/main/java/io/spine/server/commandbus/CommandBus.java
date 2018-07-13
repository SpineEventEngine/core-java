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
package io.spine.server.commandbus;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.CheckReturnValue;
import io.spine.annotation.Internal;
import io.spine.base.Identifier;
import io.spine.base.ThrowableMessage;
import io.spine.core.Command;
import io.spine.core.CommandClass;
import io.spine.core.CommandEnvelope;
import io.spine.core.Rejection;
import io.spine.protobuf.AnyPacker;
import io.spine.server.ServerEnvironment;
import io.spine.server.bus.Bus;
import io.spine.server.bus.BusFilter;
import io.spine.server.bus.DeadMessageHandler;
import io.spine.server.bus.EnvelopeValidator;
import io.spine.server.commandstore.CommandStore;
import io.spine.server.rejection.RejectionBus;
import io.spine.system.server.DispatchCommand;
import io.spine.system.server.SystemGateway;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Deque;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Throwables.getRootCause;
import static io.spine.core.Rejections.causedByRejection;
import static io.spine.core.Rejections.toRejection;
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

    private final SystemGateway systemGateway;

    /**
     * Is {@code true}, if the {@code BoundedContext} (to which this {@code CommandBus} belongs)
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

    private final DeadCommandHandler deadCommandHandler;

    /**
     * Tha validator for the commands posted into this bus.
     *
     * <p>The value is effectively final, though should be initialized lazily.
     *
     * @see #getValidator() to getreive the non-null value of the validator
     */
    private @Nullable CommandValidator commandValidator;

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
        this.systemGateway = builder.systemGateway;
        this.filterChain = builder.getFilters();
        this.deadCommandHandler = new DeadCommandHandler();
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
        Builder builder = new Builder();
        return builder;
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
        BusFilter<CommandEnvelope> gatewayFilter = new CommandReceivedTap(systemGateway);
        filterChain.push(gatewayFilter);
        filterChain.push(scheduler);
        return filterChain;
    }

    @Override
    protected CommandEnvelope toEnvelope(Command message) {
        return CommandEnvelope.of(message);
    }

    @Override
    protected void dispatch(CommandEnvelope envelope) {
        final CommandDispatcher<?> dispatcher = getDispatcher(envelope);
        try {
            dispatcher.dispatch(envelope);
            onCommandDispatched(envelope);
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
            }
        }
    }

    private void onCommandDispatched(CommandEnvelope command) {
        DispatchCommand systemCommand = DispatchCommand
                .newBuilder()
                .setId(command.getId())
                .build();
        systemGateway.postCommand(systemCommand, command.getTenantId());
        commandStore.setCommandStatusOk(command);
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
    protected DeadMessageHandler<CommandEnvelope> getDeadMessageHandler() {
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
        CommandEnvelope commandEnvelope = CommandEnvelope.of(command);
        dispatch(commandEnvelope);
    }

    private static IllegalStateException noDispatcherFound(CommandEnvelope commandEnvelope) {
        final String idStr = Identifier.toString(commandEnvelope.getId());
        final String msg = format("No dispatcher found for the command (class: %s id: %s).",
                                  commandEnvelope.getMessageClass()
                                                 .toString(),
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
        Optional<? extends CommandDispatcher<?>> dispatcher =
                getDispatcher(commandEnvelope.getMessageClass());
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
    @CanIgnoreReturnValue
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
        private @Nullable Boolean multitenant;

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

        private SystemGateway systemGateway;

        /**
         * Checks whether the manual {@link Thread} spawning is allowed within
         * the current runtime environment.
         */
        private static boolean detectThreadsAllowed() {
            boolean appEngine = ServerEnvironment.getInstance()
                                                 .isAppEngine();
            return !appEngine;
        }

        @Internal
        public @Nullable Boolean isMultitenant() {
            return multitenant;
        }

        @Internal
        @CanIgnoreReturnValue
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

        @CanIgnoreReturnValue
        public Builder setCommandStore(CommandStore commandStore) {
            checkNotNull(commandStore);
            this.commandStore = commandStore;
            return this;
        }

        @CanIgnoreReturnValue
        public Builder setCommandScheduler(CommandScheduler commandScheduler) {
            checkNotNull(commandScheduler);
            this.commandScheduler = commandScheduler;
            return this;
        }

        @CanIgnoreReturnValue
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
        @CanIgnoreReturnValue
        public Builder setThreadSpawnAllowed(boolean threadSpawnAllowed) {
            this.threadSpawnAllowed = threadSpawnAllowed;
            return this;
        }

        /**
         * Inject the {@link SystemGateway} of the bounded context to which the built bus belongs.
         *
         * <p>This method is {@link Internal} to the framework. The name of the method starts with
         * {@code inject} prefix so that this method does not appear in an autocomplete hint for
         * {@code set} prefix.
         */
        @Internal
        public Builder injectSystemGateway(SystemGateway gateway) {
            this.systemGateway = checkNotNull(gateway);
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
        @CheckReturnValue
        public CommandBus build() {
            checkSet(commandStore, CommandStore.class, "setCommandStore");
            checkSet(systemGateway, SystemGateway.class, "injectSystemGateway");

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

        private static void checkSet(@Nullable Object field,
                                     Class<?> fieldType,
                                     String setterName) {
            checkState(field != null,
                       "%s must be set. Please call CommandBus.Builder.%s().",
                       fieldType.getSimpleName(), setterName);
        }

        @Override
        protected Builder self() {
            return this;
        }

        @SuppressWarnings("CheckReturnValue")
            /* Calling registry() enforces creating the registry to make spying for CommandBus
               instances in tests work. */
        private CommandBus createCommandBus() {
            CommandBus commandBus = new CommandBus(this);
            commandBus.registry();
            return commandBus;
        }
    }

    /**
     * Produces an {@link UnsupportedCommandException} upon a dead command.
     */
    private class DeadCommandHandler implements DeadMessageHandler<CommandEnvelope> {

        @Override
        public UnsupportedCommandException handle(CommandEnvelope message) {
            Command command = message.getCommand();
            UnsupportedCommandException exception = new UnsupportedCommandException(command);
            commandStore().storeWithError(command, exception);
            return exception;
        }
    }
}
