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
import org.spine3.base.Identifiers;
import org.spine3.base.Stringifiers;
import org.spine3.server.entity.status.CannotModifyArchivedEntity;
import org.spine3.server.entity.status.CannotModifyDeletedEntity;
import org.spine3.server.entity.status.EntityStatus;

import javax.annotation.CheckReturnValue;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.protobuf.Timestamps.getCurrentTime;

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
        extends AbstractEntityLite<I, S>
        implements VersionableEntity<I, S> {

    private final Version version;

    private EntityStatus status = EntityStatus.getDefaultInstance();

    /**
     * Creates a new instance.
     *
     * @param id the ID for the new instance
     * @throws IllegalArgumentException if the ID is not of one of the
     *         {@linkplain Entity supported types}
     */
    protected AbstractVersionableEntity(I id) {
        super(id);
        checkNotNull(id);
        Identifiers.checkSupported(id.getClass());
        this.version = Version.create();
    }

    /**
     * Sets the object into the default state.
     *
     * <p>Results of this method call are:
     * <ul>
     *   <li>The state object is set to the value produced by {@link #getDefaultState()}.
     *   <li>The version number is set to zero.
     *   <li>The {@link #whenModified} field is set to the system time of the call.
     *   <li>The {@link #status} field is set to the default instance.
     * </ul>
     *
     * <p>This method cannot be called from within {@code Entity} constructor because
     * the call to {@link #getDefaultState()} relies on completed initialization
     * of the instance.
     */
    @Override
    protected void init() {
        super.init();
        setState(getDefaultState(), 0, getCurrentTime());
        this.status = EntityStatus.getDefaultInstance();
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
     * @param timestamp the time of the last modification to set
     * @see #validate(S)
     */
    protected void setState(S state, int version, Timestamp timestamp) {
        validate(state);
        injectState(state);
        setVersion(version, timestamp);
    }

    /**
     * Sets status for the entity.
     */
    void setStatus(EntityStatus status) {
        this.status = status;
    }

    /**
     * Sets version information of the entity.
     *
     * @param number the version number of the entity
     * @param timestamp the time of the last modification of the entity
     */
    protected void setVersion(int number, Timestamp timestamp) {
        checkNotNull(timestamp);
        version.copyFrom(Version.of(number, timestamp));
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
     * {@inheritDoc}
     */
    @Override
    public Version getVersion() {
        return version;
    }

    /**
     * Advances the current version by one and records the time of the modification.
     *
     * @return new version number
     */
    protected int incrementVersion() {
        version.increment();
        return version.getNumber();
    }

    /**
     * {@inheritDoc}
     * @see #setState(Message, int, Timestamp)
     */
    @CheckReturnValue
    public Timestamp whenModified() {
        return version.getTimestamp();
    }

    /**
     * Obtains the entity status.
     */
    protected EntityStatus getStatus() {
        final EntityStatus result = this.status ==  null
                ? EntityStatus.getDefaultInstance()
                : this.status;
        return result;
    }

    /**
     * Tests whether the entity is marked as archived.
     *
     * @return {@code true} if the entity is archived, {@code false} otherwise
     */
    protected boolean isArchived() {
        return getStatus().getArchived();
    }

    /**
     * Sets {@code archived} status flag to the passed value.
     */
    protected void setArchived(boolean archived) {
        this.status = getStatus().toBuilder()
                                 .setArchived(archived)
                                 .build();
    }

    /**
     * Tests whether the entity is marked as deleted.
     *
     * @return {@code true} if the entity is deleted, {@code false} otherwise
     */
    protected boolean isDeleted() {
        return getStatus().getDeleted();
    }

    /**
     * Sets {@code deleted} status flag to the passed value.
     */
    protected void setDeleted(boolean deleted) {
        this.status = getStatus().toBuilder()
                                 .setDeleted(deleted)
                                 .build();
    }

    /**
     * Ensures that the entity is not marked as {@code archived}.
     *
     * @throws CannotModifyArchivedEntity if the entity in in the archived status
     * @see #getStatus()
     * @see EntityStatus#getArchived()
     */
    protected void checkNotArchived() throws CannotModifyArchivedEntity {
        if (getStatus().getArchived()) {
            final String idStr = Stringifiers.idToString(getId());
            throw new CannotModifyArchivedEntity(idStr);
        }
    }

    /**
     * Ensures that the entity is not marked as {@code deleted}.
     *
     * @throws CannotModifyDeletedEntity if the entity is marked as {@code deleted}
     * @see #getStatus()
     * @see EntityStatus#getDeleted()
     */
    protected void checkNotDeleted() throws CannotModifyDeletedEntity {
        if (getStatus().getDeleted()) {
            final String idStr = Stringifiers.idToString(getId());
            throw new CannotModifyDeletedEntity(idStr);
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
               Objects.equals(getStatus(), that.getStatus());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getVersion(), getStatus());
    }
}
