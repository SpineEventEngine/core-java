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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.grpc.stub.StreamObserver;
import io.spine.annotation.Internal;
import io.spine.base.Command;
import io.spine.base.CommandClass;
import io.spine.base.FailureThrowable;
import io.spine.base.Identifier;
import io.spine.base.Response;
import io.spine.base.Responses;
import io.spine.envelope.CommandEnvelope;
import io.spine.io.StreamObservers;
import io.spine.server.Environment;
import io.spine.server.bus.Bus;
import io.spine.server.commandstore.CommandStore;
import io.spine.server.failure.FailureBus;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Throwables.getRootCause;
import static io.spine.validate.Validate.isNotDefault;
import static java.lang.String.format;

/**
 * Dispatches the incoming commands to the corresponding handler.
 *
 * @author Alexander Yevsyukov
 * @author Mikhail Melnik
 * @author Alexander Litus
 * @author Alex Tymchenko
 */
public class CommandBus extends Bus<Command, CommandEnvelope, CommandClass, CommandDispatcher> {

    private final CommandStore commandStore;

    private CommandBusFilter filterChain;

    private final CommandScheduler scheduler;

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
        this.failureBus = builder.failureBus;
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

    CommandStore commandStore() {
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
     * Exposes the {@code FailureBus} instance for this {@code CommandBus}.
     *
     * <p>This method is designed for internal use only. Client code should use
     * {@link io.spine.server.BoundedContext#getFailureBus() BoundedContext.getFailureBus()}
     * instead.
     */
    @Internal
    public FailureBus failureBus() {
        return this.failureBus;
    }

    private void setFilterChain(CommandBusFilter filterChain) {
        this.filterChain = filterChain;
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
        if (!filterChain.accept(commandEnvelope, responseObserver)) {
            return;
        }
        commandStore().store(command);
        responseObserver.onNext(Responses.ok());

        doPost(commandEnvelope);
        responseObserver.onCompleted();
    }

    /**
     * Does nothing because commands for which are no registered dispatchers
     * are rejected by a built-in {@link CommandBusFilter} invoked when such a command is
     * {@linkplain #post(Command, StreamObserver) posted} to the bus.
     */
    @Override
    public void handleDeadMessage(CommandEnvelope message,
                                  StreamObserver<Response> responseObserver) {
        // Do nothing because this is the responsibility of `DeadCommandFilter`.
        //TODO:2017-03-30:alexander.yevsyukov: Handle dead messages in other buses using filters
        // and remove this method from the interface.
    }

    /**
     * Passes a previously scheduled command to the corresponding dispatcher.
     */
    void postPreviouslyScheduled(Command command) {
        final CommandEnvelope commandEnvelope = CommandEnvelope.of(command);
        doPost(commandEnvelope);
    }

    private static IllegalStateException noDispatcherFound(CommandEnvelope commandEnvelope) {
        final String idStr = Identifier.toString(commandEnvelope.getCommandId());
        final String msg = format("No dispatcher found for the command (class: %s id: %s).",
                                  commandEnvelope.getMessageClass()
                                                 .getClassName(),
                                  idStr);
        throw new IllegalStateException(msg);
    }

    /**
     * Directs a command to be dispatched.
     */
    @SuppressWarnings("ConstantConditions")
        // OK to get without checking because the command was validated before this call.
    void doPost(CommandEnvelope commandEnvelope) {
        final CommandDispatcher dispatcher = getDispatcher(commandEnvelope);
        try {
            dispatcher.dispatch(commandEnvelope);
            commandStore.setCommandStatusOk(commandEnvelope);
        } catch (RuntimeException e) {
            final Throwable cause = getRootCause(e);
            commandStore.updateCommandStatus(commandEnvelope, cause, log);

            emitFailure(commandEnvelope, cause);
        }
    }

    /**
     * Emits the {@code Failure} and posts it to the {@code FailureBus},
     * if the given {@code cause} is in fact a {@code FailureThrowable} instance.
     */
    private void emitFailure(CommandEnvelope commandEnvelope, Throwable cause) {
        if (cause instanceof FailureThrowable) {
            final FailureThrowable failure = (FailureThrowable) cause;
            failureBus.post(failure.toFailure(commandEnvelope.getCommand()),
                            StreamObservers.<Response>noOpObserver());
        }
    }

    private CommandDispatcher getDispatcher(CommandEnvelope commandEnvelope) {
        final Optional<CommandDispatcher> dispatcher = getDispatcher(
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
        filterChain.onClose(this);
        commandStore.close();
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

    /**
     * The {@code Builder} for {@code CommandBus}.
     */
    public static class Builder {

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

        private FailureBus failureBus;

        private final List<CommandBusFilter> filters = Lists.newArrayList();

        /**
         * Checks whether the manual {@link Thread} spawning is allowed within
         * the current runtime environment.
         */
        private static boolean detectThreadsAllowed() {
            final boolean appEngine = Environment.getInstance()
                                                 .isAppEngine();
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

        public Optional<FailureBus> getFailureBus() {
            return Optional.fromNullable(failureBus);
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

        public Builder addFilter(CommandBusFilter filter) {
            checkNotNull(filter);
            filters.add(filter);
            return this;
        }

        public Builder removeFilter(CommandBusFilter filter) {
            checkNotNull(filter);
            filters.remove(filter);
            return this;
        }

        /**
         * Obtains immutable list of added filters.
         */
        public List<CommandBusFilter> getFilters() {
            return ImmutableList.copyOf(filters);
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
            // Do not allow creating builder instances directly.
        }

        /**
         * Builds an instance of {@link CommandBus}.
         *
         * <p>This method is supposed to be called internally when building an enclosing
         * {@code BoundedContext}.
         */
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

            if (failureBus == null) {
                failureBus = FailureBus.newBuilder()
                                       .build();
            }

            final CommandBus commandBus = createCommandBus();
            
            commandScheduler.setCommandBus(commandBus);

            if (autoReschedule) {
                commandBus.rescheduleCommands();
            }

            return commandBus;
        }

        private CommandBus createCommandBus() {
            final CommandBus commandBus = new CommandBus(this);

            // Enforce creating the registry to make spying for CommandBus-es in tests work.
            commandBus.registry();

            setFilterChain(commandBus);
            return commandBus;
        }

        private void setFilterChain(CommandBus commandBus) {
            final CommandBusFilter filterChain = FilterChain.newBuilder()
                                                            .setCommandBus(commandBus)
                                                            .addFilters(getFilters())
                                                            .setCommandScheduler(commandScheduler)
                                                            .build();
            commandBus.setFilterChain(filterChain);
        }
    }
}
