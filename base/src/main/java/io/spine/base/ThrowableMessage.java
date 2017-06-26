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
package io.spine.base;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;

import static io.spine.time.Time.getCurrentTime;

/**
 * A {@code Throwable}, which state is a {@link Message}.
 *
 * <p>Typically used to signalize about a business failure, occurred in a system. In which case
 * the {@code message} thrown is a detailed description of the failure reason.
 *
 * @author Alex Tymchenko
 */
public abstract class ThrowableMessage extends Throwable {

    private static final long serialVersionUID = 0L;

    /**
     * We accept GeneratedMessage (instead of Message) because generated messages
     * implement {@code Serializable}.
     */
    private final GeneratedMessageV3 message;

    /** The moment of creation of this object. */
    private final Timestamp timestamp;

    protected ThrowableMessage(GeneratedMessageV3 message) {
        super();
        this.message = message;
        this.timestamp = getCurrentTime();
    }

    public Message getMessageThrown() {
        return message;
    }

    /**
     * Returns timestamp of the failure message creation.
     */
    public Timestamp getTimestamp() {
        return timestamp;
    }
}
