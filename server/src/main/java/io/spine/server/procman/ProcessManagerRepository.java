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

import com.google.protobuf.Message;
import io.spine.base.CommandClass;
import io.spine.base.CommandContext;
import io.spine.base.Event;
import io.spine.base.EventClass;
import io.spine.base.EventContext;
import io.spine.envelope.CommandEnvelope;
import io.spine.envelope.EventEnvelope;
import io.spine.server.command.CommandHandlingEntity;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.commandbus.CommandDispatcherDelegate;
import io.spine.server.commandbus.DelegatingCommandDispatcher;
import io.spine.server.entity.EventDispatchingRepository;
import io.spine.server.entity.idfunc.GetTargetIdFromCommand;
import io.spine.server.event.EventBus;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

import static io.spine.util.Exceptions.newIllegalArgumentException;

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
                implements CommandDispatcherDelegate {

    private final GetTargetIdFromCommand<I, Message> getIdFromCommandMessage =
            GetTargetIdFromCommand.newInstance();

    @Nullable
    private Set<CommandClass> commandClasses;

    @Nullable
    private Set<EventClass> eventClasses;

    /** {@inheritDoc} */
    protected ProcessManagerRepository() {
        super(EventDispatchingRepository.<I>producerFromFirstMessageField());
    }

    @Override
    public void onRegistered() {
        super.onRegistered();
        getBoundedContext().getCommandBus()
                           .register(DelegatingCommandDispatcher.of(this));
    }

    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // it is immutable
    public Set<CommandClass> getCommandClasses() {
        if (commandClasses == null) {
            commandClasses = ProcessManager.TypeInfo.getCommandClasses(getEntityClass());
        }
        return commandClasses;
    }

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
     * Dispatches the command to a corresponding process manager.
     *
     * <p>If there is no stored process manager with such an ID,
     * a new process manager is created and stored after it handles the passed command.
     *
     * @param envelope a request to dispatch
     * @see CommandHandlingEntity#dispatchCommand(CommandEnvelope)
     */
    @Override
    public void dispatchCommand(CommandEnvelope envelope) {
        final Message commandMessage = envelope.getMessage();
        final CommandContext context = envelope.getCommandContext();
        final CommandClass commandClass = envelope.getMessageClass();
        checkCommandClass(commandClass);
        final I id = getIdFromCommandMessage.apply(commandMessage, context);
        final P manager = findOrCreate(id);

        final ProcManTransaction<?, ?, ?> tx = beginTransactionFor(manager);
        final List<Event> events = manager.dispatchCommand(envelope);
        store(manager);
        tx.commit();

        postEvents(events);
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
     * Dispatches the event to a corresponding process manager.
     *
     * <p>If there is no stored process manager with such an ID, a new process manager is created
     * and stored after it handles the passed event.
     *
     * @param event the event to dispatch
     * @throws IllegalArgumentException if events of this type are not handled by
     *                                  this process manager
     * @see ProcessManager#dispatchEvent(Message, EventContext)
     */
    @Override
    public void dispatch(EventEnvelope event) throws IllegalArgumentException {
        checkEventClass(event);

        super.dispatch(event);
    }

    @Override
    protected void dispatchToEntity(I id, Message eventMessage, EventContext context) {
        final P manager = findOrCreate(id);
        final ProcManTransaction<?, ?, ?> transaction = beginTransactionFor(manager);
        manager.dispatchEvent(eventMessage, context);
        transaction.commit();
        store(manager);
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

    private void checkCommandClass(CommandClass commandClass) throws IllegalArgumentException {
        final Set<CommandClass> classes = getCommandClasses();
        if (!classes.contains(commandClass)) {
            final String eventClassName = commandClass.value()
                                                      .getName();
            throw newIllegalArgumentException("Unexpected command of class: %s", eventClassName);
        }
    }

    private void checkEventClass(EventEnvelope eventEnvelope) throws IllegalArgumentException {
        final EventClass eventClass = eventEnvelope.getMessageClass();

        final Set<EventClass> classes = getMessageClasses();
        if (!classes.contains(eventClass)) {
            final String eventClassName = eventClass.value()
                                                    .getName();
            throw newIllegalArgumentException("Unexpected event of class: %s", eventClassName);
        }
    }
}
