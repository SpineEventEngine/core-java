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

package io.spine.server.procman;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import io.spine.core.CommandClass;
import io.spine.core.CommandEnvelope;
import io.spine.core.Event;
import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import io.spine.core.RejectionClass;
import io.spine.core.RejectionEnvelope;
import io.spine.server.BoundedContext;
import io.spine.server.command.CommandHandlingEntity;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.commandbus.CommandDispatcherDelegate;
import io.spine.server.commandbus.DelegatingCommandDispatcher;
import io.spine.server.entity.EventDispatchingRepository;
import io.spine.server.event.EventBus;
import io.spine.server.rejection.DelegatingRejectionDispatcher;
import io.spine.server.rejection.RejectionDispatcherDelegate;
import io.spine.server.route.CommandRouting;
import io.spine.server.route.EventProducers;
import io.spine.server.route.RejectionProducers;
import io.spine.server.route.RejectionRouting;
import io.spine.server.tenant.TenantAwareFunction0;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

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

    /** Cached set of command classes handled by process managers of this repository. */
    @Nullable
    private Set<CommandClass> commandClasses;

    /** Cached set of event classes to which process managers of this repository are subscribed. */
    @Nullable
    private Set<EventClass> eventClasses;

    /** Cached set of rejection classes to which process managers are subscribed. */
    @Nullable
    private Set<RejectionClass> rejectionClasses;

    /** The rejection routing schema used by this repository. */
    private final RejectionRouting<I> rejectionRouting =
            RejectionRouting.withDefault(RejectionProducers.<I>fromContext());

    /** {@inheritDoc} */
    protected ProcessManagerRepository() {
        super(EventProducers.<I>fromFirstMessageField());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Registers with the {@code CommandBus} for dispatching commands
     * (via {@linkplain DelegatingCommandDispatcher delegating dispatcher}).
     */
    @Override
    public void onRegistered() {
        super.onRegistered();
        final BoundedContext boundedContext = getBoundedContext();

        final DelegatingCommandDispatcher<I> commandDispatcher =
                DelegatingCommandDispatcher.of(this);

        if (!commandDispatcher.getMessageClasses()
                              .isEmpty()) {
            boundedContext.getCommandBus()
                          .register(commandDispatcher);
        }

        final DelegatingRejectionDispatcher<I> rejectionDispatcher =
                DelegatingRejectionDispatcher.of(this);
        if (!rejectionDispatcher.getMessageClasses()
                                .isEmpty()) {
            boundedContext.getRejectionBus()
                          .register(rejectionDispatcher);
        }
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
        if (eventClasses == null) {
            final Class<? extends ProcessManager> pmClass = getEntityClass();
            eventClasses = ProcessManager.TypeInfo.getEventClasses(pmClass);
        }
        return eventClasses;
    }

    /**
     * Obtains a set of classes of commands handled by process managers of this repository.
     *
     * @return a set of command classes or empty set if process managers do not handle commands
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // it is immutable
    public Set<CommandClass> getCommandClasses() {
        if (commandClasses == null) {
            commandClasses = ProcessManager.TypeInfo.getCommandClasses(getEntityClass());
        }
        return commandClasses;
    }

    /**
     * Obtains a set of rejection classes on which process managers of
     * this repository are subscribed.
     *
     * @return a set of event classes or empty set if process managers
     * are not subscribed to rejections
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // it is immutable
    public Set<RejectionClass> getRejectionClasses() {
        if (rejectionClasses == null) {
            rejectionClasses = ProcessManager.TypeInfo.getRejectionClasses(getEntityClass());
        }
        return rejectionClasses;
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
        final I id = getCommandRouting().apply(command.getMessage(), command.getCommandContext());

        final TenantAwareFunction0<List<Event>> op =
                new TenantAwareFunction0<List<Event>>(command.getTenantId()) {
                    @Override
                    public List<Event> apply() {
                        final P manager = findOrCreate(id);

                        final ProcManTransaction<?, ?, ?> tx = beginTransactionFor(manager);
                        final List<Event> events = manager.dispatchCommand(command);
                        store(manager);
                        tx.commit();
                        return events;
                    }
                };
        final List<Event> events = op.execute();
        postEvents(events);
        return id;
    }

    /**
     * Dispatches the event to a corresponding process manager.
     *
     * <p>If there is no stored process manager with such an ID, a new process manager is created
     * and stored after it handles the passed event.
     *
     * @param event the event to dispatch
     * @throws IllegalArgumentException if events of this type are not handled by
     *                                  this process manager
     * @see ProcessManager#dispatchEvent(EventEnvelope)
     */
    @Override
    public Set<I> dispatch(EventEnvelope event) throws IllegalArgumentException {
        checkEventClass(event.getMessageClass());
        return super.dispatch(event);
    }

    /**
     * Dispatches the rejection to one or more subscribing process managers.
     *
     * @param rejection the rejection to dispatch
     * @return IDs of process managers who successfully consumed the rejection
     */
    @Override
    public Set<I> dispatchRejection(final RejectionEnvelope rejection) {
        final Set<I> targets = getRejectionRouting().apply(rejection.getMessage(),
                                                           rejection.getRejectionContext());
        final TenantAwareFunction0<Set<I>> op =
                new TenantAwareFunction0<Set<I>>(rejection.getTenantId()) {
                    @Override
                    public Set<I> apply() {
                        final ImmutableSet.Builder<I> consumed = ImmutableSet.builder();
                        for (I id : targets) {
                            try {
                                ProcessManager<I, ?, ?> processManager = findOrCreate(id);
                                processManager.dispatchRejection(rejection);
                                consumed.add(id);
                            } catch (RuntimeException exception) {
                                onError(rejection, exception);
                                // Do not re-throw: let other subscribers to consume the rejection.
                            }
                        }
                        return consumed.build();
                    }
                };
        final Set<I> consumed = op.execute();
        return consumed;
    }

    @Override
    protected void dispatchToEntity(I id, EventEnvelope event) {
        final P manager = findOrCreate(id);
        final ProcManTransaction<?, ?, ?> transaction = beginTransactionFor(manager);
        manager.dispatchEvent(event);
        transaction.commit();
        store(manager);
    }

    @Override
    public void onError(CommandEnvelope envelope, RuntimeException exception) {
        logError("Command dispatching caused error (class: %s, id: %s)", envelope, exception);
    }

    @Override
    public void onError(RejectionEnvelope envelope, RuntimeException exception) {
        logError("Rejection dispatching caused error (class: %s, id: %s", envelope, exception);
    }

    private ProcManTransaction<?, ?, ?> beginTransactionFor(P manager) {
        return ProcManTransaction.start((ProcessManager<?, ?, ?>) manager);
    }

    /**
     * Posts passed events to {@link EventBus}.
     */
    private void postEvents(Iterable<Event> events) {
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

    private void checkEventClass(EventClass eventClass) throws IllegalArgumentException {
        final Set<EventClass> classes = getMessageClasses();
        if (!classes.contains(eventClass)) {
            throw Error.unexpectedEventEncountered(eventClass);
        }
    }
}
