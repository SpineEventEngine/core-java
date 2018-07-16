/*
 * Copyright 2018, TeamDev. All rights reserved.
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.core.CommandClass;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateClass;
import io.spine.server.aggregate.AggregatePart;
import io.spine.server.aggregate.AggregatePartClass;
import io.spine.server.command.CommandHandler;
import io.spine.server.command.CommandHandlerClass;
import io.spine.server.command.CommandHandlingClass;
import io.spine.server.entity.Entity;
import io.spine.server.entity.EntityClass;
import io.spine.server.event.EventSubscriber;
import io.spine.server.event.EventSubscriberClass;
import io.spine.server.procman.ProcessManager;
import io.spine.server.procman.ProcessManagerClass;
import io.spine.server.projection.Projection;
import io.spine.server.projection.ProjectionClass;
import io.spine.server.rejection.RejectionSubscriber;
import io.spine.server.rejection.RejectionSubscriberClass;

import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Sets.intersection;

/**
 * Stores information of message handling classes.
 *
 * @author Alexander Yevsyukov
 * @author Dmitry Ganzha
 */
@Internal
public class Model {

    /**
     * A map from a {@linkplain #nameOf(Class) a class name} to an extended class information
     * instance.
     */
    private final Map<String, ModelClass<?>> classes = Maps.newConcurrentMap();

    public static Model getInstance() {
        return Singleton.INSTANCE.value;
    }

    /** Prevents instantiation from outside. */
    private Model() {
    }

    /**
     * Clears the classes already added to the {@code Model}.
     *
     * <p>This method can be useful when multiple Spine projects are processed under the same
     * static context, e.g. in tests.
     */
    public void clear() {
        classes.clear();
    }

    /**
     * Obtains an instance of aggregate class information.
     *
     * <p>If the passed class was not added to the model before, it would be added as the result of
     * this method call.
     *
     * @throws DuplicateCommandHandlerError if there is the aggregate class handles one or more
     *         commands that are handled by another class, which was added to the model before
     *         calling this method
     */
    public AggregateClass<?> asAggregateClass(Class<? extends Aggregate> cls) {
        checkNotNull(cls);
        ModelClass<?> modelClass = classes.get(nameOf(cls));
        if (modelClass == null) {
            modelClass = new AggregateClass<>(cls);
            checkDuplicates((CommandHandlingClass) modelClass);
            classes.put(nameOf(cls), modelClass);
        }
        return (AggregateClass<?>) modelClass;
    }

    /**
     * Obtains an instance of aggregate part class information.
     *
     * <p>If the passed class is not added to the model before, it will be added as the result of
     * this method call.
     *
     * @throws DuplicateCommandHandlerError if the given aggregate part class handles one or
     *         more commands which are already known to the model as handled by another class
     */
    public AggregatePartClass<?> asAggregatePartClass(Class<? extends AggregatePart> cls) {
        checkNotNull(cls);
        ModelClass<?> modelClass = classes.get(nameOf(cls));
        if (modelClass == null) {
            modelClass = new AggregatePartClass<>(cls);
            checkDuplicates((CommandHandlingClass) modelClass);
            classes.put(nameOf(cls), modelClass);
        }
        return (AggregatePartClass<?>) modelClass;
    }

    /**
     * Obtains an instance of a process manager class information.
     *
     * <p>If the passed class was not added to the model before, it would be added as the result of
     * this method call.
     *
     * @throws DuplicateCommandHandlerError if there is the passed process manager class handles one
     *         or more commands that are handled by another class, which was added to the model
     *         before calling this method
     */
    public ProcessManagerClass<?> asProcessManagerClass(Class<? extends ProcessManager> cls)
        throws DuplicateCommandHandlerError {
        checkNotNull(cls);
        ModelClass<?> modelClass = classes.get(nameOf(cls));
        if (modelClass == null) {
            modelClass = ProcessManagerClass.of(cls);
            checkDuplicates((CommandHandlingClass) modelClass);
            classes.put(nameOf(cls), modelClass);
        }
        return (ProcessManagerClass<?>) modelClass;
    }

    /**
     * Obtains an instance of a projection class information.
     *
     * <p>If the passed class was not added to the model before, it would be added as the result of
     * this method call.
     */
    public ProjectionClass<?> asProjectionClass(Class<? extends Projection> cls) {
        checkNotNull(cls);
        ModelClass<?> modelClass = classes.get(nameOf(cls));
        if (modelClass == null) {
            modelClass = ProjectionClass.of(cls);
            classes.put(nameOf(cls), modelClass);
        }
        return (ProjectionClass<?>) modelClass;
    }

