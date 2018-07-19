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

package io.spine.server.rejection;

import com.google.protobuf.Message;
import io.spine.base.Error;
import io.spine.core.RejectionClass;
import io.spine.server.bus.MessageUnhandled;
import io.spine.type.TypeName;

import static com.google.common.base.Throwables.getStackTraceAsString;
import static java.lang.String.format;

/**
 * Exception that is thrown when unhandled rejection is thrown.
 *
 * @author Dmytro Dashenkov
 */
public class UnhandledRejectionException extends RuntimeException implements MessageUnhandled {

    private static final long serialVersionUID = 0L;

    private final Error error;

    public UnhandledRejectionException(Message rejectionMsg) {
        super(msgFormat(rejectionMsg));
        this.error = buildError();
    }

    private static String msgFormat(Message msg) {
        RejectionClass cls = RejectionClass.of(msg);
        String typeName = TypeName.of(msg)
                                  .value();
        String result = format(
                "There is no registered handler for the rejection class: `%s`. Protobuf type: `%s`",
                cls, typeName
        );
        return result;
    }

    @Override
    public Error asError() {
        return error;
    }

    @Override
    public Throwable asThrowable() {
        return this;
    }

    /**
     * Builds an {@link Error io.spine.base.Error} from this exception.
     *
     * <p>This method is called in the constructor; it uses the exception message and stack trace.
     */
    private Error buildError() {
        Error error = Error.newBuilder()
                           .setType(UnhandledRejectionException.class.getCanonicalName())
                           .setMessage(getMessage())
                           .setStacktrace(getStackTraceAsString(this))
                           .build();
        return error;
    }
}
