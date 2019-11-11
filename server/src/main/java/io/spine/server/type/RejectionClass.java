/*
 * Copyright 2019, TeamDev. All rights reserved.
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
package io.spine.server.type;

import com.google.protobuf.Message;
import io.spine.base.RejectionMessage;
import io.spine.base.ThrowableMessage;
import io.spine.core.Event;
import io.spine.type.MessageClass;

import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.core.Events.ensureMessage;
import static io.spine.util.Exceptions.illegalStateWithCauseOf;

/**
 * A value object holding a class of a business rejection.
 */
public final class RejectionClass extends MessageClass<RejectionMessage> {

    private static final long serialVersionUID = 0L;
    /**
     * The name of the {@link ThrowableMessage#messageThrown()} method.
     *
     * @see #from(Class)
     */
    private static final String MESSAGE_THROWN_METHOD = "messageThrown";

    private RejectionClass(Class<? extends RejectionMessage> value) {
        super(value);
    }

    /**
     * Creates a new instance of the rejection class.
     *
     * @param value
     *         a value to hold
     * @return new instance
     */
    public static RejectionClass of(Class<? extends RejectionMessage> value) {
        return new RejectionClass(checkNotNull(value));
    }

    /**
     * Creates a new instance of the rejection class by passed rejection instance.
     *
     * <p>If an instance of {@link Event} (which also implements {@code Message}) is
     * passed to this method, enclosing rejection message will be un-wrapped to determine
     * the class of the rejection.
     *
     * @param rejectionOrMessage
     *         a rejection instance
     * @return new instance
     */
    public static RejectionClass of(Message rejectionOrMessage) {
        RejectionMessage message = (RejectionMessage) ensureMessage(rejectionOrMessage);
        RejectionClass result = of(message.getClass());
        return result;
    }

    /**
     * Creates a new instance from the given {@code ThrowableMessage}.
     */
    public static RejectionClass of(ThrowableMessage rejection) {
        RejectionMessage rejectionMessage = rejection.messageThrown();
        return of(rejectionMessage);
    }

    /**
     * Obtains the class of a rejection by the class of corresponding throwable message.
     */
    public static RejectionClass from(Class<? extends ThrowableMessage> cls) {
        Method messageThrownMethod;
        try {
            messageThrownMethod = cls.getMethod(MESSAGE_THROWN_METHOD);
        } catch (NoSuchMethodException e) {
            throw illegalStateWithCauseOf(e);
        }
        @SuppressWarnings("unchecked") // Safe as declared by `ThrowableMessage.messageThrown`.
        Class<? extends RejectionMessage> returnType =
                (Class<? extends RejectionMessage>) messageThrownMethod.getReturnType();
        return of(returnType);
    }
}
