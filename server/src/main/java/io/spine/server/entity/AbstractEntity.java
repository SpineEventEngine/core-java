/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import com.google.errorprone.annotations.concurrent.LazyInit;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.spine.annotation.Internal;
import io.spine.base.EntityState;
import io.spine.base.Identifier;
import io.spine.core.Version;
import io.spine.core.Versions;
import io.spine.server.entity.model.EntityClass;
import io.spine.server.entity.rejection.CannotModifyArchivedEntity;
import io.spine.server.entity.rejection.CannotModifyDeletedEntity;
import io.spine.server.log.HandlerLifecycle;
import io.spine.server.log.HandlerLog;
import io.spine.server.model.HandlerMethod;
import io.spine.string.Stringifiers;
import io.spine.validate.ConstraintViolation;
import io.spine.validate.Validate;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Level;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.spine.logging.Logging.loggerFor;
import static io.spine.util.Exceptions.newIllegalArgumentException;
import static io.spine.validate.Validate.checkValid;
import static io.spine.validate.Validate.validateChange;
import static io.spine.validate.Validate.violationsOf;

/**
 * Abstract base for entities.
 *
 * @param <I>
 *         the type of entity identifiers
 * @param <S>
 *         the type of entity state objects
 */
@SuppressWarnings({
        "SynchronizeOnThis" /* This class uses double-check idiom for lazy init of some
            fields. See Effective Java 2nd Ed. Item #71. */,
        "ClassWithTooManyMethods"})
