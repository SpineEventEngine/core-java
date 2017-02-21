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
import org.spine3.protobuf.Messages;
import org.spine3.server.BoundedContext;
import org.spine3.server.aggregate.AggregatePart;
import org.spine3.server.aggregate.AggregateRoot;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * Abstract base for entities.
 *
 * @param <I> the type of entity identifiers
 * @param <S> the type of entity state objects
 * @author Alexander Yevsyukov
 */
public abstract class AbstractEntity<I, S extends Message> implements Entity<I, S> {

    private final I id;

    @Nullable
    private volatile S state;

    /**
     * Creates new instance with the passed ID.
     */
    protected AbstractEntity(I id) {
        this.id = id;
    }

    /**
     * Obtains the ID of the entity.
     */
    @Override
    public I getId() {
        return id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @CheckReturnValue
    public S getState() {
        S result = state;
        if (result == null) {
            state = getDefaultState();
            result = state;
        }
        return result;
    }

    /**
     * Sets the object into the default state.
     *
     * <p>Default implementation does nothing. Override to customize initialization
     * behaviour of instances of your entity classes.
     */
    @SuppressWarnings("NoopMethodInAbstractClass") // by design
    protected void init() {
        // Do nothing.
    }

    /**
     * Sets the entity state to the passed value.
     */
    void injectState(S newState) {
        this.state = checkNotNull(newState);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @CheckReturnValue
    public S getDefaultState() {
        final Class<? extends Entity> entityClass = getClass();
        final DefaultStateRegistry registry = DefaultStateRegistry.getInstance();
        if (!registry.contains(entityClass)) {
            final S state = createDefaultState();
            registry.put(entityClass, state);
        }
        @SuppressWarnings("unchecked")
        // cast is safe because this type of messages is saved to the map
        final S defaultState = (S) registry.get(entityClass);
        return defaultState;
    }

    private S createDefaultState() {
        final Class<S> stateClass = getStateClass();
        final S result = Messages.newInstance(stateClass);
        return result;
    }

    /**
     * Obtains the class of the entity state.
     */
    protected Class<S> getStateClass() {
        return TypeInfo.getStateClass(getClass());
    }

    /**
     * Obtains constructor for the passed entity class.
     *
     * <p>The entity class must have a constructor with the single parameter of type defined by
     * generic type {@code <I>}.
     *
     * @param entityClass the entity class
     * @param idClass     the class of entity identifiers
     * @param <E>         the entity type
     * @param <I>         the ID type
     * @return the constructor
     * @throws IllegalStateException if the entity class does not have the required constructor
     */
    public static <E extends Entity<I, ?>, I> Constructor<E>
    getConstructor(Class<E> entityClass, Class<I> idClass) {
        try {
            final Constructor<E> result = entityClass.getDeclaredConstructor(idClass);
            result.setAccessible(true);
            return result;
        } catch (NoSuchMethodException ignored) {
            throw noSuchConstructor(entityClass.getName(), idClass.getName());
        }
    }

    private static IllegalStateException noSuchConstructor(String entityClass, String idClass) {
        final String errMsg = format(
                "%s class must declare a constructor with a single %s ID parameter.",
                entityClass, idClass
        );
        return new IllegalStateException(new NoSuchMethodException(errMsg));
    }

    /**
     * Obtains constructor for the passed aggregate part class.
     *
     * <p>The part class must have a constructor with ID and {@code AggregateRoot} parameters.
     *
     * @param entityClass the {@code AggregatePart} class
     * @param idClass     the class of entity identifiers
     * @param <E>         the entity type
     * @param <I>         the ID type
     * @return the {@code AggregatePart} constructor
     * @throws IllegalStateException if the entity class does not have the required constructor
     */
    public static <E extends AggregatePart, I> Constructor<E>
    getAggregatePartConstructor(Class<E> entityClass, Class<I> idClass) {
        final Constructor<?>[] constructors = entityClass.getDeclaredConstructors();
        for (Constructor<?> constructor : constructors) {
            final Class<?>[] parameterTypes = constructor.getParameterTypes();
            final int length = parameterTypes.length;
            if (length != 2) {
                continue;
            }

            final boolean correctConstructor =
                    idClass.equals(parameterTypes[0]) &&
                    AggregateRoot.class.isAssignableFrom(parameterTypes[1]);
            if (correctConstructor) {
                @SuppressWarnings("unchecked") // It is safe because arguments are checked above.
                final Constructor<E> result = (Constructor<E>) constructor;
                result.setAccessible(true);
                return result;
            }
        }
        final String errMsg = format("%s class must declare a constructor " +
                                     "with ID and AggregateRoot parameters.", entityClass);
        throw new IllegalStateException(errMsg);
    }

    /**
     * Creates a new {@code AggregateRoot} entity and sets it to the default state.
     *
     * @param id             the ID of the entity
     * @param boundedContext the {@code BoundedContext} to use
     * @param rootClass      the class of the {@code AggregateRoot}
     * @param <I>            the type of entity IDs
     * @return a {@code AggregateRoot} instance
     */
    public static <I, R extends AggregateRoot<I>> R
    createAggregateRootEntity(I id, BoundedContext boundedContext, Class<R> rootClass) {
        try {
            final Constructor<R> rootConstructor =
                    rootClass.getDeclaredConstructor(boundedContext.getClass(), id.getClass());
            rootConstructor.setAccessible(true);
            R root = rootConstructor.newInstance(boundedContext, id);
            return root;
        } catch (NoSuchMethodException | InvocationTargetException |
                InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Creates a new {@code AggregatePart} entity and sets it to the default state.
     *
     * @param ctor the constructor to use
     * @param id   the ID of the entity
     * @param <I>  the type of entity IDs
     * @param <E>  the type of the entity
     * @return a {@code AggregatePart} instance
     */
    public static <I, E extends AbstractEntity<I, ?>> E
    createAggregatePartEntity(Constructor<E> ctor, I id, AggregateRoot<I> root) {
        try {
            final E result = ctor.newInstance(id, root);
            result.init();
            return result;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Creates new entity and sets it to the default state.
     *
     * @param ctor the constructor to use
     * @param id   the ID of the entity
     * @param <I>  the type of entity IDs
     * @param <E>  the type of the entity
     * @return new entity
     */
    public static <I, E extends AbstractEntity<I, ?>> E createEntity(Constructor<E> ctor, I id) {
        try {
            final E result = ctor.newInstance(id);
            result.init();
            return result;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AbstractEntity)) {
            return false;
        }
        AbstractEntity<?, ?> that = (AbstractEntity<?, ?>) o;
        return Objects.equals(getId(), that.getId()) &&
               Objects.equals(getState(), that.getState());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getState());
    }
}
