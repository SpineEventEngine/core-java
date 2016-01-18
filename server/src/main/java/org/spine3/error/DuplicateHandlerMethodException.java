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
package org.spine3.error;

import com.google.protobuf.Message;

/**
 * Indicates that more than one handling method for the same message class are present in the declaring class.
 *
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
public class DuplicateHandlerMethodException extends RuntimeException {

    public DuplicateHandlerMethodException(
            Class<?> targetClass,
            Class<? extends Message> messageClass,
            String firstMethodName,
            String secondMethodName) {

        super(String.format(
                "The %s class defines more than one method for handling the message class %s." +
                        " Methods encountered: %s, %s.",
                targetClass.getName(), messageClass.getName(),
                firstMethodName, secondMethodName));
    }

    private static final long serialVersionUID = 0L;

}