public abstract class AbstractEntity<I, S extends EntityState>
        implements Entity<I, S>, HandlerLifecycle {

    /**
     * Lazily initialized reference to the model class of this entity.
     *
     * @see #thisClass()
     * @see #modelClass()
     */
    @LazyInit
    private volatile @MonotonicNonNull EntityClass<?> thisClass;

    /**
     * The ID of the entity.
     *
     * <p>Assigned either through the {@linkplain #AbstractEntity(Object) constructor which
     * accepts the ID}, or via {@link #setId(Object)}. Is never {@code null}.
     */
    private @MonotonicNonNull I id;

    /** Cached version of string ID. */
    @LazyInit
    private volatile @MonotonicNonNull String stringId;

    /**
     * The state of the entity.
     *
     * <p>Lazily initialized to the {@linkplain #defaultState() default state},
     * if {@linkplain #state() accessed} before {@linkplain #setState(EntityState)}
     * initialization}.
     */
    @LazyInit
    private volatile @MonotonicNonNull S state;

    /** The version of the entity. */
    private volatile Version version;

    /** The lifecycle flags of the entity. */
    private volatile @MonotonicNonNull LifecycleFlags lifecycleFlags;

    /**
     * Indicates if the lifecycle flags of the entity were changed since initialization.
     *
     * <p>Changed lifecycle flags should be updated when
     * {@linkplain io.spine.server.entity.Repository#store(io.spine.server.entity.Entity) storing}.
     */
    private volatile boolean lifecycleFlagsChanged;

    private @Nullable HandlerLog handlerLog;

    /**
     * Creates a new instance with the zero version and cleared lifecycle flags.
     *
     * <p>When this constructor is called, the entity ID must be {@linkplain #setId(Object) set}
     * before any other interactions with the instance.
     */
    protected AbstractEntity() {
        setVersion(Versions.zero());
        clearLifecycleFlags();
    }

    /**
     * Creates new instance with the passed ID.
     */
    protected AbstractEntity(I id) {
        this();
        setId(id);
    }

    /**
     * Assigns the ID to the entity.
     */
    @SuppressWarnings("InstanceVariableUsedBeforeInitialized") // checked in `if`
    final void setId(I id) {
        checkNotNull(id);
        if (this.id != null) {
            checkState(id.equals(this.id),
                       "Entity ID already assigned to `%s`." +
                               " Attempted to reassign to `%s`.", this.id, id);
        }
        this.id = id;
    }

    /**
     * Creates a new instance with the passed ID and default entity state obtained
     * from the passed function.
     *
     * @param id
     *         the ID of the new entity
     * @param defaultState
     *         the function to obtain new entity state
     */
    protected AbstractEntity(I id, Function<I, S> defaultState) {
        this(id);
        checkNotNull(defaultState);
        setState(defaultState.apply(id));
    }

    @Override
    public I id() {
        return checkNotNull(id);
    }

    /**
     * {@inheritDoc}
     *
     * <p>If the state of the entity was not initialized, it is set to
     * {@linkplain #defaultState() default value} and returned.
     *
     * @return the current state or default state value
     */
    @Override
    public final S state() {
        ensureAccessToState();
        S result = state;
        if (result == null) {
            synchronized (this) {
                result = state;
                if (result == null) {
                    state = defaultState();
                    result = state;
                }
            }
        }
        return result;
    }

    /**
     * Ensures that the callee is allowed to access Entity's {@link #state() state()} method.
     *
     * <p>In case the access is prohibited, throws a {@code RuntimeException}.
     *
     * <p>In some scenarios, the state of Entity may be not up-to-date,
     * so descendants of {@code AbstractEntity} are able to put the corresponding restrictions
     * on this method invocation.
     *
     * <p>By default, this method performs no checks,
     * thus allowing to access Entity's {@code state()} at any point of time.
     */
    @Internal
    @SuppressWarnings("NoopMethodInAbstractClass" /* By design. */)
    protected void ensureAccessToState() {
        // Do nothing by default.
    }

    /**
     * Obtains model class for this entity.
     */
    @Internal
    protected EntityClass<?> thisClass() {
        EntityClass<?> result = thisClass;
        if (result == null) {
            synchronized (this) {
                result = thisClass;
                if (result == null) {
                    thisClass = modelClass();
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
    protected EntityClass<?> modelClass() {
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
    protected final S defaultState() {
        @SuppressWarnings("unchecked")
        // cast is safe because this type of messages is saved to the map
        S result = (S) thisClass().defaultState();
        return result;
    }

    /**
     * Updates the state of the entity.
     *
     * <p>The new state must be {@linkplain #validate(EntityState) valid}.
     *
     * @param state
     *         the new state to set
     * @throws InvalidEntityStateException
     *         if the passed state is not {@linkplain #validate(EntityState) valid}
     */
    final void updateState(S state) {
        checkNotNull(state);
        validate(state);
        setState(state);
    }

    /**
     * Verifies the new entity state and returns {@link ConstraintViolation}s, if any.
     *
     * <p>Default implementation uses the {@linkplain Validate#violationsOf(Message) message
     * validation}.
     *
     * @param newState
     *         a state object to replace the current state
     * @return the violation constraints
     */
    protected final List<ConstraintViolation> checkEntityState(S newState) {
        checkNotNull(newState);
        ImmutableList.Builder<ConstraintViolation> violations = ImmutableList.builder();
        violations.addAll(violationsOf(newState));
        violations.addAll(validateChange(state(), newState));
        return violations.build();
    }

    /**
     * Ensures that the passed new state is valid.
     *
     * @param newState
     *         a state object to replace the current state
     * @throws InvalidEntityStateException
     *         if the state is not valid
     * @see #checkEntityState(EntityState)
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
                    stringId = Stringifiers.toString(id());
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
        return lifecycleFlags().getArchived();
    }

    /**
     * Sets {@code archived} status flag to the passed value.
     */
    protected void setArchived(boolean archived) {
        setLifecycleFlags(lifecycleFlags().toBuilder()
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
        return lifecycleFlags().getDeleted();
    }

    /**
     * Sets {@code deleted} status flag to the passed value.
     */
    protected void setDeleted(boolean deleted) {
        setLifecycleFlags(lifecycleFlags().toBuilder()
                                          .setDeleted(deleted)
                                          .build());
    }

    /**
     * Ensures that the entity is not marked as {@code archived}.
     *
     * @throws CannotModifyArchivedEntity
     *         if the entity in the archived status
     * @see #lifecycleFlags()
     * @see io.spine.server.entity.LifecycleFlags#getArchived()
     */
    protected void checkNotArchived() throws CannotModifyArchivedEntity {
        if (lifecycleFlags().getArchived()) {
            Any packedId = Identifier.pack(id());
            throw CannotModifyArchivedEntity
                    .newBuilder()
                    .setEntityId(packedId)
                    .build();
        }
    }

    /**
     * Ensures that the entity is not marked as {@code deleted}.
     *
     * @throws CannotModifyDeletedEntity
     *         if the entity is marked as {@code deleted}
     * @see #lifecycleFlags()
     * @see io.spine.server.entity.LifecycleFlags#getDeleted()
     */
    protected void checkNotDeleted() throws CannotModifyDeletedEntity {
        if (lifecycleFlags().getDeleted()) {
            Any packedId = Identifier.pack(id());
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

    /**
     * Clears the lifecycle flags and their {@linkplain #lifecycleFlags modification flag}.
     */
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
    final void updateState(S state, Version version) {
        updateState(state);
        updateVersion(version);
    }

    /**
     * Obtains the version number of the entity.
     */
    protected int versionNumber() {
        int result = version().getNumber();
        return result;
    }

    final void updateVersion(Version newVersion) {
        checkNotNull(newVersion);
        checkValid(newVersion);
        if (version.equals(newVersion)) {
            return;
        }
        int currentVersionNumber = versionNumber();
        int newVersionNumber = newVersion.getNumber();
        if (currentVersionNumber > newVersionNumber) {
            throw newIllegalArgumentException(
                    "A version with the lower number (%d) passed to `updateVersion()` " +
                            "of the entity `%s` (`%s`) with the version number %d.",
                    newVersionNumber, thisClass(), idAsString(), currentVersionNumber
            );
        }
        setVersion(newVersion);
    }

    /**
     * Updates the state incrementing the version number and recording time of the modification.
     *
     * <p>This is a test-only convenience method. Calling this method is equivalent to calling
     * {@link #updateState(EntityState, Version)} with the incremented by one version.
     *
     * <p>Please use {@link #updateState(EntityState, Version)} directly in the production code.
     *
     * @param newState
     *         a new state to set
     */
    @VisibleForTesting
    void incrementState(S newState) {
        updateState(newState, incrementedVersion());
    }

    void setVersion(Version version) {
        this.version = version;
    }

    private Version incrementedVersion() {
        return Versions.increment(version());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides to simplify implementation of entities implementing
     * {@link io.spine.server.EventProducer}.
     */
    @Override
    public Version version() {
        return version;
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

    @OverridingMethodsMustInvokeSuper
    @Override
    public void beforeInvoke(HandlerMethod<?, ?, ?, ?> method) {
        checkNotNull(method);
        FluentLogger logger = loggerFor(getClass());
        this.handlerLog = new HandlerLog(logger, method);
    }

    @OverridingMethodsMustInvokeSuper
    @Override
    public void afterInvoke(HandlerMethod<?, ?, ?, ?> method) {
        this.handlerLog = null;
    }

    /**
     * Obtains a new fluent logging API at the given level.
     *
     * <p>If called from within a handler method, the resulting log will reference the handler
     * method as the log site. Otherwise, equivalent to
     * {@code Logging.loggerFor(getClass()).at(logLevel)}.
     *
     * @param logLevel
     *         the log level
     * @return new fluent logging API
     * @apiNote This method mirrors the declaration of
     *         {@link io.spine.server.log.LoggingEntity#at(Level)}. It is recommended to implement
     *         the {@link io.spine.server.log.LoggingEntity} interface and use the underscore
     *         logging methods instead of calling {@code at(..)} directly.
     * @see io.spine.server.log.LoggingEntity
     */
    public final FluentLogger.Api at(Level logLevel) {
        return handlerLog != null
               ? handlerLog.at(logLevel)
               : loggerFor(getClass()).at(logLevel);
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
        return Objects.equals(id(), that.id()) &&
                Objects.equals(state(), that.state()) &&
                Objects.equals(version(), that.version()) &&
                Objects.equals(lifecycleFlags(), that.lifecycleFlags());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id(), state(), version(), lifecycleFlags());
    }
}
