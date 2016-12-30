/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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
import com.google.common.collect.Sets;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import org.spine3.Internal;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandId;
import org.spine3.base.Error;
import org.spine3.base.Errors;
import org.spine3.base.FailureThrowable;
import org.spine3.base.Response;
import org.spine3.base.Responses;
import org.spine3.server.BoundedContext;
import org.spine3.server.Statuses;
import org.spine3.server.command.error.CommandException;
import org.spine3.server.command.error.UnsupportedCommandException;
import org.spine3.server.type.CommandClass;
import org.spine3.server.users.CurrentTenant;
import org.spine3.util.Environment;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.base.CommandStatus.SCHEDULED;
import static org.spine3.base.Commands.getId;
import static org.spine3.base.Commands.getMessage;
import static org.spine3.base.Commands.isScheduled;

/**
 * Dispatches the incoming commands to the corresponding handler.
 *
 * @author Alexander Yevsyukov
 * @author Mikhail Melnik
 * @author Alexander Litus
 * @author Alex Tymchenko
 */
public class CommandBus implements AutoCloseable {

    private final Filter filter;

    private final DispatcherRegistry dispatcherRegistry = new DispatcherRegistry();

    private final HandlerRegistry handlerRegistry = new HandlerRegistry();

    private final CommandStore commandStore;

    private final CommandStatusService commandStatusService;

    private final CommandScheduler scheduler;

    private final Rescheduler rescheduler;

    private final Log log;

    /**
     * Is true, if the {@code BoundedContext} (to which this {@code CommandBus} belongs) is multi-tenant.
     *
     * <p>If the {@code CommandBus} is multi-tenant, the commands posted must have the {@code tenant_id} attribute
     * defined.
     */
    private boolean isMultitenant;

    /**
     * Determines whether the manual thread spawning is allowed within current runtime environment.
     *
     * <p>If set to {@code true}, {@code CommandBus} will be running some of internal processing in parallel
     * to improve performance.
     */
    private final boolean isThreadSpawnAllowed;