    /**
     * Obtains an instance of event subscriber class information.
     *
     * <p>If the passed class was not added to the model before, it would be added as the result of
     * this method call.
     */
    public EventSubscriberClass<?> asEventSubscriberClass(Class<? extends EventSubscriber> cls) {
        checkNotNull(cls);
        ModelClass<?> modelClass = classes.get(nameOf(cls));
        if (modelClass == null) {
            modelClass = EventSubscriberClass.of(cls);
            classes.put(nameOf(cls), modelClass);
        }
        return (EventSubscriberClass<?>) modelClass;
    }

    /**
     * Obtains an instance of a command handler class information.
     *
     * <p>If the passed class was not added to the model before, it would be added as the result of
     * this method call.
     *
     * @throws DuplicateCommandHandlerError if there is the passed command handler class handles one
     *         or more commands that are handled by another class, which was added to the model
     *         before calling this method
     */
    public CommandHandlerClass asCommandHandlerClass(Class<? extends CommandHandler> cls)
            throws DuplicateCommandHandlerError {
        checkNotNull(cls);
        ModelClass<?> modelClass = classes.get(nameOf(cls));
        if (modelClass == null) {
            modelClass = CommandHandlerClass.of(cls);
            checkDuplicates((CommandHandlingClass) modelClass);
            classes.put(nameOf(cls), modelClass);
        }
        return (CommandHandlerClass<?>) modelClass;
    }

    /**
     * Obtains an instance of a rejection subscriber class information.
     *
     * <p>If the passed class was not added to the model before, it would be added as the result of
     * this method call.
     */
    public
    RejectionSubscriberClass<?> asRejectionSubscriber(Class<? extends RejectionSubscriber> cls) {
        checkNotNull(cls);
        ModelClass<?> modelClass = classes.get(nameOf(cls));
        if (modelClass == null) {
            modelClass = RejectionSubscriberClass.of(cls);
            classes.put(nameOf(cls), modelClass);
        }
        return (RejectionSubscriberClass<?>) modelClass;
    }

    private void checkDuplicates(CommandHandlingClass candidate)
        throws DuplicateCommandHandlerError {
        Set<CommandClass> candidateCommands = candidate.getCommands();
        ImmutableMap.Builder<Set<CommandClass>, CommandHandlingClass> map = ImmutableMap.builder();

        for (ModelClass<?> modelClass : classes.values()) {
            if (modelClass instanceof CommandHandlingClass) {
                CommandHandlingClass commandHandler = (CommandHandlingClass) modelClass;
                Set<CommandClass> commandClasses = commandHandler.getCommands();
                Set<CommandClass> intersection = intersection(commandClasses, candidateCommands);
                if (intersection.size() > 0) {
                    map.put(intersection, commandHandler);
                }
            }
        }

        ImmutableMap<Set<CommandClass>, CommandHandlingClass> currentHandlers = map.build();
        if (!currentHandlers.isEmpty()) {
            throw new DuplicateCommandHandlerError(candidate, currentHandlers);
        }
    }

    /**
     * Obtains an instance of an entity class information.
     *
     * <p>If the passed class was not added to the model before, it would be added as the result of
     * this method call.
     */
    public EntityClass<?> asEntityClass(Class<? extends Entity> cls) {
        checkNotNull(cls);
        ModelClass<?> modelClass = classes.get(nameOf(cls));
        if (modelClass == null) {
            modelClass = new EntityClass<>(cls);
            classes.put(nameOf(cls), modelClass);
        }
        return (EntityClass<?>) modelClass;
    }

    /**
     * Obtains the default entity state by entity class.
     *
     * @return default entity state
     */
    public Message getDefaultState(Class<? extends Entity> cls) {
        checkNotNull(cls);
        DefaultStateRegistry registry = DefaultStateRegistry.getInstance();
        Message result = registry.get(cls);
        return result;
    }

    /**
     * Returns a unique identifying name of the given class.
     *
     * <p>The returned value is guaranteed to be unique per class and non-null.
     *
     * @param cls the {@link Class} to identity
     * @return a non-null class name
     */
    private static String nameOf(Class<?> cls) {
        return cls.getName();
    }

    private enum Singleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Model value = new Model();
    }
}
