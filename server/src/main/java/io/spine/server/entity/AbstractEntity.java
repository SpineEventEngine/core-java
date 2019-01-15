/*
 * Copyright 2019, TeamDev. All rights reserved.
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.errorprone.annotations.concurrent.LazyInit;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.spine.annotation.Internal;
import io.spine.base.Identifier;
import io.spine.core.Version;
import io.spine.core.Versions;
import io.spine.server.entity.model.EntityClass;
import io.spine.server.entity.rejection.CannotModifyArchivedEntity;
import io.spine.server.entity.rejection.CannotModifyDeletedEntity;
import io.spine.string.Stringifiers;
import io.spine.validate.ConstraintViolation;
import io.spine.validate.MessageValidator;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalArgumentException;

/**
 * Abstract base for entities.
 *
 * @param <I> the type of entity identifiers
 * @param <S> the type of entity state objects
 */
@SuppressWarnings("SynchronizeOnThis") /* This class uses double-check idiom for lazy init of some
    fields. See Effective Java 2nd Ed. Item #71. */
public abstract class AbstractEntity<I, S extends Message> implements Entity<I, S> {

    /**
     * Lazily initialized reference to the model class of this entity.
     *
     * @see #thisClass()
     * @see #getModelClass()
     */
    @LazyInit
    private volatile @MonotonicNonNull EntityClass<?> thisClass;

    /** The ID of the entity. */
    private final I id;

    /** Cached version of string ID. */
    @LazyInit
    private volatile @MonotonicNonNull String stringId;

    /**
     * The state of the entity.
     *
     * <p>Lazily initialized to the {@linkplain #getDefaultState() default state},
     * if {@linkplain #getState() accessed} before {@linkplain #setState(Message)}
     * initialization}.
     */
    @LazyInit
    private volatile @MonotonicNonNull S state;

    /** The version of the entity. */
    private volatile Version version;

    /** The lifecycle flags of the entity. */
    private volatile LifecycleFlags lifecycleFlags;

    /**
     * Indicates if the lifecycle flags of the entity were changed since initialization.
     *
     * <p>Changed lifecycle flags should be updated when
     * {@linkplain io.spine.server.entity.Repository#store(io.spine.server.entity.Entity) storing}.
     */
    private volatile boolean lifecycleFlagsChanged;

    /**
     * Creates new instance with the passed ID.
     */
    protected AbstractEntity(I id) {
        this.id = checkNotNull(id);
        setVersion(Versions.zero());
        clearLifecycleFlags();
    }

    /**
     * Creates a new instance with the passed ID and default entity state obtained
     * from the passed function.
     *
     * @param id the ID of the new entity
     * @param defaultState the function to obtain new entity state
     */
    protected AbstractEntity(I id, Function<I, S> defaultState) {
        this(id);
        checkNotNull(defaultState);
        setState(defaultState.apply(id));
    }

    @Override
    public I getId() {
        return id;
    }

    /**
     * {@inheritDoc}
     *
     * <p>If the state of the entity was not initialized, it is set to
     * {@linkplain #getDefaultState() default value} and returned.
     *
     * @return the current state or default state value
     */
    @Override
    public S getState() {
        S result = state;
        if (result == null) {
            synchronized (this) {
                result = state;
                if (result == null) {
                    state = getDefaultState();
                    result = state;
                }
            }
        }
        return result;
    }

    /**
     * Obtains model class for this entity.
     */
    protected EntityClass<?> thisClass() {
        EntityClass<?> result = thisClass;
        if (result == null) {
            synchronized (this) {
                result = thisClass;
                if (result == null) {
                    thisClass = getModelClass();
                    result = thisClass;
                }
            }
        }
        return result;
    }

    /**
     * Obtains the model class.
     */
    @Internal
    protected EntityClass<?> getModelClass() {
        return EntityClass.asEntityClass(getClass());
    }

    /**
     * Sets the entity state to the passed value.
     */
    void setState(S newState) {
        this.state = checkNotNull(newState);
    }