    /**
     * Creates new instance according to the passed {@link Builder}.
     */
    private CommandBus(Builder builder) {
        this(builder.getCommandStore(),
             builder.getCommandScheduler(),
             builder.log,
             builder.isThreadSpawnAllowed());
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

    /**
     * Creates a new instance.
     *
     * @param commandStore       a store to save commands
     * @param scheduler          a command scheduler
     * @param log                a problem logger
     * @param threadSpawnAllowed whether the current runtime environment allows manual thread spawn
     */
    @SuppressWarnings("ThisEscapedInObjectConstruction") // is OK because helper instances only store the reference.
    private CommandBus(CommandStore commandStore,
                             CommandScheduler scheduler,
                             Log log,
                             boolean threadSpawnAllowed) {
        this.commandStore = checkNotNull(commandStore);
        this.commandStatusService = new CommandStatusService(commandStore);
        this.scheduler = checkNotNull(scheduler);
        this.log = checkNotNull(log);
        this.isThreadSpawnAllowed = threadSpawnAllowed;
        this.filter = new Filter(this);
        this.rescheduler = new Rescheduler(this);
    }

    /**
     * Registers the passed command dispatcher.
     *
     * @param dispatcher the dispatcher to register
     * @throws IllegalArgumentException if {@link CommandDispatcher#getCommandClasses()} returns empty set
     */
    public void register(CommandDispatcher dispatcher) {
        handlerRegistry.checkNoHandlersRegisteredForCommandsOf(dispatcher);
        dispatcherRegistry.register(dispatcher);
    }

    /**
     * Unregisters dispatching for command classes by the passed dispatcher.
     *
     * <p>If the passed dispatcher deals with commands for which another dispatcher already registered
     * the dispatch entries for such commands will not be unregistered, and warning will be logged.
     *
     * @param dispatcher the dispatcher to unregister
     */
    public void unregister(CommandDispatcher dispatcher) {
        dispatcherRegistry.unregister(dispatcher);
    }

    /**
     * Registers the passed command handler.
     *
     * @param handler a {@code non-null} handler object
     * @throws IllegalArgumentException if the handler does not have command handling methods
     */
    public void register(CommandHandler handler) {
        dispatcherRegistry.checkNoDispatchersRegisteredForCommandsOf(handler);
        handlerRegistry.register(handler);
    }

    /**
     * Unregisters the command handler from the command bus.
     *
     * @param handler the handler to unregister
     */
    public void unregister(CommandHandler handler) {
        handlerRegistry.unregister(handler);
    }

    /**
     * Directs the command to be dispatched or handled.
     *
     * <p>If the command has scheduling attributes, it will be posted for execution by the configured
     * scheduler according to values of those scheduling attributes.
     *
     * <p>If a command does not have neither dispatcher nor handler, the error is returned via
     * {@link StreamObserver#onError(Throwable)} call with {@link UnsupportedCommandException} as the cause.
     *
     * @param command the command to be processed
     * @param responseObserver the observer to return the result of the call
     */
    public void post(Command command, StreamObserver<Response> responseObserver) {
        final CommandClass commandClass = CommandClass.of(command);

        // If the command is not supported, return as error.
        if (!isSupportedCommand(commandClass)) {
            handleUnsupported(command, responseObserver);
            return;
        }
        if (!filter.handleValidation(command, responseObserver)) {
            return;
        }
        if (isScheduled(command)) {
            scheduleAndStore(command, responseObserver);
            return;
        }
        if (isMultitenant) {
            CurrentTenant.set(command.getContext()
                                     .getTenantId());
        }
        commandStore.store(command);
        responseObserver.onNext(Responses.ok());
        doPost(command);
        responseObserver.onCompleted();
    }

    /**
     * Directs a command to be dispatched or handled.
     *
     * <p>Logs exceptions which may occur during dispatching or handling and
     * sets the command status to {@code error} or {@code failure} in the storage.
     *
     * @param command a command to post
     */
    /* package */ void doPost(Command command) {
        final Message message = getMessage(command);
        final CommandClass commandClass = CommandClass.of(message);
        final CommandContext commandContext = command.getContext();
        if (dispatcherRegistry.hasDispatcherFor(commandClass)) {
            dispatch(command);
        } else if (handlerRegistry.handlerRegistered(commandClass)) {
            invokeHandler(message, commandContext);
        }
    }

    private void handleUnsupported(Command command, StreamObserver<Response> responseObserver) {
        final CommandException unsupported = new UnsupportedCommandException(command);
        commandStore.storeWithError(command, unsupported);
        responseObserver.onError(Statuses.invalidArgumentWithCause(unsupported));
    }

    private void scheduleAndStore(Command command, StreamObserver<Response> responseObserver) {
        scheduler.schedule(command);
        commandStore.store(command, SCHEDULED);
        responseObserver.onNext(Responses.ok());
        responseObserver.onCompleted();
    }

    /**
     * Checks if a command is supported by the Command Bus.
     *
     * @param commandClass a class of commands to check
     * @return {@code true} if there is a {@link CommandDispatcher} or a {@link CommandHandler} registered
     *      for commands of this type, {@code false} otherwise
     */
    @VisibleForTesting
    /* package */ boolean isSupportedCommand(CommandClass commandClass) {
        final boolean dispatcherRegistered = dispatcherRegistry.hasDispatcherFor(commandClass);
        final boolean handlerRegistered = handlerRegistry.handlerRegistered(commandClass);
        final boolean isSupported = dispatcherRegistered || handlerRegistered;
        return isSupported;
    }

    /**
     * Obtains the view {@code Set} of commands that are known to this {@code CommandBus}.
     *
     * <p>This set is changed when command dispatchers or handlers are registered or un-registered.
     *
     * @return a set of classes of supported commands
     */
    public Set<CommandClass> getSupportedCommandClasses() {
        final Set<CommandClass> result = Sets.union(dispatcherRegistry.getCommandClasses(),
                                                    handlerRegistry.getCommandClasses());
        return result;
    }

    /**
     * Obtains the instance of the {@link CommandStatusService} associated with this command bus.
     */
    public CommandStatusService getCommandStatusService() {
        return commandStatusService;
    }

    /* package */ boolean isMultitenant() {
        return isMultitenant;
    }

    /* package */ boolean isThreadSpawnAllowed() {
        return isThreadSpawnAllowed;
    }

    /* package */ CommandStore commandStore() {
        return commandStore;
    }

    /* package */ Log problemLog() {
        return log;
    }

    @VisibleForTesting
    /* package */ Rescheduler rescheduler() {
        return rescheduler;
    }

    @VisibleForTesting
    /* package */ CommandScheduler scheduler() {
        return scheduler;
    }

    private void dispatch(Command command) {
        final CommandClass commandClass = CommandClass.of(command);
        final CommandDispatcher dispatcher = dispatcherRegistry.getDispatcher(commandClass);
        final CommandId commandId = getId(command);
        try {
            dispatcher.dispatch(command);
        } catch (Exception e) {
            log.errorDispatching(e, command);
            commandStatusService.setToError(commandId, e);
        }
    }

    private void invokeHandler(Message msg, CommandContext context) {
        final CommandClass commandClass = CommandClass.of(msg);
        final CommandHandler handler = handlerRegistry.getHandler(commandClass);
        final CommandId commandId = context.getCommandId();
        try {
            handler.handle(msg, context);
            commandStatusService.setOk(commandId);
        } catch (InvocationTargetException e) {
            final Throwable cause = e.getCause();
            onHandlerException(msg, commandId, cause);
        } catch (RuntimeException e) {
            onHandlerException(msg, commandId, e);
        }
    }

    private void onHandlerException(Message msg, CommandId commandId, Throwable cause) {
        //noinspection ChainOfInstanceofChecks
        if (cause instanceof Exception) {
            final Exception exception = (Exception) cause;
            log.errorHandling(exception, msg, commandId);
            commandStatusService.setToError(commandId, exception);
        } else if (cause instanceof FailureThrowable){
            final FailureThrowable failure = (FailureThrowable) cause;
            log.failureHandling(failure, msg, commandId);
            commandStatusService.setToFailure(commandId, failure);
        } else {
            log.errorHandlingUnknown(cause, msg, commandId);
            final Error error = Errors.fromThrowable(cause);
            commandStatusService.setToError(commandId, error);
        }
    }

    /**
     * Sets the multitenancy status of the {@link CommandBus}.
     *
     * <p>A {@link CommandBus} is multi-tenant if its {@link BoundedContext} is multi-tenant.
     */
    @Internal
    public void setMultitenant(boolean isMultitenant) {
        this.isMultitenant = isMultitenant;
    }

    @Override
    public void close() throws Exception {
        dispatcherRegistry.unregisterAll();
        handlerRegistry.unregisterAll();
        commandStore.close();
        scheduler.shutdown();
    }

    /**
     * The {@code Builder} for {@code CommandBus}.
     */
    public static class Builder {

        private CommandStore commandStore;

        private Log log;

        /**
         * Optional field for the {@code CommandBus}.
         *
         * <p>If unset, the default {@link ExecutorCommandScheduler} implementation is used.
         */
        private CommandScheduler commandScheduler;

        /**
         * If set to {@code true}, the {@code CommandBus} will be creating instances of {@link Thread} for operation.
         *
         * <p>However, some runtime environments, such as Google AppEngine Standard, do not allow manual thread
         * spawning. In this case, this flag should be set to {@code false}.
         *
         * <p>The default value of this flag is set upon the best guess, based on current {@link Environment}.
         */
        private boolean threadSpawnAllowed = detectThreadsAllowed();

        /**
         * If set the builder will not call {@link CommandBus#rescheduleCommands()}.
         *
         * <p>One of the applications of this flag is to disable rescheduling of commands in tests.
         */
        private boolean autoReschedule;

        /**
         * Checks whether the manual {@link Thread} spawning is allowed withing the current runtime environment.
         */
        private static boolean detectThreadsAllowed() {
            final boolean appEngine = Environment.getInstance()
                                                 .isAppEngine();
            return !appEngine;
        }

        public CommandStore getCommandStore() {
            return commandStore;
        }

        public CommandScheduler getCommandScheduler() {
            return commandScheduler;
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

        public boolean isThreadSpawnAllowed() {
            return threadSpawnAllowed;
        }

        public Builder setThreadSpawnAllowed(boolean threadSpawnAllowed) {
            this.threadSpawnAllowed = threadSpawnAllowed;
            return this;
        }

        @VisibleForTesting
        /* package */ Builder setLog(Log log) {
            this.log = log;
            return this;
        }

        @VisibleForTesting
        /* package */ Builder setAutoReschedule(boolean autoReschedule) {
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
            checkNotNull(commandStore, "CommandStore must be set");

            if(commandScheduler == null) {
                commandScheduler = new ExecutorCommandScheduler();
            }

            if (log == null) {
                log = new Log();
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
