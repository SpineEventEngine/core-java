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
import org.spine3.annotations.Internal;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.Version;
import org.spine3.util.Exceptions;
import org.spine3.validate.ConstraintViolationThrowable;
import org.spine3.validate.ValidatingBuilder;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;

import static org.spine3.base.Events.getMessage;
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
     * The builder for the aggregate state.
     *
     * <p>This field is non-null only when the aggregate changes its state
     * during command handling or playing events.
     *
     * @see #createBuilder()
     * @see #getBuilder()
     * @see #updateState()
     */
    @Nullable
    private volatile B builder;


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

    protected abstract void apply(Message eventMessage) throws InvocationTargetException;

    /**
     * Obtains the instance of the state builder.
     *
     * <p>This method must be called only from within an event applier.
     *
     * @return the instance of the new state builder
     * @throws IllegalStateException if the method is called from outside an event applier
     */
    protected B getBuilder() {
        if (this.builder == null) {
            throw new IllegalStateException(
                    "Builder is not available. Make sure to call getBuilder() " +
                            "only from an event applier method.");
        }
        return builder;
    }

    /**
     * Updates the aggregate state and closes the update phase of the aggregate.
     */
    protected void updateState() {
        final S newState;
        try {
            newState = getBuilder().build();
        } catch (ConstraintViolationThrowable violation) {
            // should not happen, as the `Builder` validates the input in its setters.
            throw Exceptions.illegalStateWithCauseOf(violation);
        }
        final Version version = getVersion();
        updateState(newState, version);
        this.builder = null;
    }

    /**
     * Sets the new state of the aggregate.
     *
     * <p>This method is called during the aggregate update phase.
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
                    "setState() is called from outside of the aggregate update phase.");
        }
        super.updateState(state, version);
    }

    protected void play(Iterable<Event> events) {

        if(builder == null) {
            createBuilder();
        }

        try {
            for (Event event : events) {
                final Message message = getMessage(event);
                final EventContext context = event.getContext();
                try {
                    apply(message);
                    final Version newVersion = context.getVersion();
                    advanceVersion(newVersion);
                } catch (InvocationTargetException e) {
                    throw illegalStateWithCauseOf(e);
                }
            }
        } finally {
            /*
                We perform updating the state of the aggregate in this `finally`
                block (even if there was an exception in one of the appliers)
                because we want to transit the aggregate out of the “applying events” mode
                anyway. We do this to minimize the damage to the aggregate
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

    //TODO:5/6/17:alex.tymchenko: decide on whether it belongs here. Originally from `Aggregate`.
    /**
     * Sets the passed state and version.
     *
     * <p>The method circumvents the protection in the {@link #updateState(Message, Version)
     * setState()} method by creating a fake builder instance, which is cleared
     * after the call.
     *
     * <p>This method has package-private access to be accessible by the
     * {@code AggregateBuilder} test utility class from the {@code testutil} module.
     */
    protected void injectState(S stateToRestore, Version versionFromSnapshot) {
        try {
            @SuppressWarnings("unchecked")
            // The cast is safe as we checked the type on the construction.
            final B fakeBuilder = newBuilderInstance();
            this.builder = fakeBuilder;
            updateState(stateToRestore, versionFromSnapshot);
        } finally {
            releaseBuilder();
        }
    }

    //TODO:5/6/17:alex.tymchenko: implement this in a more clear way.
    private B newBuilderInstance() {
        final B builder;
        try {
            final Class<B> builderClass = getGenericParameterType(getClass(), 2);
            builder = (B) builderClass.getMethod("newBuilder")
                                      .invoke(null);

        } catch (Exception e) {
            throw Exceptions.illegalStateWithCauseOf(e);
        }
        return builder;
    }

    protected void releaseBuilder() {
        this.builder = null;
    }
}
