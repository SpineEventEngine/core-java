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

import io.spine.annotation.Internal;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregatePart;
import io.spine.server.aggregate.model.AggregateClass;
import io.spine.server.aggregate.model.AggregatePartClass;
import io.spine.server.command.CommandHandler;
import io.spine.server.command.Commander;
import io.spine.server.command.model.CommandHandlerClass;
import io.spine.server.command.model.CommanderClass;
import io.spine.server.entity.Entity;
import io.spine.server.entity.model.EntityClass;
import io.spine.server.event.EventSubscriber;
import io.spine.server.event.model.EventSubscriberClass;
import io.spine.server.procman.ProcessManager;
import io.spine.server.procman.model.ProcessManagerClass;
import io.spine.server.projection.Projection;
import io.spine.server.projection.model.ProjectionClass;
import io.spine.server.rejection.RejectionSubscriber;
import io.spine.server.rejection.model.RejectionSubscriberClass;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Stores information of message handling classes.
 *
 * @author Alexander Yevsyukov
 * @author Dmitry Ganzha
 */
@Internal
public class Model {

    /** Maps a raw Java class to a {@code ModelClass}. */
    private final ClassMap classes = new ClassMap();

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
     * @throws DuplicateCommandHandlerError if the aggregate class handles one or more
     *         commands which are already known to the model as handled by another class
     */
    public AggregateClass<?> asAggregateClass(Class<? extends Aggregate> cls)
            throws DuplicateCommandHandlerError {
        checkNotNull(cls);
        ModelClass<?> modelClass = classes.get(cls, () -> new AggregateClass<>(cls));
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
    public AggregatePartClass<?> asAggregatePartClass(Class<? extends AggregatePart> cls)
            throws DuplicateCommandHandlerError {
        checkNotNull(cls);
        ModelClass<?> modelClass = classes.get(cls, () -> new AggregatePartClass<>(cls));
        return (AggregatePartClass<?>) modelClass;
    }

    public CommanderClass<?> asCommanderClass(Class<? extends Commander> cls)
            throws DuplicateCommandHandlerError {
        checkNotNull(cls);
        ModelClass<?> modelClass = classes.get(cls, () -> CommanderClass.of(cls));
        return (CommanderClass<?>) modelClass;
    }

    /**
     * Obtains an instance of a command handler class information.
     *
     * <p>If the passed class was not added to the model before, it would be added as the result of
     * this method call.
     *
     * @throws DuplicateCommandHandlerError if the passed command handler class handles one
     *         or more commands which are already known to the model as handled by another class
     */
    public CommandHandlerClass asCommandHandlerClass(Class<? extends CommandHandler> cls)
            throws DuplicateCommandHandlerError {
        checkNotNull(cls);
        ModelClass<?> modelClass = classes.get(cls, () -> CommandHandlerClass.of(cls));
        return (CommandHandlerClass<?>) modelClass;
    }

    /**
     * Obtains an instance of an entity class information.
     *
     * <p>If the passed class was not added to the model before, it would be added as the result of
     * this method call.
     */
    public EntityClass<?> asEntityClass(Class<? extends Entity> cls) {
        checkNotNull(cls);
        ModelClass<?> modelClass = classes.get(cls, () -> new EntityClass<>(cls));
        return (EntityClass<?>) modelClass;
    }

    /**
     * Obtains an instance of event subscriber class information.
     *
     * <p>If the passed class was not added to the model before, it would be added as the result of
     * this method call.
     */
    public EventSubscriberClass<?> asEventSubscriberClass(Class<? extends EventSubscriber> cls) {
        checkNotNull(cls);
        ModelClass<?> modelClass = classes.get(cls, () -> EventSubscriberClass.of(cls));
        return (EventSubscriberClass<?>) modelClass;
    }

    /**
     * Obtains an instance of a process manager class information.
     *
     * <p>If the passed class was not added to the model before, it would be added as the result of
     * this method call.
     *
     * @throws DuplicateCommandHandlerError if the passed process manager class handles one
     *         or more commands already known to the model as handled by another class
     */
    public ProcessManagerClass<?> asProcessManagerClass(Class<? extends ProcessManager> cls)
        throws DuplicateCommandHandlerError {
        checkNotNull(cls);
        ModelClass<?> modelClass = classes.get(cls, () -> ProcessManagerClass.of(cls));
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
        ModelClass<?> modelClass = classes.get(cls, () -> ProjectionClass.of(cls));
        return (ProjectionClass<?>) modelClass;
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
        ModelClass<?> modelClass = classes.get(cls, () -> RejectionSubscriberClass.of(cls));
        return (RejectionSubscriberClass<?>) modelClass;
    }

    private enum Singleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Model value = new Model();
    }
}
