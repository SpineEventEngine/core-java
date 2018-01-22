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

package io.spine.server.procman;

import com.google.protobuf.Message;
import io.spine.annotation.SPI;
import io.spine.core.CommandClass;
import io.spine.core.CommandEnvelope;
import io.spine.core.Event;
import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import io.spine.core.RejectionClass;
import io.spine.core.RejectionEnvelope;
import io.spine.server.BoundedContext;
import io.spine.server.bus.Bus;
import io.spine.server.bus.MessageDispatcher;
import io.spine.server.command.CommandHandlingEntity;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.commandbus.CommandDispatcherDelegate;
import io.spine.server.commandbus.CommandErrorHandler;
import io.spine.server.commandbus.DelegatingCommandDispatcher;
import io.spine.server.entity.EventDispatchingRepository;
import io.spine.server.event.EventBus;
import io.spine.server.integration.ExternalMessageClass;
import io.spine.server.integration.ExternalMessageDispatcher;
import io.spine.server.integration.ExternalMessageEnvelope;
import io.spine.server.model.Model;
import io.spine.server.rejection.DelegatingRejectionDispatcher;
import io.spine.server.rejection.RejectionDispatcherDelegate;
import io.spine.server.route.CommandRouting;
import io.spine.server.route.EventProducers;
import io.spine.server.route.EventRouting;
import io.spine.server.route.RejectionProducers;
import io.spine.server.route.RejectionRouting;

import javax.annotation.CheckReturnValue;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
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
                           RejectionDispatcherDelegate<I> {

    /** The command routing schema used by this repository. */
    private final CommandRouting<I> commandRouting = CommandRouting.newInstance();

    /** The rejection routing schema used by this repository. */
    private final RejectionRouting<I> rejectionRouting =
            RejectionRouting.withDefault(RejectionProducers.<I>fromContext());

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
    private ProcessManagerClass<P> processManagerClass() {
        return (ProcessManagerClass<P>) entityClass();
    }

    @SuppressWarnings("unchecked") // The cast is ensured by generic parameters of the repository.
    @Override
    protected final ProcessManagerClass<P> getModelClass(Class<P> cls) {
        return (ProcessManagerClass<P>) Model.getInstance()
                                             .asProcessManagerClass(cls);
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

        final BoundedContext boundedContext = getBoundedContext();
        final DelegatingRejectionDispatcher<I> rejDispatcher =
                DelegatingRejectionDispatcher.of(this);

        final boolean handlesCommands = register(boundedContext.getCommandBus(),
                                                 DelegatingCommandDispatcher.of(this));
        final boolean handlesDomesticRejections = register(boundedContext.getRejectionBus(),
                                                           rejDispatcher);
        final boolean handlesExternalRejections = register(boundedContext.getIntegrationBus(),
                                                           rejDispatcher.getExternalDispatcher());
        final boolean handlesDomesticEvents = !getMessageClasses().isEmpty();
        final boolean handlesExternalEvents = !getExternalEventDispatcher().getMessageClasses()
                                                                           .isEmpty();

        final boolean subscribesToEvents = handlesDomesticEvents || handlesExternalEvents;
        final boolean reactsUponRejections = handlesDomesticRejections || handlesExternalRejections;

        if (!handlesCommands && !subscribesToEvents && !reactsUponRejections) {
            throw newIllegalStateException(
                    "Process managers of the repository %s have no command handlers, " +
                            "and do not react upon any rejections or events.", this);
        }
        this.commandErrorHandler = CommandErrorHandler.with(boundedContext.getRejectionBus());
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
        final boolean hasHandlerMethods = !dispatcher.getMessageClasses()
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
     * Obtains a set of rejection classes on which process managers of
     * this repository are subscribed.
     *
     * @return a set of rejection classes or empty set if process managers
     * are not subscribed to rejections
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // it is immutable
    public Set<RejectionClass> getRejectionClasses() {
        return processManagerClass().getRejectionReactions();
    }

    /**
     * Obtains a set of external rejection classes on which process managers of
     * this repository are subscribed.
     *
     * @return a set of external rejection classes or empty set if process managers
     * are not subscribed to external rejections
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // it is immutable
    public Set<RejectionClass> getExternalRejectionClasses() {
        return processManagerClass().getExternalRejectionReactions();
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
    public I dispatchCommand(final CommandEnvelope command) {
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

    /**
     * Dispatches the rejection to one or more subscribing process managers.
     *
     * @param rejection the rejection to dispatch
     * @return IDs of process managers who successfully consumed the rejection
     */
    @Override
    public Set<I> dispatchRejection(final RejectionEnvelope rejection) {
        return PmRejectionEndpoint.handle(this, rejection);
    }

    @Override
    public void onError(CommandEnvelope envelope, RuntimeException exception) {
        commandErrorHandler.handleError(envelope, exception);
    }

    @Override
    public void onError(RejectionEnvelope envelope, RuntimeException exception) {
        logError("Rejection dispatching caused error (class: %s, id: %s", envelope, exception);
    }

    PmTransaction<?, ?, ?> beginTransactionFor(P manager) {
        return PmTransaction.start((ProcessManager<?, ?, ?>) manager);
    }

    /**
     * Posts passed events to {@link EventBus}.
     */
    void postEvents(Iterable<Event> events) {
        final EventBus eventBus = getBoundedContext().getEventBus();
        for (Event event : events) {
            eventBus.post(event);
        }
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
    @CheckReturnValue
    protected P findOrCreate(I id) {
        final P result = super.findOrCreate(id);
        final CommandBus commandBus = getBoundedContext().getCommandBus();
        result.setCommandBus(commandBus);
        return result;
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
        return PmEventDelivery.directDelivery(this);
    }


    /**
     * Defines a strategy of rejection delivery applied to the instances managed by this repository.
     *
     * <p>By default uses direct delivery.
     *
     * <p>Descendants may override this method to redefine the strategy. In particular,
     * it is possible to postpone dispatching of a certain rejection to a particular process manager
     * instance at runtime.
     *
     * @return delivery strategy for rejections
     */
    @SPI
    protected PmRejectionDelivery<I, P> getRejectionEndpointDelivery() {
        return PmRejectionDelivery.directDelivery(this);
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
        return PmCommandDelivery.directDelivery(this);
    }

    @Override
    protected ExternalMessageDispatcher<I> getExternalEventDispatcher() {
        return new PmExternalEventDispatcher();
    }

    /**
     * An implementation of an external message dispatcher feeding external events
     * to {@code ProcessManager} instances.
     */
    private class PmExternalEventDispatcher extends AbstractExternalEventDispatcher {

        @Override
        public Set<ExternalMessageClass> getMessageClasses() {
            final ProcessManagerClass<?> pmClass = Model.getInstance()
                                                        .asProcessManagerClass(getEntityClass());
            final Set<EventClass> eventClasses = pmClass.getExternalEventReactions();
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
