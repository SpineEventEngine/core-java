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

package io.spine.server.event.model;

import com.google.errorprone.annotations.Immutable;
import io.spine.server.model.DispatchKey;
import io.spine.server.model.HandlerMethod;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;
import io.spine.type.MessageClass;

/**
 * A handler method that handles rejections.
 */
@Immutable
interface RejectionHandler<T, R extends MessageClass<?>>
        extends HandlerMethod<T, EventClass, EventEnvelope, R> {

    /**
     * Obtains the specification of parameters for this method.
     */
    EventAcceptingMethodParams parameterSpec();

    /** Tells whether the handler method is for a rejection. **/
    default boolean handlesRejection() {
        return parameterSpec().acceptsCommand();
    }

    /**
     * {@inheritDoc}
     *
     * <p>If a handler method spec is for a rejection — creates rejection dispatch key.
     */
    @Override
    default DispatchKey key() {
        if (handlesRejection()) {
            DispatchKey dispatchKey = RejectionDispatchKeys.of(messageClass(), rawMethod());
            return dispatchKey;
        }
        return HandlerMethod.super.key();
    }
}
