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

package org.spine3.server.saga;

import com.google.common.collect.ImmutableList;
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
 * The abstract base for repositories managing Sagas.
 *
 * @see Saga
 * @author Alexander Litus
 */
public abstract class SagaRepository<I, S extends Saga<I, M>, M extends Message>
        extends EntityRepository<I, S, M> implements MultiHandler {

    /**
     * The name of the method used for dispatching commands to sagas.
     *
     * <p>This constant is used for obtaining {@code Method} instance via reflection.
     *
     * @see #dispatchCommand(Message, CommandContext)
     */
    private static final String COMMAND_DISPATCHER_METHOD_NAME = "dispatchCommand";

    /**
     * The name of the method used for dispatching events to sagas.
     *
     * <p>This constant is used for obtaining {@code Method} instance via reflection.
     *
     * @see #dispatchEvent(Message, EventContext)
     */
    private static final String EVENT_DISPATCHER_METHOD_NAME = "dispatchEvent";

    /**
     * Returns a saga ID based on the information from the command and command context.
     *
     * @param command command which the saga handles
     * @param context context of the command
     * @return a saga ID
     */
    protected abstract I getSagaIdOnCommand(Message command, CommandContext context);

    /**
     * Returns a saga ID based on the information from the event and event context.
     *
     * @param event event which the saga handles
     * @param context context of the event
     * @return a saga ID
     */
    protected abstract I getSagaIdOnEvent(Message event, EventContext context);

    @Override
    public Multimap<Method, Class<? extends Message>> getCommandHandlers() {
        final Class<? extends Saga> sagaClass = getEntityClass();
        final Set<Class<? extends Message>> commandClasses = Saga.getHandledCommandClasses(sagaClass);
        final Method handler = commandDispatcherAsMethod();
        return ImmutableMultimap.<Method, Class<? extends Message>>builder()
                .putAll(handler, commandClasses)
                .build();
    }

    @Override
    public Multimap<Method, Class<? extends Message>> getEventHandlers() {
        final Class<? extends Saga> sagaClass = getEntityClass();
        final Set<Class<? extends Message>> eventClasses = Saga.getHandledEventClasses(sagaClass);
        final Method handler = eventHandlerAsMethod();
        return ImmutableMultimap.<Method, Class<? extends Message>>builder()
                .putAll(handler, eventClasses)
                .build();
    }

    /**
     * Dispatches the command to a corresponding saga.
     *
     * <p>The {@link #getSagaIdOnCommand(Message, CommandContext)} method must be implemented to retrieve the correct saga ID.
     *
     * <p>If there is no stored saga with such ID, a new saga is created and stored after it handles the passed command.
     *
     * @param command the command to dispatch
     * @param context the context of the command
     * @see Saga#dispatchCommand(Message, CommandContext)
     */
    @SuppressWarnings("unused") // This method is used via reflection
    public List<EventRecord> dispatchCommand(Message command, CommandContext context) throws InvocationTargetException {
        final I id = getSagaIdOnCommand(command, context);
        final S saga = load(id);
        final List<? extends Message> events = saga.dispatchCommand(command, context);
        store(saga);

        // TODO:2015-11-27:alexander.litus: return handling result
        return ImmutableList.<EventRecord>builder().build();
    }

    /**
     * Dispatches the event to a corresponding saga.
     *
     * <p>The {@link #getSagaIdOnEvent(Message, EventContext)} method must be implemented to retrieve the correct saga ID.
     *
     * <p>If there is no stored saga with such ID, a new saga is created and stored after it handles the passed event.
     *
     * @param event the event to dispatch
     * @param context the context of the event
     * @see Saga#dispatchEvent(Message, EventContext)
     */
    @SuppressWarnings("unused") // This method is used via reflection
    public void dispatchEvent(Message event, EventContext context) throws InvocationTargetException {
        final I id = getSagaIdOnEvent(event, context);
        final S saga = load(id);
        saga.dispatchEvent(event, context);
        store(saga);
    }

    /**
     * Loads or creates a saga by the passed ID.
     *
     * <p>The saga is created if there was no saga with such ID stored before.
     *
     * @param id the ID of the saga to load
     * @return loaded or created saga instance
     */
    @Nonnull
    @Override
    public S load(@Nonnull I id) {
        S result = super.load(id);
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
            return SagaRepository.class.getMethod(dispatcherMethodName, Message.class, contextClass);
        } catch (NoSuchMethodException e) {
            throw propagate(e);
        }
    }
}
