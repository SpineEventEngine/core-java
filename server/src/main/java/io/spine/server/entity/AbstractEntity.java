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

package io.spine.server.entity;

import com.google.common.base.MoreObjects;
import com.google.protobuf.Message;
import io.spine.server.model.Model;
import io.spine.string.Stringifiers;
import io.spine.validate.ConstraintViolation;
import io.spine.validate.MessageValidator;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstract base for entities.
 *
 * @param <I> the type of entity identifiers
 * @param <S> the type of entity state objects
 * @author Alexander Yevsyukov
 * @author Dmitry Ganzha
 */
public abstract class AbstractEntity<I, S extends Message> implements Entity<I, S> {

    /**
     * Lazily initialized reference to the model class of this entity.
     *
     * @see #thisClass()
     * @see #getModelClass()
     */
    private volatile @MonotonicNonNull EntityClass<?> thisClass;

    /** The ID of the entity. */
    private final I id;

    /** Cached version of string ID. */
    private volatile @MonotonicNonNull String stringId;

    /** The state of the entity. */
    private volatile @MonotonicNonNull S state;

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
    public S getState() {
        if (state == null) {
            state = getDefaultState();
        }
        return state;
    }

    /**
     * Obtains model class for this aggregate.
     */
    protected EntityClass<?> thisClass() {
        if (thisClass == null) {
            thisClass = getModelClass();
        }
        return thisClass;
    }

    /**
     * Obtains the model class for this entity from the {@link io.spine.server.model.Model Model}.
     */
    protected EntityClass<?> getModelClass() {
        return Model.getInstance()
                    .asEntityClass(getClass());
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
    void setState(S newState) {
        this.state = checkNotNull(newState);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public S getDefaultState() {
        Class<? extends Entity> entityClass = getClass();
        @SuppressWarnings("unchecked")
        // cast is safe because this type of messages is saved to the map
        S result = (S) Model.getInstance()
                                  .getDefaultState(entityClass);
        return result;
    }

    /**
     * Updates the state of the entity.
     *
     * <p>The new state must be {@linkplain #validate(Message) valid}.
     *
     * @param  state the new state to set
     * @throws InvalidEntityStateException
     *         if the passed state is not {@linkplain #validate(Message) valid}
     */
    protected final void updateState(S state) {
        validate(state);
        setState(state);
    }

    /**
     * Verifies the new entity state and returns {@link ConstraintViolation}s, if any.
     *
     * <p>Default implementation uses the {@linkplain MessageValidator#validate(Message)
     * message validation}.
     *
     * @param  newState a state object to replace the current state
     * @return the violation constraints
     */
    protected List<ConstraintViolation> checkEntityState(S newState) {
        checkNotNull(newState);
        return MessageValidator.newInstance()
                               .validate(newState);
    }

    /**
     * Ensures that the passed new state is valid.
     *
     * @param   newState a state object to replace the current state
     * @throws InvalidEntityStateException if the state is not valid
     * @see #checkEntityState(Message)
     */
    private void validate(S newState) throws InvalidEntityStateException {
        List<ConstraintViolation> violations = checkEntityState(newState);
        if (!violations.isEmpty()) {
            throw InvalidEntityStateException.onConstraintViolations(newState, violations);
        }
    }

    /**
     * Obtains ID of the entity in the {@linkplain Stringifiers#toString(Object) string form}.
     *
     * <p>Subsequent calls to the method returns a cached instance of the string, which minimizes
     * the performance impact of repeated calls.
     *
     * @return string form of the entity ID
     */
    @SuppressWarnings("SynchronizeOnThis") // See Effective Java 2nd Ed. Item #71.
    public String stringId() {
        String result = stringId;
        if (result == null) {
            synchronized (this) {
                result = stringId;
                if (result == null) {
                    stringId = Stringifiers.toString(getId());
                    result = stringId;
                }
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("id", stringId())
                          .toString();
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
