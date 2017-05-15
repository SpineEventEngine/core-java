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

package org.spine3.server.projection;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import org.spine3.base.EventContext;
import org.spine3.base.Version;
import org.spine3.server.entity.EventPlayingEntity;
import org.spine3.server.reflect.EventSubscriberMethod;
import org.spine3.type.EventClass;
import org.spine3.validate.ValidatingBuilder;

import java.lang.reflect.InvocationTargetException;

import static org.spine3.server.reflect.EventSubscriberMethod.forMessage;

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
public abstract class Projection<I,
                                 M extends Message,
                                 B extends ValidatingBuilder<M, ? extends Message.Builder>>
        extends EventPlayingEntity<I, M, B> {

    /**
     * Creates a new instance.
     *
     * @param id the ID for the new instance
     * @throws IllegalArgumentException if the ID is not of one of the supported types
     */
    protected Projection(I id) {
        super(id);
    }

    protected void handle(Message event, EventContext ctx) {
        apply(event, ctx);
    }


    void apply(Message eventMessage,
                         EventContext eventContext)  {
        final EventSubscriberMethod method = forMessage(getClass(), eventMessage);
        try {
            method.invoke(this, eventMessage, eventContext);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>This method has protected access to be accessible by the
     * {@code ProjectionBuilder} test utility classes from the {@code testutil} module.
     */
    @Override               // Overridden to expose this method to tests.
    @VisibleForTesting
    protected void injectState(M stateToRestore, Version version) {
        super.injectState(stateToRestore, version);
    }

    static class TypeInfo {

        private TypeInfo() {
            // Prevent instantiation of this utility class.
        }

        /**
         * Returns the set of event classes handled by the passed {@link Projection} class.
         *
         * @param cls the class to inspect
         * @return immutable set of event classes or an empty set if no events are handled
         */
        static ImmutableSet<EventClass> getEventClasses(Class<? extends Projection> cls) {
            return EventSubscriberMethod.getEventClasses(cls);
        }
    }
}
