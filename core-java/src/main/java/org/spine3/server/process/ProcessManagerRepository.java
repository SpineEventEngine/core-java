/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.process;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.protobuf.Message;
import org.spine3.base.CommandContext;
import org.spine3.base.EventContext;
import org.spine3.base.EventRecord;
import org.spine3.server.EntityRepository;
import org.spine3.server.MultiHandler;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Throwables.propagate;

/**
 * The abstract base for repositories for Process Managers.
 *
 * @param <I> the type of IDs of process managers
 * @param <PM> the type of process managers
 * @param <M> the type of process manager state messages
 * @see ProcessManager
 * @author Alexander Litus
 */
public abstract class ProcessManagerRepository<I, PM extends ProcessManager<I, M>, M extends Message>
        extends EntityRepository<I, PM, M> implements MultiHandler {

    /**
     * The name of the method used for dispatching commands to process managers.
     *
     * <p>This constant is used for obtaining {@code Method} instance via reflection.
     *
     * @see #dispatchCommand(Message, CommandContext)
     */
    private static final String COMMAND_DISPATCHER_METHOD_NAME = "dispatchCommand";

    /**
     * The name of the method used for dispatching events to process managers.
     *
     * <p>This constant is used for obtaining {@code Method} instance via reflection.
     *
     * @see #dispatchEvent(Message, EventContext)
     */
    private static final String EVENT_DISPATCHER_METHOD_NAME = "dispatchEvent";

    /**
     * Returns a process manager ID based on the information from the command and command context.
     *
     * @param command command which the process manager handles
     * @param context context of the command
     * @return a process manager ID
     */
    protected abstract I getProcessManagerIdOnCommand(Message command, CommandContext context);

    /**
     * Returns a process manager ID based on the information from the event and event context.
     *
     * @param event event which the process manager handles
     * @param context context of the event
     * @return a process manager ID
     */
    protected abstract I getProcessManagerIdOnEvent(Message event, EventContext context);

    @Override
    public Multimap<Method, Class<? extends Message>> getCommandHandlers() {
        final Class<? extends ProcessManager> pmClass = getEntityClass();
        final Set<Class<? extends Message>> commandClasses = ProcessManager.getHandledCommandClasses(pmClass);
        final Method handler = commandDispatcherAsMethod();
        return ImmutableMultimap.<Method, Class<? extends Message>>builder()
                .putAll(handler, commandClasses)
                .build();
    }

    @Override
    public Multimap<Method, Class<? extends Message>> getEventHandlers() {
        final Class<? extends ProcessManager> pmClass = getEntityClass();
        final Set<Class<? extends Message>> eventClasses = ProcessManager.getHandledEventClasses(pmClass);
        final Method handler = eventHandlerAsMethod();
        return ImmutableMultimap.<Method, Class<? extends Message>>builder()
                .putAll(handler, eventClasses)
                .build();
    }

    /**
     * Dispatches the command to a corresponding process manager.
     *
     * <p>The {@link #getProcessManagerIdOnCommand(Message, CommandContext)} method must be implemented
     * to retrieve the correct process manager ID.
     *
     * <p>If there is no stored process manager with such an ID, a new process manager is created
     * and stored after it handles the passed command.
     *
     * @param command the command to dispatch
     * @param context the context of the command
     * @see ProcessManager#dispatchCommand(Message, CommandContext)
     */
    @SuppressWarnings("unused") // This method is used via reflection
    public List<EventRecord> dispatchCommand(Message command, CommandContext context) throws InvocationTargetException {
        final I id = getProcessManagerIdOnCommand(command, context);
        final PM manager = load(id);
        final List<EventRecord> events = manager.dispatchCommand(command, context);
        store(manager);
        return events;
    }

    /**
     * Dispatches the event to a corresponding process manager.
     *
     * <p>The {@link #getProcessManagerIdOnEvent(Message, EventContext)} method must be implemented
     * to retrieve the correct process manager ID.
     *
     * <p>If there is no stored process manager with such ID, a new process manager is created
     * and stored after it handles the passed event.
     *
     * @param event the event to dispatch
     * @param context the context of the event
     * @see ProcessManager#dispatchEvent(Message, EventContext)
     */
    @SuppressWarnings("unused") // This method is used via reflection
    public void dispatchEvent(Message event, EventContext context) throws InvocationTargetException {
        final I id = getProcessManagerIdOnEvent(event, context);
        final PM manager = load(id);
        manager.dispatchEvent(event, context);
        store(manager);
    }

    /**
     * Loads or creates a process manager by the passed ID.
     *
     * <p>The process manager is created if there was no manager with such an ID stored before.
     *
     * @param id the ID of the process manager to load
     * @return loaded or created process manager instance
     */
    @Nonnull
    @Override
    public PM load(@Nonnull I id) {
        PM result = super.load(id);
        if (result == null) {
            result = create(id);
        }
        return result;
    }

    /**
     * Returns the reference to the method {@link #dispatchCommand(Message, CommandContext)} of this repository.
     */
    private static Method commandDispatcherAsMethod() {
        return getDispatcherMethod(COMMAND_DISPATCHER_METHOD_NAME, CommandContext.class);
    }

    /**
     * Returns the reference to the method {@link #dispatchEvent(Message, EventContext)} of this repository.
     */
    private static Method eventHandlerAsMethod() {
        return getDispatcherMethod(EVENT_DISPATCHER_METHOD_NAME, EventContext.class);
    }

    private static Method getDispatcherMethod(String dispatcherMethodName, Class<? extends Message> contextClass) {
        try {
            return ProcessManagerRepository.class.getMethod(dispatcherMethodName, Message.class, contextClass);
        } catch (NoSuchMethodException e) {
            throw propagate(e);
        }
    }
}
