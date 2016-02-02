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

package org.spine3.server;

import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.server.reflect.Classes;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static com.google.api.client.util.Throwables.propagate;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.protobuf.util.TimeUtil.getCurrentTime;
import static org.spine3.server.EntityId.checkType;

/**
 * A server-side wrapper for objects with an identity.
 *
 * <p>See {@link EntityId} for supported ID types.
 *
 * @param <I> the type of the entity ID
 * @param <M> the type of the entity state
 * @author Alexander Yevsyikov
 * @see EntityId
 */
public abstract class Entity<I, M extends Message> {

    /**
     * The index of the declaration of the generic type {@code M} in this class.
     */
    private static final int STATE_CLASS_GENERIC_INDEX = 1;

    private final I id;

    private M state;

    private Timestamp whenModified;

    private int version;

    private final M defaultState;

    /**
     * Creates a new instance.
     *
     * @param id the ID for the new instance
     * @throws IllegalArgumentException if the ID is not of one of the supported types
     * @see EntityId
     */
    public Entity(I id) {
        // We make the constructor public in the abstract class to avoid having protected constructors in derived
        // classes. We require that entity constructors be public as they are called by repositories.
        checkType(id);
        this.id = id;
        this.defaultState = retrieveDefaultState();
    }

    /**
     * Obtains the default entity state using {@code getDefaultInstance()} method of the state class.
     *
     * @return the default state of the entity
     */
    @CheckReturnValue
    protected M getDefaultState() {
        return defaultState;
    }

    private M retrieveDefaultState() {
        final Class<M> stateClass = Classes.getGenericParameterType(getClass(), STATE_CLASS_GENERIC_INDEX);
        try {
            final Constructor<M> constructor = stateClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            final M state = constructor.newInstance();
            return state;
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw propagate(e);
        }
    }

    /**
     * Obtains the entity state.
     *
     * @return the current state object or the value produced by {@link #getDefaultState()} if the state wasn't set
     */
    @CheckReturnValue
    public M getState() {
        final M result = (state == null) ? getDefaultState() : state;
        return result;
    }

    /**
     * Validates the passed state.
     *
     * <p>Does nothing by default. Aggregate roots may override this method to
     * specify logic of validating initial or intermediate state of the root.
     *
     * @param state a state object to replace the current state
     * @throws IllegalStateException if the state is not valid
     */
    @SuppressWarnings({"NoopMethodInAbstractClass", "UnusedParameters"})
    // Have this no-op method to prevent enforcing implementation in all sub-classes.
    protected void validate(M state) throws IllegalStateException {
        // Do nothing by default.
    }

    /**
     * Validates and sets the state.
     *
     * @param state the state object to set
     * @param version the entity version to set
     * @param whenLastModified the time of the last modification to set
     * @see #validate(M)
     */
    protected void setState(M state, int version, Timestamp whenLastModified) {
        validate(state);
        this.state = checkNotNull(state, "state");
        this.version = version;
        this.whenModified = checkNotNull(whenLastModified, "whenLastModified");
    }

    /**
     * Updates the state incrementing the version number and recording time of the modification.
     *
     * @param newState a new state to set
     */
    protected void incrementState(M newState) {
        setState(newState, incrementVersion(), getCurrentTime());
    }

    /**
     * Sets the object into the default state.
     *
     * <p>Results of this method call are:
     * <ul>
     *   <li>The state object is set to the value produced by {@link #getDefaultState()}.</li>
     *   <li>The version number is set to zero.</li>
     *   <li>The {@link #whenModified} field is set to the system time of the call.</li>
     * </ul>
     * <p>The timestamp is set to current system time.
     */
    protected void setDefault() {
        setState(getDefaultState(), 0, getCurrentTime());
    }

    /**
     * @return current version number
     */
    public int getVersion() {
        return version;
    }

    /**
     * Advances the current version by one and records the time of the modification.
     *
     * @return new version number
     */
    protected int incrementVersion() {
        ++version;
        whenModified = getCurrentTime();
        return version;
    }

    @CheckReturnValue
    public I getId() {
        return id;
    }

    /**
     * Obtains the timestamp of the last modification.
     *
     * @return the timestamp instance or the value produced by {@link Timestamp#getDefaultInstance()} if the state wasn't set
     * @see #setState(Message, int, Timestamp)
     */
    @CheckReturnValue
    @Nonnull
    public Timestamp whenModified() {
        return (whenModified == null) ? Timestamp.getDefaultInstance() : whenModified;
    }
}
