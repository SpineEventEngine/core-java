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

package io.spine.server.model;

import com.google.protobuf.Any;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Message;
import io.spine.base.Identifier;
import io.spine.base.ThrowableMessage;
import io.spine.server.EventProducer;
import io.spine.server.entity.Entity;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.getRootCause;

/**
 * Signals that invocation of a message handling method failed with an exception.
 *
 * @author Alexander Yevsyukov
 */
public class HandlerMethodFailedException extends RuntimeException {

    private static final long serialVersionUID = 0L;
    
    private final String target;
    private final GeneratedMessageV3 dispatchedMessage;
    private final GeneratedMessageV3 messageContext;

    /**
     * Creates new instance.
     *
     * <p>If the root cause of the exception was thrown by a handler method,
     * {@linkplain ThrowableMessage#initProducer(Any) sets the identity} of the object which thrown.
     *
     * @param target             the object which method failed
     * @param dispatchedMessage  the message passed to the method which failed
     * @param messageContext     the context of the message
     * @param cause              the exception thrown by the method
     */
    public HandlerMethodFailedException(Object  target,
                                        Message dispatchedMessage,
                                        Message messageContext,
                                        Exception cause) {
        super(checkNotNull(cause));
        this.target = target.toString();
        /**
           All messages we handle are generated, so the cast below is safe.
           We do not want to accept `GeneratedMessageV3` to avoid the cast in the calling code
           which uses `Message` as {@linkplain GeneratedMessageV3 advised} by Protobuf authors.
         */
        this.dispatchedMessage = (GeneratedMessageV3) checkNotNull(dispatchedMessage);
        this.messageContext = (GeneratedMessageV3) checkNotNull(messageContext);
        setProducer(cause, target);
    }

    /**
     * If the root cause of the exception was thrown by a handler method,
     * {@linkplain ThrowableMessage#initProducer(Any) sets the identity} of the object which thrown.
     */
    private static void setProducer(Exception exception, Object target) {
        Throwable rootCause = getRootCause(exception);
        if (rootCause instanceof ThrowableMessage) {
            ThrowableMessage thrownMessage = (ThrowableMessage) rootCause;
            Any producerId = idOf(target);
            thrownMessage.initProducer(producerId);
        }
    }

    /**
     * Obtains an identity of an object which threw {@link ThrowableMessage}.
     *
     * @implNote Attempts to cast the passed object to {@link Entity} or {@link EventProducer}.
     * If none of this works, returns the result of {@link Object#toString()}.
     */
    @SuppressWarnings("ChainOfInstanceofChecks")
    private static Any idOf(Object target) {
        if (target instanceof Entity) {
            Object entityId = ((Entity) target).id();
            return Identifier.pack(entityId);
        }

        if (target instanceof EventProducer) {
            return ((EventProducer) target).producerId();
        }

        return Identifier.pack(target.toString());
    }

    /**
     * Obtains the string {@linkplain Object#toString() identity} of the object which method failed.
     */
    public String getTarget() {
        return target;
    }

    /**
     * Obtains the message which was passed to the failed method.
     */
    public Message getDispatchedMessage() {
        return dispatchedMessage;
    }

    /**
     * Obtains the context of the message passed to the failed method.
     */
    public Message getMessageContext() {
        return messageContext;
    }
}
