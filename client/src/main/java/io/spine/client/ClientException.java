/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.client;

import io.spine.base.Error;
import io.spine.core.Status;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

public class ClientException extends RuntimeException {

    private static final long serialVersionUID = 0L;

    private final Error error;

    public ClientException(Error error) {
        super(message(checkNotNull(error)));
        this.error = error;
    }

    private static String message(Error error) {
        String errorMessage = format("%s: %s%n%s",
                                     error.getType(),
                                     error.getMessage(),
                                     error.getAttributesMap());
        return errorMessage;
    }

    public static void checkNoError(Status status) {
        if (status.hasError()) {
            throw new ClientException(status.getError());
        }
    }

    public final Error error() {
        return error;
    }
}
