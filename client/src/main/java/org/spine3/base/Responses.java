/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

import com.google.protobuf.Empty;

/**
 * Utilities for working with {@link org.spine3.base.Response} objects.
 *
 * @author Alexander Yevsyukov
 */
public class Responses {

    private Responses() {
    }

    /**
     * The response returned on successful acceptance of a command for processing.
     */
    private static final Response RESPONSE_OK = Response.newBuilder()
            .setOk(Empty.getDefaultInstance())
            .build();

    /**
     * @return the instance of OK {@link Response}
     */
    public static Response ok() {
        return RESPONSE_OK;
    }

    /**
     * @return {@code true} if the passed response represents `ok` status,
     *         {@code false} otherwise
     */
    public static boolean isOk(Response response) {
        final boolean result = response.getStatusCase() == Response.StatusCase.OK;
        return result;
    }

    /**
     * @return {@code true} if the passed response represents `unsupported command` error,
     *         {@code false} otherwise
     */
    public static boolean isUnsupportedCommand(Response response) {
        if (response.getStatusCase() == Response.StatusCase.ERROR) {
            final Error error = response.getError();
            final boolean isUnsupported = error.getCode() == CommandValidationError.UNSUPPORTED_COMMAND.getNumber();
            return isUnsupported;
        }
        return false;
    }
}
