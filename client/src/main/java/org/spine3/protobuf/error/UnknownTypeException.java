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
package org.spine3.protobuf.error;

/**
 * Exception that is thrown when unsupported message is obtained
 * or in case there is no class for given Protobuf message.
 *
 * @author Mikhail Melnik
 */
public class UnknownTypeException extends RuntimeException {

    private static final String ERR_MSG = "No Java class found for message type: ";

    /**
     * Creates a new instance.
     *
     * @param typeUrl the unknown type URL
     */
    public UnknownTypeException(String typeUrl) {
        super(ERR_MSG + typeUrl);
    }

    /**
     * Creates a new instance.
     *
     * @param typeUrl the unknown type URL
     * @param cause the exception cause
     */
    public UnknownTypeException(String typeUrl, Throwable cause) {
        super(ERR_MSG + typeUrl, cause);
    }

    private static final long serialVersionUID = 0L;
}
