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

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.protobuf.Messages;

import javax.annotation.CheckReturnValue;

import static com.google.protobuf.util.TimeUtil.getCurrentTime;

/**
 * A business object stored by a repository.
 *
 * @param <I> the type of object IDs
 * @param <S> the type of object states.
 */
public abstract class StoredObject<I extends Message, S extends Message> {

    //TODO:2015-09-07:alexander.yevsyukov: Rename this class to Entity?

    private final I id;
    private final Any idAsAny;

    private S state;
    private Timestamp whenLastModified = getCurrentTime();
    private int version = 0;

    protected StoredObject(I id) {
        this.id = id;
        this.idAsAny = Messages.toAny(id);
    }

    @CheckReturnValue
    protected abstract S getDefaultState();

    @CheckReturnValue
    public S getState() {
        return state;
    }

    /**
     * Validates the passed state.
     * <p>
     * Does nothing by default. Aggregate roots may override this method to
     * specify logic of validating initial or intermediate state of the root.
     *
     * @param state a state object to replace the current state
     * @throws IllegalStateException if the state is not valid
     */
    @SuppressWarnings({"NoopMethodInAbstractClass", "UnusedParameters"})
    // Have this no-op method to prevent enforcing implementation in all sub-classes.
    protected void validate(S state) throws IllegalStateException {
        // Do nothing by default.
    }

    protected void setState(S state) {
        validate(state);
        this.state = state;
    }

    /**
     * @return current version number of the aggregate.
     */
    @CheckReturnValue
    public int getVersion() {
        return version;
    }

    protected int incrementVersion() {
        ++version;
        whenLastModified = getCurrentTime();

        return version;
    }

    protected void setVersion(int version) {
        this.version = version;
    }

    protected void setWhenLastModified(Timestamp whenLastModified) {
        this.whenLastModified = whenLastModified;
    }

    @CheckReturnValue
    public I getId() {
        return id;
    }

    public Any getIdAsAny() {
        return idAsAny;
    }

    @CheckReturnValue
    public Timestamp whenLastModified() {
        return this.whenLastModified;
    }
}
