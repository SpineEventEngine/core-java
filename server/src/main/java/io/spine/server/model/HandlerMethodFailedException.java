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

package io.spine.server.model;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Message;

import static com.google.common.base.Preconditions.checkNotNull;

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
