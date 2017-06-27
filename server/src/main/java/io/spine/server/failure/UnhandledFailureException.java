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

package io.spine.server.failure;

import com.google.protobuf.Message;
import io.spine.base.Error;
import io.spine.core.FailureClass;
import io.spine.server.bus.MessageUnhandled;
import io.spine.type.TypeName;
import io.spine.util.Exceptions;

import static java.lang.String.format;

/**
 * Exception that is thrown when unhandled failure is thrown.
 *
 * @author Dmytro Dashenkov
 */
public class UnhandledFailureException extends RuntimeException implements MessageUnhandled {

    private static final long serialVersionUID = 0L;

    public UnhandledFailureException(Message failureMsg) {
        super(msgFormat(failureMsg));
    }

    private static String msgFormat(Message msg) {
        final FailureClass cls = FailureClass.of(msg);
        final String typeName = TypeName.of(msg).value();
        final String result = format(
                "There is no registered handler for the failure class: `%s`. Protobuf type: `%s`",
                cls, typeName
        );
        return result;
    }

    @Override
    public Error asError() {
        return Exceptions.toError(this);
    }

    @Override
    public Throwable asThrowable() {
        return this;
    }
}
