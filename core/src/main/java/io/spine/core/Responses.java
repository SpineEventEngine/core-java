/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.core;

import com.google.protobuf.Empty;

/**
 * Utilities for working with {@link Response Response} objects.
 */
public final class Responses {

    private static final Status STATUS_OK = Status.newBuilder()
                                                  .setOk(Empty.getDefaultInstance())
                                                  .build();

    /** The response returned on successful acceptance of a message for processing. */
    private static final Response RESPONSE_OK = Response.newBuilder()
                                                        .setStatus(STATUS_OK)
                                                        .build();
    /** Prevent instantiation of this utility class. */
    private Responses() {
    }

    /** Returns the instance of OK {@link Response}. */
    public static Response ok() {
        return RESPONSE_OK;
    }

    /**
     * Returns the {@code OK} {@link Status} instance.
     */
    public static Status statusOk() {
        return STATUS_OK;
    }

    /**
     * Checks if the response is OK.
     *
     * @return {@code true} if the passed response represents `ok` status,
     * {@code false} otherwise
     */
    public static boolean isOk(Response response) {
        boolean result = response.getStatus()
                                 .getStatusCase() == Status.StatusCase.OK;
        return result;
    }
}
