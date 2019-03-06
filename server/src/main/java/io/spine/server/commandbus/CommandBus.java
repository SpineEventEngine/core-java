/*
 * Copyright 2019, TeamDev. All rights reserved.
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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.concurrent.LazyInit;
import io.grpc.stub.StreamObserver;
import io.spine.annotation.Internal;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.core.Commands;
import io.spine.core.Event;
import io.spine.core.TenantId;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.bus.BusBuilder;
import io.spine.server.bus.BusFilter;
import io.spine.server.bus.DeadMessageHandler;
import io.spine.server.bus.EnvelopeValidator;
import io.spine.server.bus.UnicastBus;
import io.spine.server.command.CommandErrorHandler;
import io.spine.server.event.EventBus;
import io.spine.server.event.RejectionEnvelope;
import io.spine.server.tenant.TenantIndex;
import io.spine.server.type.CommandClass;
import io.spine.server.type.CommandEnvelope;
import io.spine.system.server.SystemWriteSide;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Deque;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newLinkedList;
import static io.spine.server.bus.BusBuilder.FieldCheck.checkSet;
import static io.spine.server.bus.BusBuilder.FieldCheck.systemNotSet;
import static io.spine.server.bus.BusBuilder.FieldCheck.tenantIndexNotSet;
import static io.spine.system.server.WriteSideFunction.delegatingTo;
import static java.util.Optional.ofNullable;

/**
 * Dispatches the incoming commands to the corresponding handler.
 *
 * @author Alexander Yevsyukov
 * @author Mikhail Melnik
 * @author Alexander Litus
 * @author Alex Tymchenko
 * @author Dmytro Dashenkov
 */
