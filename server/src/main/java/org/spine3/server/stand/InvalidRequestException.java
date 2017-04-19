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
package org.spine3.server.stand;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Message;
import org.spine3.base.Error;

/**
 * A base class for exceptions fired in case an invalid request
 * has been submitted to {@linkplain Stand}.
 *
 * @author Alex Tymchenko
 */
public class InvalidRequestException extends RuntimeException {

    private static final long serialVersionUID = 0L;

    private final GeneratedMessageV3 request;
    private final Error error;

    /**
     * Creates a new instance.
     *
     * @param messageText an error message text
     * @param request     a related actor request
     * @param error       an error occurred
     */
    InvalidRequestException(String messageText,
                            GeneratedMessageV3 request,
                            Error error) {
        super(messageText);
        this.request = request;
        this.error = error;
    }

    /**
     * Obtains an original request, which caused the {@code error}.
     */
    public Message getRequest() {
        return request;
    }

    /**
     * Obtains the error occurred in response to the invalid request.
     */
    public Error getError() {
        return error;
    }
}
