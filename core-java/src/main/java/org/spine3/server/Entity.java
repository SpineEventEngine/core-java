/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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

package org.spine3.server;

import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

import static com.google.protobuf.util.TimeUtil.getCurrentTime;

/**
 * A server-side wrapper for message objects with identity stored by a repository.
 *
 * @param <I> the type of object IDs
 * @param <M> the type of object states.
 */
public abstract class Entity<I, M extends Message> {

    private final I id;

    @Nullable
    private M state;

    @Nullable
    private Timestamp whenModified;

    private int version;

    protected Entity(I id) {
        this.id = id;
    }

    @CheckReturnValue
    protected abstract M getDefaultState();

    /**
     * @return the current state object or {@code null} if the state wasn't set
     */
    @CheckReturnValue
    @Nullable
    public M getState() {
        return state;
    }

    /**
     * Validates the passed state.
     * <p/>
     * Does nothing by default. Aggregate roots may override this method to
     * specify logic of validating initial or intermediate state of the root.
     *
     * @param state a state object to replace the current state
     * @throws IllegalStateException if the state is not valid
     */
    @SuppressWarnings({"NoopMethodInAbstractClass", "UnusedParameters"})
    // Have this no-op method to prevent enforcing implementation in all sub-classes.
    protected void validate(M state) throws IllegalStateException {
        // Do nothing by default.
    }

    /**
     * Validates and sets the state.
     *
     * @param state the state object to set
     * @see #validate(M)
     */
    protected void setState(M state, int version, Timestamp whenLastModified) {
        validate(state);
        this.state = state;
        this.version = version;
        this.whenModified = whenLastModified;
    }

    /**
     * Updates the state incrementing the version number and recording time of the modification.
     *
     * @param newState a new state to set
     */
    protected void incrementState(M newState) {
        setState(newState, incrementVersion(), getCurrentTime());
    }

    /**
     * Sets the object into the default state.
     * <p/>
     * Results of this method call are:
     * <ul>
     *   <li>The state object is set to the value produced by {@link #getDefaultState()}.</li>
     *   <li>The version number is set to zero.</li>
     *   <li>The {@link #whenModified} field is set to the system time of the call.</li>
     * </ul>
     * <p/>
     * The timestamp is set to current system time.
     */
    protected void setDefault() {
        setState(getDefaultState(), 0, getCurrentTime());
    }

    /**
     * @return current version number
     */
    @CheckReturnValue
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
     * @return the timestamp instance or {@code null} if the state wasn't set
     * @see #setState(Message, int, Timestamp)
     */
    @CheckReturnValue
    @Nullable
    public Timestamp whenModified() {
        return this.whenModified;
    }
}
