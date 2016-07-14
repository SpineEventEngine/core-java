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

package org.spine3.server.procman;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.Events;
import org.spine3.server.BoundedContext;
import org.spine3.server.command.CommandBus;
import org.spine3.server.command.CommandDispatcher;
import org.spine3.server.entity.EntityEventDispatcher;
import org.spine3.server.entity.EntityRepository;
import org.spine3.server.entity.GetTargetIdFromCommand;
import org.spine3.server.entity.IdFunction;
import org.spine3.server.event.EventBus;
import org.spine3.server.type.CommandClass;
import org.spine3.server.type.EventClass;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.base.Commands.getMessage;

/**
 * The abstract base for Process Managers repositories.
 *
 * @param <I> the type of IDs of process managers
 * @param <PM> the type of process managers
 * @param <S> the type of process manager state messages
 * @see ProcessManager
 * @author Alexander Litus
 */
public abstract class ProcessManagerRepository<I, PM extends ProcessManager<I, S>, S extends Message>
                          extends EntityRepository<I, PM, S>
                          implements CommandDispatcher, EntityEventDispatcher<I> {

    private final GetTargetIdFromCommand<I, Message> getIdFromCommandMessage = GetTargetIdFromCommand.newInstance();

    @Nullable
    private ImmutableSet<CommandClass> commandClasses;

    @Nullable
    private ImmutableSet<EventClass> eventClasses;

    /** {@inheritDoc} */
    protected ProcessManagerRepository(BoundedContext boundedContext) {
        super(boundedContext);
    }

    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // it is immutable
    public Set<CommandClass> getCommandClasses() {
        if (commandClasses == null) {
            final Class<? extends ProcessManager> pmClass = getEntityClass();
            final Set<Class<? extends Message>> classes = ProcessManager.getHandledCommandClasses(pmClass);
            commandClasses = CommandClass.setOf(classes);
        }
        return commandClasses;
    }

    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // it is immutable
    public Set<EventClass> getEventClasses() {
        if (eventClasses == null) {
            final Class<? extends ProcessManager> pmClass = getEntityClass();
            final Set<Class<? extends Message>> classes = ProcessManager.getHandledEventClasses(pmClass);
            eventClasses = EventClass.setOf(classes);
        }
        return eventClasses;
    }

    /**
     * Dispatches the command to a corresponding process manager.
     *
     * <p>If there is no stored process manager with such an ID, a new process manager is created
     * and stored after it handles the passed command.
     *
     * @param command a request to dispatch
     * @see ProcessManager#dispatchCommand(Message, CommandContext)
     * @throws InvocationTargetException if an exception occurs during command dispatching
     * @throws IllegalStateException if no command handler method found for a command
     * @throws IllegalArgumentException if commands of this type are not handled by the process manager
     */
    @Override
    public void dispatch(Command command)
            throws InvocationTargetException, IllegalStateException, IllegalArgumentException {
        final Message commandMessage = getMessage(checkNotNull(command));
        final CommandContext context = command.getContext();
        final CommandClass commandClass = CommandClass.of(commandMessage);
        checkCommandClass(commandClass);
        final I id = getIdFromCommandMessage.getId(commandMessage, context);
        final PM manager = load(id);
        final List<Event> events = manager.dispatchCommand(commandMessage, context);
        store(manager);
        postEvents(events);
    }

    /** Posts passed events to {@link EventBus}. */
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
     * @throws IllegalArgumentException if events of this type are not handled by the process manager
     * @see ProcessManager#dispatchEvent(Message, EventContext)
     */
    @Override
    public void dispatch(Event event) throws IllegalArgumentException {
        final Message message = Events.getMessage(event);
        final EventContext context = event.getContext();
        final EventClass eventClass = EventClass.of(message);
        checkEventClass(eventClass);
        final IdFunction idFunction = getIdFunction(eventClass);
        // All ID functions are supposed to return IDs of this type.
        @SuppressWarnings("unchecked")
        final I id = (I) idFunction.getId(message, context);
        final PM manager = load(id);
        try {
            manager.dispatchEvent(message, context);
            store(manager);
        } catch (InvocationTargetException e) {
            log().error("Error during dispatching event", e);
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
    @Nonnull
    @Override
    public PM load(I id) {
        PM result = super.load(id);
        if (result == null) {
            result = create(id);
        }
        final CommandBus commandBus = getBoundedContext().getCommandBus();
        result.setCommandBus(commandBus);
        return result;
    }

    private void checkCommandClass(CommandClass commandClass) throws IllegalArgumentException {
        final Set<CommandClass> classes = getCommandClasses();
        checkArgument(classes.contains(commandClass), "Unexpected command of class: " + commandClass.value().getName());
    }

    private void checkEventClass(EventClass eventClass) throws IllegalArgumentException {
        final Set<EventClass> classes = getEventClasses();
        checkArgument(classes.contains(eventClass), "Unexpected event of class: " + eventClass.value()
                                                                                              .getName());
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(ProcessManagerRepository.class);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }
}
