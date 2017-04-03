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

package org.spine3.server.procman;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Message;
import org.spine3.base.CommandContext;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.envelope.CommandEnvelope;
import org.spine3.envelope.EventEnvelope;
import org.spine3.server.BoundedContext;
import org.spine3.server.command.CommandBus;
import org.spine3.server.command.CommandDispatcherDelegate;
import org.spine3.server.entity.EventDispatchingRepository;
import org.spine3.server.entity.idfunc.GetTargetIdFromCommand;
import org.spine3.server.event.EventBus;
import org.spine3.type.CommandClass;
import org.spine3.type.EventClass;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

import static org.spine3.util.Exceptions.newIllegalArgumentException;

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
                                               P extends ProcessManager<I, S>,
                                               S extends Message>
                extends EventDispatchingRepository<I, P, S>
                implements CommandDispatcherDelegate {

    /** The {@code BoundedContext} in which this repository works. */
    private final BoundedContext boundedContext;

    private final GetTargetIdFromCommand<I, Message> getIdFromCommandMessage =
            GetTargetIdFromCommand.newInstance();

    @Nullable
    private Set<CommandClass> commandClasses;

    @Nullable
    private Set<EventClass> eventClasses;

    /** {@inheritDoc} */
    protected ProcessManagerRepository(BoundedContext boundedContext) {
        super(EventDispatchingRepository.<I>producerFromFirstMessageField());
        this.boundedContext = boundedContext;
    }

    /** Returns the {@link BoundedContext} in which this repository works. */
    private BoundedContext getBoundedContext() {
        return boundedContext;
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
     * @see ProcessManager#dispatchCommand(Message, CommandContext)
     */
    @Override
    public void dispatchCommand(CommandEnvelope envelope) {
        final Message commandMessage = envelope.getMessage();
        final CommandContext context = envelope.getCommandContext();
        final CommandClass commandClass = envelope.getMessageClass();
        checkCommandClass(commandClass);
        final I id = getIdFromCommandMessage.apply(commandMessage, context);
        final P manager = findOrCreate(id);
        final List<Event> events = manager.dispatchCommand(commandMessage, context);
        store(manager);
        postEvents(events);
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

    @VisibleForTesting
    void dispatch(Event event) throws IllegalArgumentException {
        dispatch(EventEnvelope.of(event));
    }

    @Override
    protected void dispatchToEntity(I id, Message eventMessage, EventContext context) {
        final P manager = findOrCreate(id);
        manager.dispatchEvent(eventMessage, context);
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
