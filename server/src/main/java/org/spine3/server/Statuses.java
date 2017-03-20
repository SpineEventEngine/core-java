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

package org.spine3.server;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.spine3.annotations.Internal;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utility class for working with {@link Status}es.
 *
 * @author Alexander Yevsyukov
 */
@Internal
public class Statuses {

    private Statuses() {
    }

    /**
     * Creates an instance of {@code StatusRuntimeException} of status {@code Status.INVALID_ARGUMENT}
     * with the passed exception.
     */
    public static StatusRuntimeException invalidArgumentWithCause(Exception exception) {
        checkNotNull(exception);
        final StatusRuntimeException result = Status.INVALID_ARGUMENT
                .withCause(exception)
                .asRuntimeException();
        return result;
    }
}
