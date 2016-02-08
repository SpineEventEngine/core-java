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
import java.util.Map;

import static com.google.api.client.util.Throwables.propagate;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.protobuf.util.TimeUtil.getCurrentTime;
import static org.spine3.server.EntityId.checkType;

/**
 * A server-side wrapper for objects with an identity.
 *
 * <p>See {@link EntityId} for supported ID types.
 *
 * @param <Id> the type of the entity ID
 * @param <State> the type of the entity state
 * @author Alexander Yevsyikov
 * @author Alexander Litus
 * @see EntityId
 */
public abstract class Entity<Id, State extends Message> {

    /**
     * The index of the declaration of the generic parameter type {@code Id} in this class.
     */
    private static final int ID_CLASS_GENERIC_INDEX = 0;

    /**
     * The index of the declaration of the generic parameter type {@code State} in this class.
     */
    private static final int STATE_CLASS_GENERIC_INDEX = 1;

    private final Id id;

    private State state;

    private Timestamp whenModified;

    private int version;

    /**
     * Creates a new instance.
     *
     * @param id the ID for the new instance
     * @throws IllegalArgumentException if the ID is not of one of the supported types
     */
    public Entity(Id id) {
        // We make the constructor public in the abstract class to avoid having protected constructors in derived
        // classes. We require that entity constructors be public as they are called by repositories.
        checkType(id);
        this.id = id;
    }

    /**
     * Obtains the default entity state.
     *
     * @return an empty instance of the state class
     */
    @CheckReturnValue
    protected State getDefaultState() {
        final Class<? extends Entity> entityClass = getClass();
        final DefaultStateRegistry registry = DefaultStateRegistry.getInstance();
        if (!registry.contains(entityClass)) {
            final State state = retrieveDefaultState();
            registry.put(entityClass, state);
        }
        @SuppressWarnings("unchecked") // cast is safe because this type of messages is saved to the map
        final State defaultState = (State) registry.get(entityClass);
        return defaultState;
    }

    private State retrieveDefaultState() {
        final Class<State> stateClass = Classes.getGenericParameterType(getClass(), STATE_CLASS_GENERIC_INDEX);
        try {
            final Constructor<State> constructor = stateClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            final State state = constructor.newInstance();
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
    public State getState() {
        final State result = (state == null) ? getDefaultState() : state;
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
    protected void validate(State state) throws IllegalStateException {
        // Do nothing by default.
    }

    /**
     * Validates and sets the state.
     *
     * @param state the state object to set
     * @param version the entity version to set
     * @param whenLastModified the time of the last modification to set
     * @see #validate(State)
     */
    protected void setState(State state, int version, Timestamp whenLastModified) {
        validate(state);
        this.state = checkNotNull(state);
        this.version = version;
        this.whenModified = checkNotNull(whenLastModified);
    }

    /**
     * Updates the state incrementing the version number and recording time of the modification.
     *
     * @param newState a new state to set
     */
    protected void incrementState(State newState) {
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
    public Id getId() {
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

    /**
     * Retrieves the ID class of the entities of the given class using reflection.
     *
     * @param entityClass the class of entities to check
     * @return the entity ID class
     */
    public static <Id> Class<Id> getIdClass(Class<? extends Entity<Id, ?>> entityClass) {
        final Class<Id> idClass = Classes.getGenericParameterType(entityClass, ID_CLASS_GENERIC_INDEX);
        return idClass;
    }

    /**
     * A wrapper for the map from entity classes to entity default states.
     */
    private static class DefaultStateRegistry {

        private final Map<Class<? extends Entity>, Message> defaultStates = newHashMap();

        /**
         * Specifies if the entity state of this class is already registered.
         *
         * @param entityClass the class to check
         * @return {@code true} if there is a state for the passed class, {@code false} otherwise
         */
        @CheckReturnValue
        public boolean contains(Class<? extends Entity> entityClass) {
            final boolean result = defaultStates.containsKey(entityClass);
            return result;
        }

        /**
         * Saves a state.
         *
         * @param entityClass an entity class
         * @param state a default state of the entity
         * @throws IllegalArgumentException if the state of this class is already registered
         */
        public void put(Class<? extends Entity> entityClass, Message state) {
            if (contains(entityClass)) {
                throw new IllegalArgumentException("This class is registered already: " + entityClass.getName());
            }
            defaultStates.put(entityClass, state);
        }

        /**
         * Obtains a state for the passed class..
         *
         * @param entityClass an entity class
         */
        @CheckReturnValue
        public Message get(Class<? extends Entity> entityClass) {
            final Message state = defaultStates.get(entityClass);
            return state;
        }

        public static DefaultStateRegistry getInstance() {
            return Singleton.INSTANCE.value;
        }

        private enum Singleton {
            INSTANCE;
            @SuppressWarnings("NonSerializableFieldInSerializableClass")
            private final DefaultStateRegistry value = new DefaultStateRegistry();
        }
    }
}
