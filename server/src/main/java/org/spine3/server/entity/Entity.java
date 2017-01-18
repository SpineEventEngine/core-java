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

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.base.Identifiers;
import org.spine3.protobuf.Messages;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.protobuf.Timestamps.getCurrentTime;
import static org.spine3.server.reflect.Classes.getGenericParameterType;

/**
 * A server-side object with an identity.
 *
 * <p>An entity identifier can be of one of the following types:
 *   <ul>
 *      <li>String
 *      <li>Long
 *      <li>Integer
 *      <li>A class implementing {@link Message}
 *   </ul>
 *
 * <p>Consider using {@code Message}-based IDs if you want to have typed IDs in your code, and/or
 * if you need to have IDs with some structure inside. Examples of such structural IDs are:
 *   <ul>
 *      <li>EAN value used in bar codes
 *      <li>ISBN
 *      <li>Phone number
 *      <li>email address as a couple of local-part and domain
 *   </ul>
 *
 *
 * <p>A state of an entity is defined as a protobuf message and basic versioning attributes.
 * The entity keeps only its latest state and meta information associated with this state.
 *
 * @param <I> the type of the entity ID
 * @param <S> the type of the entity state
 * @author Alexander Yevsyikov
 * @author Alexander Litus
 */
public abstract class Entity<I, S extends Message> {

    /** The index of the declaration of the generic parameter type {@code I} in this class. */
    private static final int ID_CLASS_GENERIC_INDEX = 0;

    /** The index of the declaration of the generic parameter type {@code S} in this class. */
    public static final int STATE_CLASS_GENERIC_INDEX = 1;

    private final I id;

    private S state;

    private Timestamp whenModified;

    private int version;

    /**
     * Creates a new instance.
     *
     * @param id the ID for the new instance
     * @throws IllegalArgumentException if the ID is not of one of the supported types for identifiers
     */
    protected Entity(I id) {
        Identifiers.checkSupported(id.getClass());
        this.id = id;
    }

    /**
     * Obtains constructor for the passed entity class.
     *
     * <p>The entity class must have a constructor with the single parameter of type defined by
     * generic type {@code <I>}.
     *
     * @param entityClass the entity class
     * @param idClass the class of entity identifiers
     * @param <E> the entity type
     * @param <I> the ID type
     * @return the constructor
     * @throws IllegalStateException if the entity class does not have the required constructor
     */
    static <E extends Entity<I, ?>, I> Constructor<E> getConstructor(Class<E> entityClass, Class<I> idClass) {
        try {
            final Constructor<E> result = entityClass.getDeclaredConstructor(idClass);
            return result;
        } catch (NoSuchMethodException ignored) {
            throw noSuchConstructorException(entityClass.getName(), idClass.getName());
        }
    }

    private static IllegalStateException noSuchConstructorException(String entityClass, String idClass) {
        final String message = entityClass + " class must declare a constructor with a single " + idClass + " ID parameter.";
        return new IllegalStateException(new NoSuchMethodException(message));
    }

