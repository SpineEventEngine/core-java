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

package io.spine.server.model;

import com.google.errorprone.annotations.Immutable;
import io.spine.core.MessageEnvelope;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * An abstract base for those strategies, which define whether a particular method is eligible
 * to be invoked with the given message envelope as an argument.
 *
 * @author Dmytro Dashenkov
 */
@Immutable
public interface MessageAcceptor<E extends MessageEnvelope<?, ?, ?>> {

    /**
     * Invokes the method of an accepting party, using the provided {@code MessageEnvelope}
     * as a source or method argument values.
     *
     * @param target
     *         the object, which method is to be invoked
     * @param method
     *         the method to invoke
     * @param envelope
     *         the envelope to use
     * @return the result of the method call
     * @throws InvocationTargetException
     *         if the called method throws an exception
     * @throws IllegalAccessException
     *         if the method to call is in fact inaccessible due to some reason, such as visibility
     */
    Object invoke(Object target, Method method, E envelope)
            throws InvocationTargetException, IllegalAccessException;
}
