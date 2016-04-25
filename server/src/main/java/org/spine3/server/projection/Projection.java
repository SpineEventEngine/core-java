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

package org.spine3.server.projection;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import org.spine3.base.EventContext;
import org.spine3.server.entity.Entity;
import org.spine3.server.reflect.Classes;
import org.spine3.server.reflect.EventSubscriberMethod;
import org.spine3.server.reflect.MethodRegistry;

import java.lang.reflect.InvocationTargetException;

import static com.google.common.base.Throwables.propagate;
import static org.spine3.server.reflect.EventSubscriberMethod.PREDICATE;

/**
 * {@link Projection} holds a structural representation of data extracted from a stream of events.
 *
 * <p>The process of projecting the event stream into data we collect is performed
 * by event subscribers for the events of interest. These event handlers are implemented
 * in the classes extending this abstract class.
 *
 * <p>Event subscribers are invoked by a {@link ProjectionRepository} that manages instances
 * of a stream projection class.
 *
 * @param <I> the type of the IDs
 * @param <M> the type of the state objects holding projection data
 */
public abstract class Projection<I, M extends Message> extends Entity<I, M> {

    /**
     * Creates a new instance.
     *
     * @param id the ID for the new instance
     * @throws IllegalArgumentException if the ID is not of one of the supported types
     */
    @SuppressWarnings("ConstructorNotProtectedInAbstractClass")
    public Projection(I id) {
        super(id);
    }

    protected void handle(Message event, EventContext ctx) {
        dispatch(event, ctx);
    }

    private void dispatch(Message event, EventContext ctx) {
        final Class<? extends Message> eventClass = event.getClass();
        final EventSubscriberMethod method = MethodRegistry.getInstance()
                                                        .get(getClass(), eventClass, EventSubscriberMethod.factory());
        if (method == null) {
            throw missingEventHandler(eventClass);
        }
        try {
            method.invoke(this, event, ctx);
        } catch (InvocationTargetException e) {
            propagate(e);
        }
    }

    /**
     * Returns the set of event classes handled by the passed {@link Projection} class.
     *
     * @param clazz the class to inspect
     * @return immutable set of event classes or an empty set if no events are handled
     */
    public static ImmutableSet<Class<? extends Message>> getEventClasses(Class<? extends Projection> clazz) {
        return Classes.getHandledMessageClasses(clazz, PREDICATE);
    }

    private IllegalStateException missingEventHandler(Class<? extends Message> eventClass) {
        return new IllegalStateException(String.format("Missing event handler for event class %s in the stream projection class %s",
                eventClass, this.getClass()));
    }

}