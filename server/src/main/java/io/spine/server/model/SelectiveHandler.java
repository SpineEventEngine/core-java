/*
 * Copyright 2020, TeamDev. All rights reserved.
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

import com.google.errorprone.annotations.Immutable;
import io.spine.server.type.MessageEnvelope;
import io.spine.type.MessageClass;

/**
 * A handler method which accept only instances of messages that match the filtering conditions.
 *
 * <p>Having two or more methods that accept the same message type but instances of different
 * values to avoid if-elif branches (that filter by the value of the message in a bigger method).
 *
 * <p>It is possible to filter by the same field of the same message type.
 * 
 * @see io.spine.core.ByField
 * @see HandlerFieldFilterClashError
 */
@Immutable
public interface SelectiveHandler<T,
                                  C extends MessageClass<?>,
                                  E extends MessageEnvelope<?, ?, ?>,
                                  R extends MessageClass<?>>
        extends HandlerMethod<T, C, E, R> {

    /**
     * Obtains the filter to apply to the messages received by this method.
     */
    ArgumentFilter filter();
}
