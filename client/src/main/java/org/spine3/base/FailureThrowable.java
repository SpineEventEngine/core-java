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

package org.spine3.base;

import com.google.common.base.Throwables;
import com.google.protobuf.Any;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.annotations.Internal;

import static org.spine3.protobuf.AnyPacker.pack;
import static org.spine3.time.Time.getCurrentTime;

/**
 * Abstract base for throwable business failures.
 *
 * @author Alexander Yevsyukov
 */
public abstract class FailureThrowable extends Throwable {

    private static final long serialVersionUID = 0L;

    /**
     * For the {@code failureMessage} and {@code commandMessage} we accept GeneratedMessage
     * (instead of Message) because generated messages implement Serializable.
     */
    private final GeneratedMessageV3 failureMessage;

    /** The moment of creation of this object. */
    private final Timestamp timestamp;

    protected FailureThrowable(GeneratedMessageV3 failureMessage) {
        super();
        this.failureMessage = failureMessage;
        this.timestamp = getCurrentTime();
    }

    public Message getFailureMessage() {
        return failureMessage;
    }

    /**
     * Returns timestamp of the failure message creation.
     */
    public Timestamp getTimestamp() {
        return timestamp;
    }

    /**
     * Converts this {@code FailureThrowable} into {@link Failure}.
     *
     * @param command the command which caused the failure
     */
    @Internal
    public Failure toFailure(Command command) {
        final Any packedMessage = pack(failureMessage);
        final FailureContext context = createContext(command);
        final FailureId id = Failures.generateId();
        return Failure.newBuilder()
                      .setId(id)
                      .setMessage(packedMessage)
                      .setContext(context)
                      .build();
    }

    private FailureContext createContext(Command command) {
        final String stacktrace = Throwables.getStackTraceAsString(this);
        return FailureContext.newBuilder()
                             .setTimestamp(timestamp)
                             .setStacktrace(stacktrace)
                             .setCommand(command)
                             .build();
    }
}
