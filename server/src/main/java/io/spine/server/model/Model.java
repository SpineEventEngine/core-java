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

package io.spine.server.model;

import com.google.common.collect.Maps;
import io.spine.annotation.Internal;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateClass;
import io.spine.server.command.CommandHandler;
import io.spine.server.command.CommandHandlerClass;
import io.spine.server.event.EventSubscriber;
import io.spine.server.event.EventSubscriberClass;
import io.spine.server.procman.ProcessManager;
import io.spine.server.procman.ProcessManagerClass;
import io.spine.server.projection.Projection;
import io.spine.server.projection.ProjectionClass;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Stores information of message handling classes.
 *
 * @author Alexander Yevsyukov
 */
@Internal
public class Model {

    private static final Model INSTANCE = new Model();

    /** A map from a raw class to an extended class information instance. */
    private final Map<Class<?>, HandlerClass<?>> handlerClasses = Maps.newConcurrentMap();

    public static Model getInstance() {
        return INSTANCE;
    }

    /** Prevent instantiation from outside. */
    private Model() {}

    /**
     * Obtains an instance of aggregate class information.
     */
    public AggregateClass<?> asAggregateClass(Class<? extends Aggregate> cls) {
        checkNotNull(cls);
        HandlerClass<?> handlerClass = handlerClasses.get(cls);
        if (handlerClass == null) {
            handlerClass = AggregateClass.of(cls);
            handlerClasses.put(cls, handlerClass);
        }
        return (AggregateClass<?>)handlerClass;
    }

    /**
     * Obtains an instance of a process manager class information.
     */
    public ProcessManagerClass<?> asProcessManagerClass(Class<? extends ProcessManager> cls) {
        checkNotNull(cls);
        HandlerClass<?> handlerClass = handlerClasses.get(cls);
        if (handlerClass == null) {
            handlerClass = ProcessManagerClass.of(cls);
            handlerClasses.put(cls, handlerClass);
        }
        return (ProcessManagerClass<?>)handlerClass;
    }

    /**
     * Obtains an instance of a projection class information.
     */
    public ProjectionClass<?> asProjectionClass(Class<? extends Projection> cls) {
        checkNotNull(cls);
        HandlerClass<?> handlerClass = handlerClasses.get(cls);
        if (handlerClass == null) {
            handlerClass = ProjectionClass.of(cls);
            handlerClasses.put(cls, handlerClass);
        }
        return (ProjectionClass<?>)handlerClass;
    }

    /**
     * Obtains an instance of event subscriber class information.
     */
    public EventSubscriberClass<?> asEventSubscriberClass(Class<? extends EventSubscriber> cls) {
        checkNotNull(cls);
        HandlerClass<?> handlerClass = handlerClasses.get(cls);
        if (handlerClass == null) {
            handlerClass = EventSubscriberClass.of(cls);
            handlerClasses.put(cls, handlerClass);
        }
        return (EventSubscriberClass<?>)handlerClass;
    }

    /**
     * Obtains an instance of a command handler class information.
     */
    public CommandHandlerClass asCommandHandlerClass(Class<? extends CommandHandler> cls) {
        checkNotNull(cls);
        HandlerClass<?> handlerClass = handlerClasses.get(cls);
        if (handlerClass == null) {
            handlerClass = CommandHandlerClass.of(cls);
            handlerClasses.put(cls, handlerClass);
        }
        return (CommandHandlerClass<?>)handlerClass;
    }
}