    /**
     * Creates new entity and sets it to the default state.
     *
     * @param constructor the constructor to use
     * @param id the ID of the entity
     * @param <I> the type of entity IDs
     * @param <E> the type of the entity
     * @return new entity
     */
    static <I, E extends Entity<I, ?>> E createEntity(Constructor<E> constructor, I id) {
        try {
            final E result = constructor.newInstance(id);
            result.setDefault();
            return result;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Obtains the default entity state.
     *
     * @return an empty instance of the state class
     */
    @CheckReturnValue
    protected S getDefaultState() {
        final Class<? extends Entity> entityClass = getClass();
        final DefaultStateRegistry registry = DefaultStateRegistry.getInstance();
        if (!registry.contains(entityClass)) {
            final S state = createDefaultState();
            registry.put(entityClass, state);
        }
        @SuppressWarnings("unchecked") // cast is safe because this type of messages is saved to the map
        final S defaultState = (S) registry.get(entityClass);
        return defaultState;
    }

    private S createDefaultState() {
        final Class<S> stateClass = getStateClass();
        final S result = Messages.newInstance(stateClass);
        return result;
    }

    /**
     * Obtains the entity state.
     *
     * @return the current state object or the value produced by {@link #getDefaultState()}
     *         if the state wasn't set
     */
    @CheckReturnValue
    public S getState() {
        final S result = state == null
                         ? getDefaultState()
                         : state;
        return result;
    }

    /**
     * Validates the passed state.
     *
     * <p>Does nothing by default. Aggregates may override this method to
     * specify logic of validating initial or intermediate state.
     *
     * @param state a state object to replace the current state
     * @throws IllegalStateException if the state is not valid
     */
    @SuppressWarnings({"NoopMethodInAbstractClass", "UnusedParameters"})
    // Have this no-op method to prevent enforcing implementation in all sub-classes.
    protected void validate(S state) throws IllegalStateException {
        // Do nothing by default.
    }

    /**
     * Validates and sets the state.
     *
     * @param state the state object to set
     * @param version the entity version to set
     * @param whenLastModified the time of the last modification to set
     * @see #validate(S)
     */
    protected void setState(S state, int version, Timestamp whenLastModified) {
        validate(state);
        this.state = checkNotNull(state);
        setVersion(version, whenLastModified);
    }

    /**
     * Sets version information of the entity.
     *
     * @param version the version number of the entity
     * @param whenLastModified the time of the last modification of the entity
     */
    protected void setVersion(int version, Timestamp whenLastModified) {
        this.version = version;
        this.whenModified = checkNotNull(whenLastModified);
    }


    /**
     * Updates the state incrementing the version number and recording time of the modification.
     *
     * @param newState a new state to set
     */
    protected void incrementState(S newState) {
        setState(newState, incrementVersion(), getCurrentTime());
    }

    /**
     * Sets the object into the default state.
     *
     * <p>Results of this method call are:
     * <ul>
     *   <li>The state object is set to the value produced by {@link #getDefaultState()}.
     *   <li>The version number is set to zero.
     *   <li>The {@link #whenModified} field is set to the system time of the call.
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
        final Timestamp result = whenModified == null
                                 ? Timestamp.getDefaultInstance()
                                 : whenModified;
        return result;
    }

    /**
     * Retrieves the ID class of the entities of the given class using reflection.
     *
     * @param entityClass the entity class to inspect
     * @param <I> the entity ID type
     * @return the entity ID class
     */
    public static <I> Class<I> getIdClass(Class<? extends Entity<I, ?>> entityClass) {
        checkNotNull(entityClass);
        final Class<I> idClass = getGenericParameterType(entityClass, ID_CLASS_GENERIC_INDEX);
        return idClass;
    }

    /**
     * Obtains the class of the entity state.
     */
    protected Class<S> getStateClass() {
        final Class<? extends Entity> clazz = getClass();
        return getStateClass(clazz);
    }

    /**
     * Retrieves the state class of the passed entity class.
     *
     * @param entityClass the entity class to inspect
     * @param <S> the entity state type
     * @return the entity state class
     */
    public static <S extends Message> Class<S> getStateClass(Class<? extends Entity> entityClass) {
        final Class<S> result = getGenericParameterType(entityClass, STATE_CLASS_GENERIC_INDEX);
        return result;
    }

    /**
     * Returns the short name of the ID type.
     *
     * @return
     *  <ul>
     *      <li>Short Protobuf type name if the value is {@link Message}.
     *      <li>Simple class name of the value, otherwise.
     *  </ul>
     */
    public String getShortIdTypeName() {
        if (id instanceof Message) {
            //noinspection TypeMayBeWeakened
            final Message message = (Message) id;
            final Descriptors.Descriptor descriptor = message.getDescriptorForType();
            final String result = descriptor.getName();
            return result;
        } else {
            final String result = id.getClass().getSimpleName();
            return result;
        }
    }

    @Override
    @SuppressWarnings("ConstantConditions" /* It is required to check for null. */)
    public boolean equals(Object anotherObj) {
        if (this == anotherObj) {
            return true;
        }
        if (anotherObj == null ||
            getClass() != anotherObj.getClass()) {
            return false;
        }
        @SuppressWarnings("unchecked") // parameter must have the same generics
        final Entity<I, S> another = (Entity<I, S>) anotherObj;
        if (!getId().equals(another.getId())) {
            return false;
        }
        if (!getState().equals(another.getState())) {
            return false;
        }
        if (getVersion() != another.getVersion()) {
            return false;
        }
        final boolean result = whenModified().equals(another.whenModified());
        return result;
    }

    @Override
    public int hashCode() {
        int result = getId().hashCode();
        result = 31 * result + getState().hashCode();
        result = 31 * result + whenModified().hashCode();
        result = 31 * result + getVersion();
        return result;
    }
}
