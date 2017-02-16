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
import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.protobuf.Timestamps.getCurrentTime;

/**
 * An abstract base for entities with versions.
 *
 * The entity keeps only its latest state and version information associated with this state.
 *
 * @param <I> the type of the entity ID
 * @param <S> the type of the entity state
 * @author Alexander Yevsyikov
 * @author Alexander Litus
 */
public abstract class Entity<I, S extends Message>
        extends AnEntityLite<I, S>
        implements IEntity<I, S> {

    @Nullable
    private Timestamp whenModified;

    private int version;

    private EntityStatus status = EntityStatus.getDefaultInstance();

    /**
     * Creates a new instance.
     *
     * @param id the ID for the new instance
     * @throws IllegalArgumentException if the ID is not of one of the supported types for identifiers
     */
    protected Entity(I id) {
        super(id);
        checkNotNull(id);
        Identifiers.checkSupported(id.getClass());
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
    void init() {
        setState(getDefaultState(), 0, getCurrentTime());
        this.status = EntityStatus.getDefaultInstance();
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
            result.setAccessible(true);
            return result;
        } catch (NoSuchMethodException ignored) {
            throw noSuchConstructor(entityClass.getName(), idClass.getName());
        }
    }

    private static IllegalStateException noSuchConstructor(String entityClass, String idClass) {
        final String errMsg = String.format("%s class must declare a constructor with a single %s ID parameter.",
                                            entityClass, idClass);
        return new IllegalStateException(new NoSuchMethodException(errMsg));
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
            result.init();
            return result;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
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
        injectState(state);
        setVersion(version, whenLastModified);
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
     * {@inheritDoc}
     */
    @Override
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

    /**
     * {@inheritDoc}
     * @see #setState(Message, int, Timestamp)
     */
    @Override
    @CheckReturnValue
    public Timestamp whenModified() {
        final Timestamp result = whenModified == null
                                 ? Timestamp.getDefaultInstance()
                                 : whenModified;
        return result;
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
        if (!getStatus().equals(another.getStatus())) {
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
        result = 31 * result + getStatus().hashCode();
        return result;
    }
}