public class CommandBus extends UnicastBus<Command,
                                           CommandEnvelope,
                                           CommandClass,
                                           CommandDispatcher<?>> {

    private final CommandScheduler scheduler;
    private final EventBus eventBus;
    private final SystemWriteSide systemWriteSide;
    private final TenantIndex tenantIndex;
    private final CommandErrorHandler errorHandler;
    private final CommandFlowWatcher flowWatcher;

    /**
     * Is {@code true}, if the {@code BoundedContext} (to which this {@code CommandBus} belongs)
     * is multi-tenant.
     *
     * <p>If the {@code CommandBus} is multi-tenant, the commands posted must have the
     * {@code tenant_id} attribute defined.
     */
    private final boolean multitenant;

    private final DeadCommandHandler deadCommandHandler;

    /**
     * Tha validator for the commands posted into this bus.
     *
     * <p>The value is effectively final, though should be initialized lazily.
     *
     * @see #validator() to getreive the non-null value of the validator
     */
    @LazyInit
    private @MonotonicNonNull CommandValidator commandValidator;

    /**
     * Creates new instance according to the passed {@link Builder}.
     */
    private CommandBus(Builder builder) {
        super(builder);
        this.multitenant = builder.multitenant != null
                           ? builder.multitenant
                           : false;
        this.scheduler = builder.commandScheduler;
        this.eventBus = builder.eventBus;
        this.systemWriteSide = builder.system()
                                      .orElseThrow(systemNotSet());
        this.tenantIndex = builder.tenantIndex()
                                  .orElseThrow(tenantIndexNotSet());
        this.deadCommandHandler = new DeadCommandHandler();
        this.errorHandler = CommandErrorHandler.with(systemWriteSide);
        this.flowWatcher = builder.flowWatcher;
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

    @VisibleForTesting
    CommandScheduler scheduler() {
        return scheduler;
    }

    @Override
    protected CommandDispatcherRegistry createRegistry() {
        return new CommandDispatcherRegistry();
    }

    @SuppressWarnings("ReturnOfCollectionOrArrayField") // OK for a protected factory method
    @Override
    protected Collection<BusFilter<CommandEnvelope>> filterChainTail() {
        return ImmutableList.of(scheduler);
    }

    @Override
    protected Collection<BusFilter<CommandEnvelope>> filterChainHead() {
        BusFilter<CommandEnvelope> tap = new CommandReceivedTap(this::systemFor);
        Deque<BusFilter<CommandEnvelope>> result = newLinkedList();
        result.push(tap);
        return result;
    }

    @Override
    protected CommandEnvelope toEnvelope(Command message) {
        return CommandEnvelope.of(message);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Wraps the {@code source} observer with the {@link CommandAckMonitor}.
     *
     * @return new instance of {@link CommandAckMonitor} with the given parameters
     */
    @Override
    protected StreamObserver<Ack> prepareObserver(Iterable<Command> commands,
                                                  StreamObserver<Ack> source) {
        StreamObserver<Ack> wrappedSource = super.prepareObserver(commands, source);
        TenantId tenant = tenantOf(commands);
        StreamObserver<Ack> result = CommandAckMonitor
                .newBuilder()
                .setDelegate(wrappedSource)
                .setTenantId(tenant)
                .setSystemWriteSide(systemWriteSide)
                .build();
        return result;
    }

    private static TenantId tenantOf(Iterable<Command> commands) {
        return Streams.stream(commands)
                      .map(Commands::getTenantId)
                      .findAny()
                      .orElse(TenantId.getDefaultInstance());
    }

    SystemWriteSide systemFor(TenantId tenantId) {
        checkNotNull(tenantId);
        SystemWriteSide result = delegatingTo(systemWriteSide).get(tenantId);
        return result;
    }

    @Override
    protected void dispatch(CommandEnvelope envelope) {
        CommandDispatcher<?> dispatcher = getDispatcher(envelope);
        flowWatcher.onDispatchCommand(envelope);
        try {
            dispatcher.dispatch(envelope);
        } catch (RuntimeException exception) {
            onError(envelope, exception);
        }
    }

    private void onError(CommandEnvelope envelope, RuntimeException exception) {
        Optional<Event> rejection = errorHandler.handle(envelope, exception)
                                                .asRejection()
                                                .map(RejectionEnvelope::outerObject);
        rejection.ifPresent(eventBus::post);
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

    @Override
    protected DeadMessageHandler<CommandEnvelope> deadMessageHandler() {
        return deadCommandHandler;
    }

    @Override
    protected EnvelopeValidator<CommandEnvelope> validator() {
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

    @Override
    protected void store(Iterable<Command> commands) {
        TenantId tenantId = tenantOf(commands);
        tenantIndex.keep(tenantId);
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
    public static class Builder extends BusBuilder<CommandEnvelope, Command, Builder> {

        /**
         * The multi-tenancy flag for the {@code CommandBus} to build.
         *
         * <p>The value of this field should be equal to that of corresponding
         * {@linkplain BoundedContextBuilder BoundedContext.Builder} and is not
         * supposed to be {@linkplain #setMultitenant(Boolean) set directly}.
         *
         * <p>If set directly, the value would be matched to the multi-tenancy flag of
         * {@code BoundedContext}.
         */
        private @Nullable Boolean multitenant;

        /**
         * Optional field for the {@code CommandBus}.
         *
         * <p>If unset, the default {@link ExecutorCommandScheduler} implementation is used.
         */
        private CommandScheduler commandScheduler;
        private EventBus eventBus;
        private CommandFlowWatcher flowWatcher;

        /** Prevents direct instantiation. */
        private Builder() {
            super();
        }

        @Internal
        public @Nullable Boolean isMultitenant() {
            return multitenant;
        }

        @Internal
        public Builder setMultitenant(@Nullable Boolean multitenant) {
            this.multitenant = multitenant;
            return this;
        }

        public Optional<CommandScheduler> getCommandScheduler() {
            return ofNullable(commandScheduler);
        }

        public Builder setCommandScheduler(CommandScheduler commandScheduler) {
            checkNotNull(commandScheduler);
            this.commandScheduler = commandScheduler;
            return this;
        }

        /**
         * Inject the {@link EventBus} of the bounded context to which the built bus belongs.
         *
         * <p>This method is {@link Internal} to the framework. The name of the method starts with
         * {@code inject} prefix so that this method does not appear in an autocomplete hint for
         * {@code set} prefix.
         */
        @Internal
        public Builder injectEventBus(EventBus eventBus) {
            checkNotNull(eventBus);
            this.eventBus = eventBus;
            return this;
        }

        Optional<EventBus> getEventBus() {
            return ofNullable(eventBus);
        }

        @Override
        protected void checkFieldsSet() {
            super.checkFieldsSet();
            checkSet(eventBus, EventBus.class, "injectEventBus");
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
            checkFieldsSet();

            if (commandScheduler == null) {
                commandScheduler = new ExecutorCommandScheduler();
            }
            flowWatcher = new CommandFlowWatcher((tenantId) -> {
                @SuppressWarnings("OptionalGetWithoutIsPresent") // ensured by checkFieldsSet()
                SystemWriteSide writeSide = system().get();
                SystemWriteSide result = delegatingTo(writeSide).get(tenantId);
                return result;
            });
            commandScheduler.setFlowWatcher(flowWatcher);

            CommandBus commandBus = createCommandBus();
            commandScheduler.setCommandBus(commandBus);

            return commandBus;
        }

        @Override
        @CheckReturnValue
        protected Builder self() {
            return this;
        }

        @CheckReturnValue
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
    private static class DeadCommandHandler implements DeadMessageHandler<CommandEnvelope> {

        @Override
        public UnsupportedCommandException handle(CommandEnvelope message) {
            Command command = message.command();
            UnsupportedCommandException exception = new UnsupportedCommandException(command);
            return exception;
        }
    }
}
