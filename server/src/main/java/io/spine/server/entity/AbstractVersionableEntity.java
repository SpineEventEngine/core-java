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
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.spine.core.Version;
import io.spine.core.Versions;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalArgumentException;

/**
 * An abstract base for entities with versions.
 *
 * <p>The entity keeps only its latest state and version information associated with this state.
 *
 * @param <I> the type of the entity ID
 * @param <S> the type of the entity state
 */
public abstract class AbstractVersionableEntity<I, S extends Message>
        extends AbstractEntity<I, S>
        implements VersionableEntity<I, S> {

    private Version version;

    /**
     * Creates a new instance.
     *
     * <p>Upon construction the entity has:
     * <ul>
     *     <li>The version number is set to zero.
     *     <li>{@linkplain #getLifecycleFlags() Lifecycle flag} are cleared.
     * </ul>
     *
     * @param id the ID for the new instance
     * @throws IllegalArgumentException if the ID is not of one of the
     *         {@linkplain Entity supported types}
     */
    protected AbstractVersionableEntity(I id) {
        super(id);
        setVersion(Versions.zero());
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
     * {@link #updateState(Message, Version)} with the incremented by one version.
     *
     * <p>Please use {@link #updateState(Message, Version)} directly in the production code.
     *
     * @param newState a new state to set
     */
    @VisibleForTesting
    void incrementState(S newState) {
        updateState(newState, incrementedVersion());
    }

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
