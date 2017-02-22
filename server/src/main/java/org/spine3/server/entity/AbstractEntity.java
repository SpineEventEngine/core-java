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

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

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
        this.id = checkNotNull(id);
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
