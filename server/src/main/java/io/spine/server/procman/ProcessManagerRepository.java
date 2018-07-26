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

package io.spine.server.procman;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import io.spine.annotation.SPI;
import io.spine.core.BoundedContextName;
import io.spine.core.Command;
import io.spine.core.CommandClass;
import io.spine.core.CommandEnvelope;
import io.spine.core.Event;
import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import io.spine.server.BoundedContext;
import io.spine.server.ServerEnvironment;
import io.spine.server.bus.Bus;
import io.spine.server.bus.MessageDispatcher;
import io.spine.server.command.CommandHandlingEntity;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.commandbus.CommandDispatcherDelegate;
import io.spine.server.commandbus.CommandErrorHandler;
import io.spine.server.commandbus.DelegatingCommandDispatcher;
import io.spine.server.delivery.Shardable;
import io.spine.server.delivery.ShardedStreamConsumer;
import io.spine.server.delivery.ShardingStrategy;
import io.spine.server.delivery.UniformAcrossTargets;
import io.spine.server.entity.EntityLifecycleMonitor;
import io.spine.server.entity.EventDispatchingRepository;
import io.spine.server.entity.TransactionListener;
import io.spine.server.event.EventBus;
import io.spine.server.integration.ExternalMessageClass;
import io.spine.server.integration.ExternalMessageDispatcher;
import io.spine.server.integration.ExternalMessageEnvelope;
import io.spine.server.model.Model;
import io.spine.server.procman.model.ProcessManagerClass;
import io.spine.server.route.CommandRouting;
import io.spine.server.route.EventProducers;
import io.spine.server.route.EventRouting;
import io.spine.server.route.RejectionProducers;
import io.spine.server.route.RejectionRouting;
import io.spine.system.server.SystemGateway;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Suppliers.memoize;
import static io.spine.option.EntityOption.Kind.PROCESS_MANAGER;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * The abstract base for Process Managers repositories.
 *
 * @param <I> the type of IDs of process managers
 * @param <P> the type of process managers
 * @param <S> the type of process manager state messages
 * @see ProcessManager
 * @author Alexander Litus
 * @author Alexander Yevsyukov
 */
