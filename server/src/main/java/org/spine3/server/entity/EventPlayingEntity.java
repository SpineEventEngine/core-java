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
package org.spine3.server.entity;

import com.google.protobuf.Message;
import org.spine3.annotation.Internal;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.Version;
import org.spine3.server.reflect.GenericTypeIndex;
import org.spine3.validate.ConstraintViolationThrowable;
import org.spine3.validate.ValidatingBuilder;
import org.spine3.validate.ValidatingBuilders;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.base.Events.getMessage;
import static org.spine3.server.entity.EventPlayingEntity.GenericParameter.STATE_BUILDER;
import static org.spine3.util.Exceptions.illegalStateWithCauseOf;
import static org.spine3.util.Reflection.getGenericParameterType;

/**
 * A base for entities, which can play {@linkplain org.spine3.base.Event events}.
 *
 * @author Alex Tymchenko
 */
public abstract class EventPlayingEntity <I,
                                          S extends Message,
                                          B extends ValidatingBuilder<S, ? extends Message.Builder>>
                      extends AbstractVersionableEntity<I, S> {

    /**
     * The builder for the entity state.
     *
     * <p>This field is non-null only when the entity changes its state
     * during command handling or playing events.
     *
     * @see #createBuilder()
     * @see #getBuilder()
     * @see #updateState()
     */
    @Nullable
    private volatile B builder;

    /**
     * The flag, which becomes {@code true}, if the state of the entity
     * {@linkplain #updateState() has been changed} since it has been
     * {@linkplain RecordBasedRepository#findOrCreate(Object)} loaded or created.
     */
    private volatile boolean stateChanged;

    /**
     * Creates a new instance.
     *
     * @param id the ID for the new instance
     * @throws IllegalArgumentException if the ID is not of one of the
     *                                  {@linkplain Entity supported types}
     */
    protected EventPlayingEntity(I id) {
        super(id);
    }

    protected abstract void apply(Message eventMessage,
                                  EventContext context) throws InvocationTargetException;

    /**
     * Determines whether the state of this entity or its lifecycle flags have been modified
     * since this entity instance creation.
     *
     * <p>This method is used internally to determine whether this entity instance should be
     * stored or the storage update can be skipped for this instance.
     *
     * @return {@code true} if the state or flags have been modified, {@code false} otherwise.
     */
    @Internal
    public boolean isChanged() {
        return this.stateChanged || lifecycleFlagsChanged();
    }

    /**
     * Obtains the instance of the state builder.
     *
     * <p>This method must be called only from within an event applier.
     *
     * @return the instance of the new state builder
     * @throws IllegalStateException if the method is called from outside an event applier
     */
    protected B getBuilder() {
        if (!isUpdateStateInProgress()) {
            throw new IllegalStateException(
                    "Builder is not available. Make sure to call getBuilder() " +
                            "only from an event applier method.");
        }
        return builder;
    }

    /**
     * Determines if the state update cycle is currently active.
     *
     * @return {@code true} if it is active, {@code false} otherwise
     */
    protected boolean isUpdateStateInProgress() {
        final boolean result = this.builder != null;
        return result;
    }

    /**
     * Updates the entity state and closes the update phase of the entity.
     */
    protected void updateState() {
        try {
            final B builder = getBuilder();

            // The state is only updated, if at least some changes were made to the builder.
            if(builder.isDirty()) {
                final S newState = builder.build();

                final Version version = getVersion();
                updateState(newState, version);
            }
        } catch (ConstraintViolationThrowable violation) {
            // should not happen, as the `Builder` validates the input in its setters.
            throw illegalStateWithCauseOf(violation);
        } finally {
            releaseBuilder();
        }
    }

    /**
     * Sets the new state of the entity.
     *
     * <p>This method is called during the entity update phase.
     * It is not not supposed to be called from outside of this class.
     *
     * @param state   the state object to set
     * @param version the entity version to set
     * @throws IllegalStateException if the method is called from outside
     */
    @Internal
    @Override
    protected final void updateState(S state, Version version) {
        if (builder == null) {
            throw new IllegalStateException(
                    "setState() is called from outside of the entity update phase.");
        }
        super.updateState(state, version);
        markStateChanged();
    }

    private void markStateChanged() {
        this.stateChanged = true;
    }

    protected void play(Iterable<Event> events) {

        if(!isUpdateStateInProgress()) {
            createBuilder();
        }

        try {
            for (Event event : events) {
                final Message message = getMessage(event);
                final EventContext context = event.getContext();
                try {
                    apply(message, context);
                    final Version newVersion = context.getVersion();
                    advanceVersion(newVersion);
                } catch (InvocationTargetException e) {
                    throw illegalStateWithCauseOf(e);
                }
            }
        } finally {
            /*
                We perform updating the state of the entity in this `finally`
                block (even if there was an exception in one of the appliers)
                because we want to transit the entity out of the “applying events” mode
                anyway. We do this to minimize the damage to the entity
                in the case of an exception caused by an applier method.

                In general, applier methods must not throw. Command handlers can
                in case of business failures.

                The exception thrown from an applier still will be seen because we
                re-throw its cause in the `catch` block above.
             */
            updateState();
        }
    }

    /**
     * This method starts the phase of updating the entity state.
     *
     * <p>The update phase is closed by the {@link #updateState()}.
     */
    protected void createBuilder() {
        final B builder = newBuilderInstance();
        builder.mergeFrom(getState());
        this.builder = builder;
    }

    /**
     * Sets the passed state and version.
     *
     * <p>The method circumvents the protection in the {@link #updateState(Message, Version)
     * setState()} method by creating a fake builder instance, which is cleared
     * after the call.
     *
     * <p>The {@linkplain #isChanged()} return value is not affected by this method.
     */
    protected void injectState(S stateToRestore, Version versionFromSnapshot) {
        try {
            // Remember the current flag value.
            final boolean wasStateChanged = this.stateChanged;

            @SuppressWarnings("unchecked")
            // The cast is safe as we checked the type on the construction.
            final B fakeBuilder = newBuilderInstance();
            this.builder = fakeBuilder;
            updateState(stateToRestore, versionFromSnapshot);

            // Restore the previously memoized value.
            this.stateChanged = wasStateChanged;
        } finally {
            releaseBuilder();
        }
    }

    protected void releaseBuilder() {
        this.builder = null;
    }

    private B newBuilderInstance() {
        @SuppressWarnings("unchecked")   // it's safe, as we rely on the definition of this class.
        final Class<? extends EventPlayingEntity<I, S, B>> aClass =
                (Class<? extends EventPlayingEntity<I, S, B>>)getClass();
        final Class<B> builderClass = TypeInfo.getBuilderClass(aClass);
        final B builder = ValidatingBuilders.newInstance(builderClass);
        return builder;
    }



    /**
     * Enumeration of generic type parameters of this class.
     */
    enum GenericParameter implements GenericTypeIndex {

        /** The index of the generic type {@code <I>}. */
        ID(0),

        /** The index of the generic type {@code <S>}. */
        STATE(1),

        /** The index of the generic type {@code <B>}. */
        STATE_BUILDER(2);

        private final int index;

        GenericParameter(int index) {
            this.index = index;
        }

        @Override
        public int getIndex() {
            return this.index;
        }
    }

    /**
     * Provides type information on classes extending {@code EventPlayingEntity}.
     */
    static class TypeInfo {

        private TypeInfo() {
            // Prevent construction from outside.
        }

        /**
         * Obtains the class of the {@linkplain ValidatingBuilder} for the given
         * {@code EventPlayingEntity} descendant class {@code entityClass}.
         */
        private static <I,
                        S extends Message,
                        B extends ValidatingBuilder<S, ? extends Message.Builder>>
        Class<B> getBuilderClass(Class<? extends EventPlayingEntity<I, S, B>> entityClass) {
            checkNotNull(entityClass);

            final Class<B> builderClass = getGenericParameterType(entityClass,
                                                                  STATE_BUILDER.getIndex());
            return builderClass;
        }
    }
}
