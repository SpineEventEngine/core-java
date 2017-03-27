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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import io.grpc.stub.StreamObserver;
import org.spine3.base.Command;
import org.spine3.base.Identifiers;
import org.spine3.base.Response;
import org.spine3.base.Responses;
import org.spine3.envelope.CommandEnvelope;
import org.spine3.server.Statuses;
import org.spine3.server.bus.Bus;
import org.spine3.server.failure.FailureBus;
import org.spine3.server.tenant.TenantIndex;
import org.spine3.type.CommandClass;
import org.spine3.users.TenantId;
import org.spine3.util.Environment;

import javax.annotation.Nullable;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Throwables.getRootCause;
import static java.lang.String.format;
import static org.spine3.base.CommandStatus.SCHEDULED;
import static org.spine3.base.Commands.isScheduled;
import static org.spine3.validate.Validate.isNotDefault;

/**
 * Dispatches the incoming commands to the corresponding handler.
 *
 * @author Alexander Yevsyukov
 * @author Mikhail Melnik
 * @author Alexander Litus
 * @author Alex Tymchenko
 */
public class CommandBus extends Bus<Command, CommandEnvelope, CommandClass, CommandDispatcher> {

    private final Filter filter;

    private final CommandStore commandStore;

    private final CommandStore.StatusService commandStatusService;

    private final CommandScheduler scheduler;

    private final Rescheduler rescheduler;

    private final FailureBus failureBus;

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

    /**
     * The repository for storing tenant IDs of incoming commands.
     *
     * <p>This field is {@code null} if the command bus is single-tenant.
     */
    @Nullable
    private final TenantIndex tenantIndex;

    /**
     * Creates new instance according to the passed {@link Builder}.
     */
    @SuppressWarnings("ThisEscapedInObjectConstruction") // OK as nested objects only
    private CommandBus(Builder builder) {
        super();
        this.multitenant = builder.multitenant;
        this.tenantIndex = this.multitenant
                                ? builder.tenantIndex
                                : null;
        this.commandStore = builder.commandStore;
        this.commandStatusService = commandStore.createStatusService(builder.log);
        this.scheduler = builder.commandScheduler;
        this.log = builder.log;
        this.isThreadSpawnAllowed = builder.threadSpawnAllowed;
        this.filter = new Filter(this);
        this.rescheduler = new Rescheduler(this);
        this.failureBus = builder.failureBus;
    }

    /**
     * Initializes the instance by rescheduling commands.
     */
    private void rescheduleCommands() {
        rescheduler.rescheduleCommands();
    }

    /**
     * Creates a new {@link Builder} for the {@code CommandBus}.
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    public boolean isMultitenant() {
        return multitenant;
    }

    boolean isThreadSpawnAllowed() {
        return isThreadSpawnAllowed;
    }

    CommandStore commandStore() {
        return commandStore;
    }

    Log problemLog() {
        return log;
    }

    @VisibleForTesting
    Rescheduler rescheduler() {
        return rescheduler;
    }

    @VisibleForTesting
    CommandScheduler scheduler() {
        return scheduler;
    }

    @VisibleForTesting
    FailureBus failureBus() {
        return this.failureBus;
    }

    @Override
    protected CommandDispatcherRegistry createRegistry() {
        return new CommandDispatcherRegistry();
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

    /**
     * Obtains the instance of the {@link CommandStore.StatusService CommandStore.StatusService}
     * associated with this command bus.
     */
    CommandStore.StatusService getCommandStatusService() {
        return commandStatusService;
    }

    private Optional<CommandDispatcher> getDispatcher(CommandClass commandClass) {
        return registry().getDispatcher(commandClass);
    }

    /**
     * Directs the command to be dispatched.
     *
     * <p>If the command has scheduling attributes, it will be posted for execution by
     * the configured scheduler according to values of those scheduling attributes.
     *
     * <p>If a command does not have a dispatcher, the error is
     * {@linkplain StreamObserver#onError(Throwable) returned} with
     * {@link UnsupportedCommandException} as the cause.
     *
     * @param command the command to be processed
     * @param responseObserver the observer to return the result of the call
     */
    @Override
    public void post(Command command, StreamObserver<Response> responseObserver) {
        checkNotNull(command);
        checkNotNull(responseObserver);
        checkArgument(isNotDefault(command));

        final CommandEnvelope commandEnvelope = CommandEnvelope.of(command);
        final CommandClass commandClass = commandEnvelope.getMessageClass();

        final Optional<CommandDispatcher> dispatcher = getDispatcher(commandClass);

        // If the command is not supported, return as error.
        if (!dispatcher.isPresent()) {
            handleDeadMessage(commandEnvelope, responseObserver);
            return;
        }

        if (!filter.handleValidation(command, responseObserver)) {
            return;
        }

        if (isScheduled(command)) {
            scheduleAndStore(command, responseObserver);
            return;
        }

        commandStore.store(command);
        responseObserver.onNext(Responses.ok());
        doPost(commandEnvelope, dispatcher.get());
        responseObserver.onCompleted();
    }

    @Override
    public void handleDeadMessage(CommandEnvelope message,
                                  StreamObserver<Response> responseObserver) {
        Command command = message.getCommand();
        final CommandException unsupported = new UnsupportedCommandException(command);
        commandStore.storeWithError(command, unsupported);
        responseObserver.onError(Statuses.invalidArgumentWithCause(unsupported));
    }

