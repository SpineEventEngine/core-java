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
import com.google.protobuf.Timestamp;
import org.spine3.base.Command;
import org.spine3.base.Version;
import org.spine3.base.Versions;
import org.spine3.server.entity.failure.CannotModifyArchivedEntity;
import org.spine3.server.entity.failure.CannotModifyDeletedEntity;

import javax.annotation.CheckReturnValue;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.spine3.base.Identifiers.idToString;
import static org.spine3.base.Versions.checkIsIncrement;
import static org.spine3.util.Exceptions.newIllegalArgumentException;

/**
 * An abstract base for entities with versions.
 *
 * <p>The entity keeps only its latest state and version information associated with this state.
 *
 * @param <I> the type of the entity ID
 * @param <S> the type of the entity state
 * @author Alexander Yevsyikov
 * @author Alexander Litus
 */
public abstract class AbstractVersionableEntity<I, S extends Message>
        extends AbstractEntity<I, S>
        implements VersionableEntity<I, S>,
                   EntityWithLifecycle<I, S> {

    private Version version;

    private LifecycleFlags lifecycleFlags;

    /**
     * If {@code true} the visibility of the entity was changed since initialization.
     *
     * <p>If so, the visibility status of the entity should be updated when
     * {@linkplain Repository#store(Entity) storing}.
     */
    private volatile boolean lifecycleFlagsChanged;

    /**
     * Creates a new instance.
     *
     * @param id the ID for the new instance
     * @throws IllegalArgumentException if the ID is not of one of the
     *         {@linkplain Entity supported types}
     */
    protected AbstractVersionableEntity(I id) {
        super(id);
        setVersion(Versions.create());
        setVisible();
    }

    /**
     * Sets the object into the default state.
     *
     * <p>Results of this method call are:
     * <ul>
     *   <li>The state object is set to the value produced by {@link #getDefaultState()}.
     *   <li>The version number is set to zero.
     *   <li>The {@link #lifecycleFlags} field is set to the default instance.
     * </ul>
     *
     * <p>This method cannot be called from within {@code Entity} constructor because
     * the call to {@link #getDefaultState()} relies on completed initialization
     * of the instance.
     */
    @Override
    protected void init() {
        super.init();
        injectState(getDefaultState());
        initVersion(Versions.create());
        setVisible();
    }

    private void setVisible() {
        setLifecycleFlags(LifecycleFlags.getDefaultInstance());
        lifecycleFlagsChanged = false;
    }

    /**
     * Updates the state and version of the entity.
     *
     * <p>The new state must be {@linkplain #validate(Message) valid}.
     *
     * <p>The passed version must have a number not less than the current version of the entity.
     *
     * @param state   the state object to set
     * @param version the entity version to set
     * @throws IllegalStateException
     *                if the passed state is not {@linkplain #validate(S) valid}
     * @throws IllegalArgumentException
     *                if the passed version has the number which is greater than the current
     *                version of the entity
     * @see #validate(S)
     */
    protected void updateState(S state, Version version) {
        updateState(state);
        updateVersion(version);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean lifecycleFlagsChanged() {
        return lifecycleFlagsChanged;
    }

    /**
     * Obtains the version number of the entity.
     */
    protected int versionNumber() {
        final int result = getVersion().getNumber();
        return result;
    }

    /**
     * Initializes the entity with the passed version.
     *
     * <p>This method assumes that the entity version is zero.
     * If this is not so, {@code IllegalStateException} will be thrown.
     *
     * <p>One of the usages for this method is for creating an entity instance
     * from a storage.
     *
     * <p>To increment a version of the entity please call {@link #incrementVersion()}.
     *
     * <p>To set a new version which is several numbers ahead please use
     * {@link #advanceVersion(Version)}.
     *
     * @param version the version to set.
     */
    protected void initVersion(Version version) {
        if (versionNumber() > 0) {
            final String errMsg = format(
                    "initVersion() called on an entity with non-zero version number (%d).",
                    versionNumber()
            );
            throw new IllegalStateException(errMsg);
        }
        setVersion(version);
    }

    protected void advanceVersion(Version newVersion) {
        checkNotNull(newVersion);
        checkIsIncrement(this.getVersion(), newVersion);
        setVersion(newVersion);
    }

    private void updateVersion(Version newVersion) {
        checkNotNull(newVersion);
        if (version.equals(newVersion)) {
            return;
        }

        final int currentVersionNumber = versionNumber();
        final int newVersionNumber = newVersion.getNumber();
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
     * @param newState a new state to set
     */
    protected void incrementState(S newState) {
        updateState(newState, incrementedVersion());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Version getVersion() {
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
    protected int incrementVersion() {
        setVersion(incrementedVersion());
        return version.getNumber();
    }

    /**
     * Obtains timestamp of the entity version.
     */
    @CheckReturnValue
    public Timestamp whenModified() {
        return version.getTimestamp();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LifecycleFlags getLifecycleFlags() {
        final LifecycleFlags result = this.lifecycleFlags == null
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
     * @param modification the {@linkplain Command} which execution triggered this check
     * @throws CannotModifyArchivedEntity if the entity in in the archived status
     * @see #getLifecycleFlags()
     * @see LifecycleFlags#getArchived()
     */
    protected void checkNotArchived(Command modification) throws CannotModifyArchivedEntity {
        if (getLifecycleFlags().getArchived()) {
            final String idStr = idToString(getId());
            throw new CannotModifyArchivedEntity(modification.getMessage(),
                                                 modification.getContext(),
                                                 idStr);
        }
    }

    /**
     * Ensures that the entity is not marked as {@code deleted}.
     *
     * @param modification the {@linkplain Command} which execution triggered this check
     * @throws CannotModifyDeletedEntity if the entity is marked as {@code deleted}
     * @see #getLifecycleFlags()
     * @see LifecycleFlags#getDeleted()
     */
    protected void checkNotDeleted(Command modification) throws CannotModifyDeletedEntity {
        if (getLifecycleFlags().getDeleted()) {
            final String idStr = idToString(getId());
            throw new CannotModifyDeletedEntity(modification.getMessage(),
                                                modification.getContext(),
                                                idStr);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AbstractVersionableEntity)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        AbstractVersionableEntity<?, ?> that = (AbstractVersionableEntity<?, ?>) o;
        return Objects.equals(getVersion(), that.getVersion()) &&
               Objects.equals(getLifecycleFlags(), that.getLifecycleFlags());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getVersion(), getLifecycleFlags());
    }
}
