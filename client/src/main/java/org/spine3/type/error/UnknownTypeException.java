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
package org.spine3.type.error;

/**
 * Exception that is thrown when unsupported message is obtained
 * or in case when there is no class for given Protobuf message.
 *
 * @author Mikhail Melnik
 */
public class UnknownTypeException extends RuntimeException {

    private static final long serialVersionUID = 0L;

    private static final String ERR_MSG = "No Java class found for the Protobuf message of type: ";

    /**
     * Creates a new instance with the type name.
     *
     * @param typeName the unknown type
     */
    public UnknownTypeException(String typeName) {
        super(ERR_MSG + typeName);
    }

    /**
     * Creates a new instance with the type name and the cause.
     *
     * @param typeName the unknown type
     * @param cause    the exception cause
     */
    public UnknownTypeException(String typeName, Throwable cause) {
        super(ERR_MSG + typeName, cause);
    }

    /**
     * Creates a new instance when only the cause is known.
     *
     * <p>Use this constructor when propagating
     * {@link com.google.protobuf.InvalidProtocolBufferException InvalidProtocolBufferException}
     * without knowing which type caused the exception (e.g. when calling {@code JsonFormat.print()}.
     */
    public UnknownTypeException(Throwable cause) {
        super(cause);
    }
}
