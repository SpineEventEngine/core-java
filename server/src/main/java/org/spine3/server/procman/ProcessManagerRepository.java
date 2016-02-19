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

import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.Events;
import org.spine3.protobuf.MessageField;
import org.spine3.server.BoundedContext;
import org.spine3.server.CommandBus;
import org.spine3.server.CommandDispatcher;
import org.spine3.server.EntityRepository;
import org.spine3.server.EventDispatcher;
import org.spine3.server.procman.error.MissingProcessManagerIdException;
import org.spine3.server.procman.error.NoIdExtractorException;
import org.spine3.type.CommandClass;
import org.spine3.type.EventClass;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.base.Commands.getMessage;
import static org.spine3.base.Identifiers.ID_PROPERTY_SUFFIX;

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
                          implements CommandDispatcher, EventDispatcher {

    /**
     * {@inheritDoc}
     */
    protected ProcessManagerRepository(BoundedContext boundedContext) {
        super(boundedContext);
    }

    @Override
    public Set<CommandClass> getCommandClasses() {
        final Class<? extends ProcessManager> pmClass = getEntityClass();
        final Set<Class<? extends Message>> commandClasses = ProcessManager.getHandledCommandClasses(pmClass);
        final Set<CommandClass> result = CommandClass.setOf(commandClasses);
        return result;
    }

    @Override
    public Set<EventClass> getEventClasses() {
        final Class<? extends ProcessManager> pmClass = getEntityClass();
        final Set<Class<? extends Message>> eventClasses = ProcessManager.getHandledEventClasses(pmClass);
        final Set<EventClass> result = EventClass.setOf(eventClasses);
        return result;
    }

    /**
     * Dispatches the command to a corresponding process manager.
     *
     * <p>If there is no stored process manager with such an ID, a new process manager is created
     * and stored after it handles the passed command.
     *
     * @param request a request to dispatch
     * @see ProcessManager#dispatchCommand(Message, CommandContext)
     * @throws InvocationTargetException if an exception occurs during command dispatching
     * @throws NoIdExtractorException if no {@link IdExtractor} found for this type of command message
     */
    @Override
    public List<Event> dispatch(Command request)
            throws InvocationTargetException, IllegalStateException, NoIdExtractorException {
        final Message command = getMessage(checkNotNull(request));
        final CommandContext context = request.getContext();
        final I id = getId(command, context);
        final PM manager = load(id);
        final List<Event> events = manager.dispatchCommand(command, context);
        store(manager);
        return events;
    }

    /**
     * Dispatches the event to a corresponding process manager.
     *
     * <p>If there is no stored process manager with such an ID, a new process manager is created
     * and stored after it handles the passed event.
     *
     * @param event the event to dispatch
     * @see ProcessManager#dispatchEvent(Message, EventContext)
     */
    @Override
    public void dispatch(Event event) throws NoIdExtractorException {
        final Message eventMessage = Events.getMessage(event);
        final EventContext context = event.getContext();
        final I id = getId(eventMessage, context);
        final PM manager = load(id);
        try {
            manager.dispatchEvent(eventMessage, context);
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

    /**
     * Obtains a process manager ID from event/command message and context.
     *
     * @param message an event or command message to extract an ID from
     * @param context either {@link EventContext} or {@link CommandContext} instance
     * @throws NoIdExtractorException if no {@link IdExtractor} found for this type of message
     */
    private I getId(Message message, Message context) throws NoIdExtractorException {
        final IdExtractor idExtractor = getIdExtractor(message.getClass());
        if (idExtractor == null) {
            throw new NoIdExtractorException(message.getClass());
        }
        // All id extractors are supposed to return IDs of this type.
        @SuppressWarnings("unchecked")
        final I result = (I) idExtractor.extract(message, context);
        return result;
    }

    /**
     * Returns extractor which can extract an ID from a message of the passed class.
     *
     * @param messageClass any class of event/command messages handled by the process manager
     * @return an ID extractor or {@code null} if no extractor for such message type exists
     */
    @Nullable
    protected abstract IdExtractor<? extends Message, ? extends Message> getIdExtractor(Class<? extends Message> messageClass);

    /**
     * Extracts a process manager ID from event/command message and context.
     *
     * @param <M> the type of event or command message to extract ID from
     * @param <C> either {@link EventContext} or {@link CommandContext} type
     */
    protected abstract class IdExtractor<M extends Message, C extends Message> {

        /**
         * Extracts a process manager ID from event/command message and context.
         *
         * @param message an event or command message to extract an ID from
         * @param context either {@link EventContext} or {@link CommandContext} instance
         * @return a process manager ID based on the input parameters
         */
        protected abstract I extract(M message, C context);
    }

    /**
     * Extracts a process manager ID from event/command message and context based on the passed field index.
     *
     * <p>A process manager ID field name must end with {@code "id"} suffix.
     *
     * @param <M> the type of event or command message to extract ID from
     * @param <C> either {@link EventContext} or {@link CommandContext} type
     */
    protected class IdFieldByIndexExtractor<M extends Message, C extends Message> extends IdExtractor<M, C> {

        private final ProcessManagerIdField idField;

        /**
         * Creates a new instance.
         *
         * @param idIndex the index of ID field in this type of messages
         */
        public IdFieldByIndexExtractor(int idIndex) {
            this.idField = new ProcessManagerIdField(idIndex);
        }

        @Override
        protected I extract(M message, C context) {
            @SuppressWarnings("unchecked") // we expect that the field is of this type
            final I id = (I) idField.getValue(message);
            return id;
        }

        /**
         * Accessor for process manager ID fields.
         */
        private class ProcessManagerIdField extends MessageField {

            private ProcessManagerIdField(int index) {
                super(index);
            }

            @Override
            protected RuntimeException createUnavailableFieldException(Message message, String fieldName) {
                return new MissingProcessManagerIdException(message.getClass().getName(), fieldName, getIndex());
            }

            @Override
            protected boolean isFieldAvailable(Message message) {
                final String fieldName = MessageField.getFieldName(message, getIndex());
                final boolean result = fieldName.endsWith(ID_PROPERTY_SUFFIX);
                return result;
            }
        }
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
