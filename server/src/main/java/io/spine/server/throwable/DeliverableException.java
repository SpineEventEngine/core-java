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

package io.spine.server.throwable;

import io.spine.annotation.Internal;
import io.spine.base.Error;

/**
 * An interface for the {@link Exception} types which may be serialized within a protobuf message.
 *
 * <p>This interface is designed to be implemented in {@link Throwable} types only.
 *
 * @author Dmytro Dashenkov
 */
@SuppressWarnings("NonExceptionNameEndsWithException")
    // The interface is designed to be implemented in Throwable types.
@Internal
public interface DeliverableException {

    /**
     * Converts this {@link Exception} into an {@link Error io.spine.base.Error}.
     */
    Error asError();

    /**
     * Obtains the instance of this exception whose compile-time type is
     * {@link Throwable java.lang.Throwable}.
     */
    Throwable asThrowable();
}