public abstract class ProcessManagerRepository<I,
                                               P extends ProcessManager<I, S, ?>,
                                               S extends Message>
                extends EventDispatchingRepository<I, P, S>
                implements CommandDispatcherDelegate<I>,
                           Shardable {

    /** The command routing schema used by this repository. */
    private final CommandRouting<I> commandRouting = CommandRouting.newInstance();

    /** The rejection routing schema used by this repository. */
    private final RejectionRouting<I> rejectionRouting =
            RejectionRouting.withDefault(RejectionProducers.fromContext());

    private final Supplier<PmCommandDelivery<I, P>> commandDeliverySupplier =
            memoize(() -> {
                PmCommandDelivery<I, P> result = new PmCommandDelivery<>(this);
                return result;
            });

    private final Supplier<PmEventDelivery<I, P>> eventDeliverySupplier =
            memoize(() -> {
                PmEventDelivery<I, P> result = new PmEventDelivery<>(this);
                return result;
            });

    /**
     * The {@link CommandErrorHandler} tackling the dispatching errors.
     *
     * <p>This field is not {@code final} only because it is initialized in {@link #onRegistered()}
     * method.
     */
    private CommandErrorHandler commandErrorHandler;

    /**
     * Creates a new instance with the event routing by the first message field.
     */
    protected ProcessManagerRepository() {
        super(EventProducers.<I>fromFirstMessageField());
    }

    /** Obtains class information of process managers managed by this repository. */
    @SuppressWarnings("unchecked") // The cast is ensured by generic parameters of the repository.
    ProcessManagerClass<P> processManagerClass() {
        return (ProcessManagerClass<P>) entityClass();
    }

    @SuppressWarnings("unchecked") // The cast is ensured by generic parameters of the repository.
    @Override
    protected final ProcessManagerClass<P> getModelClass(Class<P> cls) {
        return (ProcessManagerClass<P>) Model.getInstance()
                                             .asProcessManagerClass(cls);
    }

    @Override
    public ProcessManagerClass<P> getShardedModelClass() {
        return processManagerClass();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Registers with the {@code CommandBus} for dispatching commands
     * (via {@linkplain DelegatingCommandDispatcher delegating dispatcher}).
     *
     * <p>Registers with the {@code IntegrationBus} for dispatching external events and rejections.
     *
     * <p>Ensures there is at least one message handler declared by the class of the managed
     * process manager:
     *
     * <ul>
     *     <li>command handler methods;</li>
     *     <li>domestic or external event reactor methods;</li>
     *     <li>domestic or external rejection reactor methods.</li>
     * </ul>
     *
     * <p>Throws an {@code IllegalStateException} otherwise.
     */
    @SuppressWarnings("MethodWithMoreThanThreeNegations")   // It's fine, as reflects the logic.
    @Override
    public void onRegistered() {
        super.onRegistered();

        BoundedContext boundedContext = getBoundedContext();
        boolean handlesCommands = register(boundedContext.getCommandBus(),
                                           DelegatingCommandDispatcher.of(this));
        boolean handlesDomesticEvents = !getMessageClasses().isEmpty();
        boolean handlesExternalEvents = !getExternalEventDispatcher().getMessageClasses()
                                                                     .isEmpty();

        boolean subscribesToEvents = handlesDomesticEvents || handlesExternalEvents;

        if (!handlesCommands && !subscribesToEvents) {
            throw newIllegalStateException(
                    "Process managers of the repository %s have no command handlers, " +
                            "and do not react upon any rejections or events.", this);
        }
        SystemGateway systemGateway = boundedContext.getSystemGateway();
        this.commandErrorHandler = CommandErrorHandler.with(systemGateway);
        ServerEnvironment.getInstance()
                         .getSharding()
                         .register(this);
    }

    /**
     * Registers the given dispatcher in the bus if there is at least one message class declared
     * for dispatching by the dispatcher.
     *
     * @param bus        the bus to register dispatchers in
     * @param dispatcher the dispatcher to register
     * @param <D>        the type of dispatcher
     * @return {@code true} if there are message classes to dispatch by the given dispatchers,
     *         {@code false} otherwise
     */
    @SuppressWarnings("unchecked")  // To avoid a long "train" of generic parameter definitions.
    private static <D extends MessageDispatcher<?, ?, ?>> boolean register(Bus<?, ?, ?, D> bus,
                                                                           D dispatcher) {
        boolean hasHandlerMethods = !dispatcher.getMessageClasses()
                                               .isEmpty();
        if (hasHandlerMethods) {
            bus.register(dispatcher);
        }
        return hasHandlerMethods;
    }

    /**
     * Registers itself as {@link io.spine.server.event.EventDispatcher EventDispatcher} if
     * process managers of this repository are subscribed at least to one event.
     */
    @Override
    protected void registerAsEventDispatcher() {
        if (!getMessageClasses().isEmpty()) {
            super.registerAsEventDispatcher();
        }
    }

    /**
     * Obtains a set of event classes on which process managers of this repository are subscribed.
     *
     * @return a set of event classes or empty set if process managers do not subscribe to events
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // it is immutable
    public Set<EventClass> getMessageClasses() {
        return processManagerClass().getEventReactions();
    }

    /**
     * Obtains a set of classes of commands handled by process managers of this repository.
     *
     * @return a set of command classes or empty set if process managers do not handle commands
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // it is immutable
    public Set<CommandClass> getCommandClasses() {
        return processManagerClass().getCommands();
    }

    /**
     * Obtains command routing schema used by this repository.
     */
    protected final CommandRouting<I> getCommandRouting() {
        return commandRouting;
    }

    /**
     * Obtains rejection routing schema used by this repository.
     */
    protected final RejectionRouting<I> getRejectionRouting() {
        return rejectionRouting;
    }

    /**
     * Dispatches the command to a corresponding process manager.
     *
     * <p>If there is no stored process manager with such an ID,
     * a new process manager is created and stored after it handles the passed command.
     *
     * @param command a request to dispatch
     * @see CommandHandlingEntity#dispatchCommand(CommandEnvelope)
     */
    @Override
    public I dispatchCommand(CommandEnvelope command) {
        checkNotNull(command);
        return PmCommandEndpoint.handle(this, command);
    }

    /**
     * Dispatches the event to a corresponding process manager.
     *
     * <p>If there is no stored process manager with such an ID, a new process manager is created
     * and stored after it handles the passed event.
     *
     * @param event the event to dispatch
     * @see ProcessManager#dispatchEvent(EventEnvelope)
     */
    @Override
    public Set<I> dispatch(EventEnvelope event) {
        Set<I> result = PmEventEndpoint.handle(this, event);
        return result;
    }

    @Override
    public void onError(CommandEnvelope envelope, RuntimeException exception) {
        commandErrorHandler.handleError(envelope, exception)
                           .rethrow();
    }

    @SuppressWarnings("unchecked")   // to avoid massive generic-related issues.
    PmTransaction<?, ?, ?> beginTransactionFor(P manager) {
        PmTransaction<I, S, ?> tx = PmTransaction.start((ProcessManager<I, S, ?>) manager);
        TransactionListener listener = EntityLifecycleMonitor.newInstance(this);
        tx.setListener(listener);
        return tx;
    }

    /**
     * Posts passed events to {@link EventBus}.
     */
    void postEvents(Iterable<Event> events) {
        EventBus eventBus = getBoundedContext().getEventBus();
        for (Event event : events) {
            eventBus.post(event);
        }
    }

    void onDispatchCommand(I id, Command command) {
        lifecycleOf(id).onDispatchCommand(command);
    }

    void onCommandHandled(I id, Command command) {
        lifecycleOf(id).onCommandHandled(command);
    }

    void onDispatchEvent(I id, Event event) {
        lifecycleOf(id).onDispatchEventToReactor(event);
    }

    /**
     * Loads or creates a process manager by the passed ID.
     *
     * <p>The process manager is created if there was no manager with such an ID stored before.
     *
     * <p>The repository injects {@code CommandBus} from its {@code BoundedContext} into the
     * instance of the process manager so that it can post commands if needed.
     *
     * @param id the ID of the process manager to load
     * @return loaded or created process manager instance
     */
    @Override
    protected P findOrCreate(I id) {
        P result = super.findOrCreate(id);
        CommandBus commandBus = getBoundedContext().getCommandBus();
        result.setCommandBus(commandBus);
        return result;
    }

    @Override
    public P create(I id) {
        P procman = super.create(id);
        lifecycleOf(id).onEntityCreated(PROCESS_MANAGER);
        return procman;
    }

    /** Open access to the event routing to the package. */
    EventRouting<I> eventRouting() {
        return getEventRouting();
    }

    /**
     * Defines a strategy of event delivery applied to the instances managed by this repository.
     *
     * <p>By default uses direct delivery.
     *
     * <p>Descendants may override this method to redefine the strategy. In particular,
     * it is possible to postpone dispatching of a certain event to a particular process manager
     * instance at runtime.
     *
     * @return delivery strategy for events applied to the instances managed by this repository
     */
    @SPI
    protected PmEventDelivery<I, P> getEventEndpointDelivery() {
        return eventDeliverySupplier.get();
    }

    /**
     * Defines a strategy of command delivery applied to the instances managed by this repository.
     *
     * <p>By default uses direct delivery.
     *
     * <p>Descendants may override this method to redefine the strategy. In particular,
     * it is possible to postpone dispatching of a certain command to a particular process manager
     * instance at runtime.
     *
     * @return delivery strategy for rejections
     */
    @SPI
    protected PmCommandDelivery<I, P> getCommandEndpointDelivery() {
        return commandDeliverySupplier.get();
    }

    @Override
    protected ExternalMessageDispatcher<I> getExternalEventDispatcher() {
        return new PmExternalEventDispatcher();
    }

    @Override
    public ShardingStrategy getShardingStrategy() {
        return UniformAcrossTargets.singleShard();
    }

    @Override
    public Iterable<ShardedStreamConsumer<?, ?>> getMessageConsumers() {
        Iterable<ShardedStreamConsumer<?, ?>> result =
                ImmutableList.of(
                        getCommandEndpointDelivery().getConsumer(),
                        getEventEndpointDelivery().getConsumer()
                );
        return result;
    }

    @Override
    public BoundedContextName getBoundedContextName() {
        BoundedContextName name = getBoundedContext().getName();
        return name;
    }

    @Override
    public void close() {
        ServerEnvironment.getInstance()
                         .getSharding()
                         .unregister(this);
        super.close();
    }

    /**
     * An implementation of an external message dispatcher feeding external events
     * to {@code ProcessManager} instances.
     */
    private class PmExternalEventDispatcher extends AbstractExternalEventDispatcher {

        @Override
        public Set<ExternalMessageClass> getMessageClasses() {
            ProcessManagerClass<?> pmClass = Model.getInstance()
                                                  .asProcessManagerClass(getEntityClass());
            Set<EventClass> eventClasses = pmClass.getExternalEventReactions();
            return ExternalMessageClass.fromEventClasses(eventClasses);
        }

        @Override
        public void onError(ExternalMessageEnvelope envelope, RuntimeException exception) {
            checkNotNull(envelope);
            checkNotNull(exception);
            logError("Error dispatching external event to process manager" +
                             " (class: %s, id: %s)",
                     envelope, exception);
        }
    }
}