    /**
     * Passes a previously scheduled command to the corresponding dispatcher.
     */
    void postPreviouslyScheduled(Command command) {
        final CommandEnvelope commandEnvelope = CommandEnvelope.of(command);
        final Optional<CommandDispatcher> dispatcher = getDispatcher(
                commandEnvelope.getMessageClass()
        );
        if (!dispatcher.isPresent()) {
            throw noDispatcherFound(commandEnvelope);
        }
        doPost(commandEnvelope, dispatcher.get());
    }

    private static IllegalStateException noDispatcherFound(CommandEnvelope commandEnvelope) {
        final String idStr = Identifiers.idToString(commandEnvelope.getCommandId());
        final String msg = format("No dispatcher found for the command (class: %s id: %s).",
                                  commandEnvelope.getMessageClass()
                                                 .getClassName(),
                                  idStr);
        throw new IllegalStateException(msg);
    }

    private void scheduleAndStore(Command command, StreamObserver<Response> responseObserver) {
        scheduler.schedule(command);
        commandStore.store(command, SCHEDULED);
        responseObserver.onNext(Responses.ok());
        responseObserver.onCompleted();
    }

    /**
     * Directs a command to be dispatched.
     */
    void doPost(CommandEnvelope commandEnvelope, CommandDispatcher dispatcher) {
        try {
            dispatcher.dispatch(commandEnvelope);
            commandStatusService.setOk(commandEnvelope);
        } catch (RuntimeException e) {
            final Throwable cause = getRootCause(e);
            commandStatusService.updateCommandStatus(commandEnvelope, cause);
        }
    }

    /**
     * Ensures the {@code CommandBus} has a {@code TenantRepository} in multi-tenant context.
     *
     * @throws IllegalStateException if the repository is not set
     */
    private TenantIndex tenantIndex() {
        checkState(tenantIndex != null,
                   "TenantIndex is not initialized in multi-tenant context.");
        return tenantIndex;
    }

    /**
     * Closes the instance, preventing any for further posting of commands.
     *
     * <p>The following operations are performed:
     * <ol>
     *     <li>All command dispatchers are un-registered.
     *     <li>{@code CommandStore} is closed.
     *     <li>{@code CommandScheduler} is shut down.
     * </ol>
     *
     * @throws Exception if closing the {@code CommandStore} cases an exception
     */
    @Override
    public void close() throws Exception {
        registry().unregisterAll();
        commandStore.close();
        scheduler.shutdown();
        failureBus.close();
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

    Set<TenantId> getAllTenants() {
        final Set<TenantId> result = tenantIndex().getAll();
        return result;
    }

    /**
     * The {@code Builder} for {@code CommandBus}.
     */
    public static class Builder {

        private boolean multitenant;

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

        private FailureBus failureBus;

        private TenantIndex tenantIndex;

        /**
         * Checks whether the manual {@link Thread} spawning is allowed within
         * the current runtime environment.
         */
        private static boolean detectThreadsAllowed() {
            final boolean appEngine = Environment.getInstance()
                                                 .isAppEngine();
            return !appEngine;
        }

        public boolean isMultitenant() {
            return multitenant;
        }

        public boolean isThreadSpawnAllowed() {
            return threadSpawnAllowed;
        }

        public CommandStore getCommandStore() {
            return commandStore;
        }

        public Optional<CommandScheduler> commandScheduler() {
            return Optional.fromNullable(commandScheduler);
        }

        public Optional<FailureBus> failureBus() {
            return Optional.fromNullable(failureBus);
        }

        public Optional<TenantIndex> tenantIndex() {
            return Optional.fromNullable(tenantIndex);
        }

        public Builder setMultitenant(boolean multitenant) {
            this.multitenant = multitenant;
            return this;
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

        public Builder setFailureBus(FailureBus failureBus) {
            checkNotNull(failureBus);
            this.failureBus = failureBus;
            return this;
        }

        /**
         * Sets the tenant index for the bus to build.
         *
         * @param tenantIndex the tenant index or
         *                    {@code null} if the {@code CommandBus} to build is single-tenant
         */
        public Builder setTenantIndex(@Nullable TenantIndex tenantIndex) {
            if (this.multitenant) {
                checkNotNull(tenantIndex,
                             "TenantIndex cannot be null in multi-tenant CommandBus");
            }
            this.tenantIndex = tenantIndex;
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
         * based on current {@link Environment}.
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
         * If set the builder will not call {@link CommandBus#rescheduleCommands()}.
         *
         * <p>One of the applications of this flag is to disable rescheduling of commands in tests.
         */
        @VisibleForTesting
        Builder setAutoReschedule(boolean autoReschedule) {
            this.autoReschedule = autoReschedule;
            return this;
        }

        private Builder() {
            // Do not allow creating builder instances directly.
        }

        /**
         * Builds an instance of {@link CommandBus}.
         */
        public CommandBus build() {
            checkState(commandStore != null,
                       "CommandStore must be set. Please call CommandBus.Builder.setCommandStore()."
            );

            if (commandScheduler == null) {
                commandScheduler = new ExecutorCommandScheduler();
            }

            if (log == null) {
                log = new Log();
            }

            if (failureBus == null) {
                failureBus = FailureBus.newBuilder()
                                       .build();
            }

            if (multitenant) {
                checkState(tenantIndex != null,
                           "TenantRepository must be set for multi-tenant CommandBus.");
            } else {
                tenantIndex = TenantIndex.Factory.singleTenant();
            }

            final CommandBus commandBus = new CommandBus(this);

            if (commandScheduler.getCommandBus() == null) {
                commandScheduler.setCommandBus(commandBus);
            }

            if (autoReschedule) {
                commandBus.rescheduleCommands();
            }

            return commandBus;
        }
    }
}