    /**
     * Obtains the default state of the entity.
     */
    protected final S getDefaultState() {
        @SuppressWarnings("unchecked")
        // cast is safe because this type of messages is saved to the map
        S result = (S) thisClass().getDefaultState();
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
    final void updateState(S state) {
        validate(state);
        setState(state);
    }

    /**
     * Verifies the new entity state and returns {@link ConstraintViolation}s, if any.
     *
     * <p>Default implementation uses the {@linkplain MessageValidator#validate() message
     * validation}.
     *
     * @param newState
     *         a state object to replace the current state
     * @return the violation constraints
     */
    protected List<ConstraintViolation> checkEntityState(S newState) {
        checkNotNull(newState);
        return MessageValidator.newInstance(newState)
                               .validate();
    }

    /**
     * Ensures that the passed new state is valid.
     *
     * @param newState
     *         a state object to replace the current state
     * @throws InvalidEntityStateException
     *         if the state is not valid
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
    @Override
    public String idAsString() {
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
                          .add("id", idAsString())
                          .toString();
    }

    /**
     * Sets status for the entity.
     */
    void setLifecycleFlags(LifecycleFlags lifecycleFlags) {
        if (!lifecycleFlags.equals(this.lifecycleFlags)) {
            this.lifecycleFlags = lifecycleFlags;
            this.lifecycleFlagsChanged = true;
        }
    }

    @Override
    public LifecycleFlags getLifecycleFlags() {
        LifecycleFlags result = this.lifecycleFlags == null
                                ? LifecycleFlags.getDefaultInstance()
                                : this.lifecycleFlags;
        return result;
    }

    /**
     * Tests whether the entity is marked as archived.
     *
     * @return {@code true} if the entity is archived, {@code false} otherwise
     */
    @Override
    public final boolean isArchived() {
        return getLifecycleFlags().getArchived();
    }

    /**
     * Sets {@code archived} status flag to the passed value.
     */
    protected void setArchived(boolean archived) {
        setLifecycleFlags(getLifecycleFlags().toBuilder()
                                             .setArchived(archived)
                                             .build());
    }

    /**
     * Tests whether the entity is marked as deleted.
     *
     * @return {@code true} if the entity is deleted, {@code false} otherwise
     */
    @Override
    public final boolean isDeleted() {
        return getLifecycleFlags().getDeleted();
    }

    /**
     * Sets {@code deleted} status flag to the passed value.
     */
    protected void setDeleted(boolean deleted) {
        setLifecycleFlags(getLifecycleFlags().toBuilder()
                                             .setDeleted(deleted)
                                             .build());
    }

    /**
     * Ensures that the entity is not marked as {@code archived}.
     *
     * @throws CannotModifyArchivedEntity if the entity in in the archived status
     * @see #getLifecycleFlags()
     * @see io.spine.server.entity.LifecycleFlags#getArchived()
     */
    protected void checkNotArchived() throws CannotModifyArchivedEntity {
        if (getLifecycleFlags().getArchived()) {
            Any packedId = Identifier.pack(getId());
            throw CannotModifyArchivedEntity
                    .newBuilder()
                    .setEntityId(packedId)
                    .build();
        }
    }

    /**
     * Ensures that the entity is not marked as {@code deleted}.
     *
     * @throws CannotModifyDeletedEntity if the entity is marked as {@code deleted}
     * @see #getLifecycleFlags()
     * @see io.spine.server.entity.LifecycleFlags#getDeleted()
     */
    protected void checkNotDeleted() throws CannotModifyDeletedEntity {
        if (getLifecycleFlags().getDeleted()) {
            Any packedId = Identifier.pack(getId());
            throw CannotModifyDeletedEntity
                    .newBuilder()
                    .setEntityId(packedId)
                    .build();
        }
    }

    @Override
    public boolean lifecycleFlagsChanged() {
        return lifecycleFlagsChanged;
    }

    private void clearLifecycleFlags() {
        setLifecycleFlags(LifecycleFlags.getDefaultInstance());
        lifecycleFlagsChanged = false;
    }

    /**
     * Updates the state and version of the entity.
     *
     * <p>The new state must be valid.
     *
     * <p>The passed version must have a number not less than the current version of the entity.
     *
     * @param state
     *         the state object to set
     * @param version
     *         the entity version to set
     * @throws IllegalStateException
     *         if the passed state is not valid
     * @throws IllegalArgumentException
     *         if the passed version has the number which is greater than the current
     *         version of the entity
     */
    void updateState(S state, Version version) {
        updateState(state);
        updateVersion(version);
    }

    /**
     * Obtains the version number of the entity.
     */
    protected int versionNumber() {
        int result = getVersion().getNumber();
        return result;
    }

    private void updateVersion(Version newVersion) {
        checkNotNull(newVersion);
        if (version.equals(newVersion)) {
            return;
        }

        int currentVersionNumber = versionNumber();
        int newVersionNumber = newVersion.getNumber();
        if (currentVersionNumber > newVersionNumber) {
            throw newIllegalArgumentException(
                    "A version with the lower number (%d) passed to `updateVersion()` " +
                    "of the entity with the version number %d.",
                    newVersionNumber, currentVersionNumber);
        }

        setVersion(newVersion);
    }

    /**
     * Updates the state incrementing the version number and recording time of the modification.
     *
     * <p>This is a test-only convenience method. Calling this method is equivalent to calling
     * {@link #updateState(com.google.protobuf.Message, io.spine.core.Version)} with the incremented by one version.
     *
     * <p>Please use {@link #updateState(com.google.protobuf.Message, io.spine.core.Version)} directly in the production code.
     *
     * @param newState a new state to set
     */
    @VisibleForTesting
    void incrementState(S newState) {
        updateState(newState, incrementedVersion());
    }

    @Override
    public final Version getVersion() {
        return version;
    }

    private void setVersion(Version version) {
        this.version = version;
    }

    private Version incrementedVersion() {
        return Versions.increment(getVersion());
    }

    /**
     * Advances the current version by one and records the time of the modification.
     *
     * @return new version number
     */
    int incrementVersion() {
        setVersion(incrementedVersion());
        return version.getNumber();
    }

    /**
     * Obtains timestamp of the entity version.
     */
    public Timestamp whenModified() {
        return version.getTimestamp();
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
                Objects.equals(getState(), that.getState()) &&
                Objects.equals(getVersion(), that.getVersion()) &&
                Objects.equals(getLifecycleFlags(), that.getLifecycleFlags());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getState(), getVersion(), getLifecycleFlags());
    }
}
