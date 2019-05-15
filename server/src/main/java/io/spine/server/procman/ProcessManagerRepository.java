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

package io.spine.server.procman;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.core.Event;
import io.spine.server.BoundedContext;
import io.spine.server.command.CommandErrorHandler;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.commandbus.CommandDispatcherDelegate;
import io.spine.server.commandbus.DelegatingCommandDispatcher;
import io.spine.server.entity.EntityLifecycle;
import io.spine.server.entity.EntityLifecycleMonitor;
import io.spine.server.entity.EventDispatchingRepository;
import io.spine.server.entity.TransactionListener;
import io.spine.server.event.EventBus;
import io.spine.server.integration.ExternalMessageClass;
import io.spine.server.integration.ExternalMessageDispatcher;
import io.spine.server.integration.ExternalMessageEnvelope;
import io.spine.server.procman.model.ProcessManagerClass;
import io.spine.server.route.CommandRouting;
import io.spine.server.route.EventRoute;
import io.spine.server.type.CommandClass;
import io.spine.server.type.CommandEnvelope;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.option.EntityOption.Kind.PROCESS_MANAGER;
import static io.spine.server.procman.model.ProcessManagerClass.asProcessManagerClass;
import static io.spine.server.tenant.TenantAwareRunner.with;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * The abstract base for Process Managers repositories.
 *
 * @param <I> the type of IDs of process managers
 * @param <P> the type of process managers
 * @param <S> the type of process manager state messages
 * @see ProcessManager
 */
