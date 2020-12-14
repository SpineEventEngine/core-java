/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.server.transport;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.spine.annotation.Internal;
import io.spine.base.Error;
import io.spine.grpc.MetadataConverter;
import io.spine.server.MessageError;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.grpc.Status.INVALID_ARGUMENT;

/**
 * Utility class for working with {@link Status}es.
 */
@Internal
public class Statuses {

    /** Prevent instantiation of this utility class. */
    private Statuses() {
    }

    /**
     * Creates an instance of {@code StatusRuntimeException} of status
     * {@code Status.INVALID_ARGUMENT} with the passed cause.
     *
     * <p>Resulting {@code StatusRuntimeException} will contain the passed
     * {@link MessageError} transformed to
     * the {@linkplain StatusRuntimeException#getTrailers() metadata}.
     *
     * @param cause the exception cause
     * @return the constructed {@code StatusRuntimeException}
     */
    public static StatusRuntimeException invalidArgumentWithCause(MessageError cause) {
        checkNotNull(cause);
        return createException(cause.asThrowable(), cause.asError());
    }

    /**
     * Constructs the {@code StatusRuntimeException} with the given cause and error.
     *
     * @see #invalidArgumentWithCause(MessageError)
     */
    private static StatusRuntimeException createException(Throwable cause, Error error) {
        Metadata metadata = MetadataConverter.toMetadata(error);
        StatusRuntimeException result = INVALID_ARGUMENT.withCause(cause)
                                                        .asRuntimeException(metadata);
        return result;
    }
}