public abstract class ProcessManagerRepository<I,
                                               P extends ProcessManager<I, S, ?>,
                                               S extends Message>
                extends EventDispatchingRepository<I, P, S>
                implements CommandDispatcherDelegate<I> {

    /** The command routing schema used by this repository. */
    private final CommandRouting<I> commandRouting = CommandRouting.newInstance();

    /**
     * The {@link CommandErrorHandler} tackling the dispatching errors.
     *
     * <p>This field is not {@code final} only because it is initialized in {@link #onRegistered()}
     * method.
     */
    private @MonotonicNonNull CommandErrorHandler commandErrorHandler;

    /**
     * The configurable lifecycle rules of the repository.
     *
     * <p>The rules allow to automatically mark entities as archived/deleted upon certain event and
     * rejection types emitted.
     *
     * @see LifecycleRules#archiveOn(Class[])
     * @see LifecycleRules#deleteOn(Class[])
     */
    private final LifecycleRules lifecycleRules = new LifecycleRules();

    /**
     * Creates a new instance with the event routing by the first message field.
     */
    protected ProcessManagerRepository() {
        super(EventRoute.byFirstMessageField());
    }

    /**
     * Obtains class information of process managers managed by this repository.
     */
    private ProcessManagerClass<P> processManagerClass() {
        return (ProcessManagerClass<P>) entityModelClass();
    }

    @Internal
    @Override
    protected final ProcessManagerClass<P> toModelClass(Class<P> cls) {
        return asProcessManagerClass(cls);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Registers with the {@code CommandBus} for dispatching commands
     * (via {@linkplain DelegatingCommandDispatcher delegating dispatcher}).
     *
     * <p>Registers with the {@code IntegrationBus} for dispatching external events and rejections.
     *
     * <p>Ensures there is at least one handler method declared by the class of the managed
     * process manager:
     *
     * <ul>
     *     <li>command handler methods;
     *     <li>domestic or external event reactor methods;
     *     <li>domestic or external rejection reactor methods;
     *     <li>commanding method.
     * </ul>
     *
     * <p>Throws an {@code IllegalStateException} otherwise.
     */
    @Override
    public void onRegistered() {
        super.onRegistered();

        BoundedContext boundedContext = boundedContext();
        boundedContext.registerCommandDispatcher(this);

        checkNotDeaf();

        this.commandErrorHandler = boundedContext.createCommandErrorHandler();
        PmSystemEventWatcher<I> systemSubscriber = new PmSystemEventWatcher<>(this);
        systemSubscriber.registerIn(boundedContext);
    }

    /**
     * Ensures the process manager class handles at least one type of messages.
     */
    private void checkNotDeaf() {
        boolean dispatchesEvents = dispatchesEvents() || dispatchesExternalEvents();

        if (!dispatchesCommands() && !dispatchesEvents) {
            throw newIllegalStateException(
                    "Process managers of the repository %s have no command handlers, " +
                            "and do not react to any events.", this);
        }
    }

    /**
     * Obtains a set of event classes to which process managers of this repository react.
     *
     * @return a set of event classes or empty set if process managers do not react to
     *         domestic events
     */
    @Override
    public Set<EventClass> messageClasses() {
        return processManagerClass().domesticEvents();
    }

    /**
     * Obtains classes of external events to which the process managers managed by this repository
     * react.
     *
     * @return a set of event classes or an empty set if process managers do not react to
     *         external events
     */
    @Override
    public Set<EventClass> externalEventClasses() {
        return processManagerClass().externalEvents();
    }

    /**
     * Obtains a set of classes of commands handled by process managers of this repository.
     *
     * @return a set of command classes or empty set if process managers do not handle commands
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // it is immutable
    public Set<CommandClass> commandClasses() {
        return processManagerClass().commands();
    }

    /**
     * Obtains command routing schema used by this repository.
     */
    protected final CommandRouting<I> commandRouting() {
        return commandRouting;
    }

    /**
     * Obtains configurable lifecycle rules of this repository.
     *
     * <p>The rules allow to automatically archive/delete entities upon certain event and rejection
     * types produced.
     *
     * <p>The rules can be set as follows:
     * <pre>{@code
     *   repository.lifecycle()
     *             .archiveOn(Event1.class, Rejection1.class)
     *             .deleteOn(Rejection2.class)
     * }</pre>
     */
    public final LifecycleRules lifecycle() {
        return lifecycleRules;
    }

    @Override
    public ImmutableSet<EventClass> outgoingEvents() {
        Set<EventClass> eventClasses = processManagerClass().outgoingEvents();
        return ImmutableSet.copyOf(eventClasses);
    }

    /**
     * Dispatches the command to a corresponding process manager.
     *
     * <p>If there is no stored process manager with such an ID,
     * a new process manager is created and stored after it handles the passed command.
     *
     * @param command a request to dispatch
     */
    @Override
    public I dispatchCommand(CommandEnvelope command) {
        checkNotNull(command);
        I target = with(command.tenantId()).evaluate(() -> doDispatch(command));
        return target;
    }

    private I doDispatch(CommandEnvelope command) {
        I target = route(command);
        lifecycleOf(target).onDispatchCommand(command.command());
        return target;
    }

    private I route(CommandEnvelope cmd) {
        CommandRouting<I> routing = commandRouting();
        I target = routing.apply(cmd.message(), cmd.context());
        lifecycleOf(target).onTargetAssignedToCommand(cmd.id());
        return target;
    }

    /**
     * Dispatches the given command to the {@link ProcessManager} with the given ID.
     *
     * @param id
     *         the target entity ID
     * @param command
     *         the command to dispatch
     */
    void dispatchNowTo(I id, CommandEnvelope command) {
        PmCommandEndpoint<I, P> endpoint = PmCommandEndpoint.of(this, command);
        endpoint.dispatchTo(id);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Sends a system command to dispatch the given event to a reactor.
     */
    @Override
    protected final void dispatchTo(I id, Event event) {
        lifecycleOf(id).onDispatchEventToReactor(event);
    }

    /**
     * Dispatches the given event to the {@link ProcessManager} with the given ID.
     *
     * @param id
     *         the target entity ID
     * @param event
     *         the event to dispatch
     */
    void dispatchNowTo(I id, EventEnvelope event) {
        PmEventEndpoint<I, P> endpoint = PmEventEndpoint.of(this, event);
        endpoint.dispatchTo(id);
    }

    @Override
    public void onError(CommandEnvelope cmd, RuntimeException exception) {
        commandErrorHandler.handle(cmd, exception, event -> postEvents(ImmutableList.of(event)));
    }

    @SuppressWarnings("unchecked")   // to avoid massive generic-related issues.
    @VisibleForTesting
    protected PmTransaction<?, ?, ?> beginTransactionFor(P manager) {
        PmTransaction<I, S, ?> tx =
                PmTransaction.start((ProcessManager<I, S, ?>) manager, lifecycle());
        TransactionListener listener = EntityLifecycleMonitor.newInstance(this);
        tx.setListener(listener);
        return tx;
    }

    /**
     * Posts passed events to {@link EventBus}.
     */
    void postEvents(Collection<Event> events) {
        Iterable<Event> filteredEvents = eventFilter().filter(events);
        EventBus bus = boundedContext().eventBus();
        bus.post(filteredEvents);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overridden to expose the method into current package.
     */
    @Override
    protected EntityLifecycle lifecycleOf(I id) {
        return super.lifecycleOf(id);
    }

    /**
     * Loads or creates a process manager by the passed ID.
     *
     * <p>The process manager is created if there was no manager with such ID stored before.
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
        CommandBus commandBus = boundedContext().commandBus();
        result.setCommandBus(commandBus);
        return result;
    }

    @Override
    public P create(I id) {
        P procman = super.create(id);
        lifecycleOf(id).onEntityCreated(PROCESS_MANAGER);
        return procman;
    }

    @Override
    public Optional<ExternalMessageDispatcher<I>> createExternalDispatcher() {
        if (!dispatchesExternalEvents()) {
            return Optional.empty();
        }
        return Optional.of(new PmExternalEventDispatcher());
    }

    /**
     * An implementation of an external message dispatcher feeding external events
     * to {@code ProcessManager} instances.
     */
    private class PmExternalEventDispatcher extends AbstractExternalEventDispatcher {

        @Override
        public Set<ExternalMessageClass> messageClasses() {
            ProcessManagerClass<?> pmClass = asProcessManagerClass(entityClass());
            Set<EventClass> eventClasses = pmClass.externalEvents();
            return ExternalMessageClass.fromEventClasses(eventClasses);
        }

        @Override
        public void onError(ExternalMessageEnvelope envelope, RuntimeException exception) {
            checkNotNull(envelope);
            checkNotNull(exception);
            logError("Error dispatching external event (class: `%s`, id: `%s`) " +
                             "to a process manager with state `%s`.",
                     envelope, exception);
        }
    }
}
