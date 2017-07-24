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

package io.spine.server.reflect;

import com.google.protobuf.Message;
import io.spine.core.CommandContext;
import io.spine.core.Subscribe;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A wrapper of the Failure subscriber method aware of both the Command message and
 * the {@link CommandContext}.
 *
 * <p>The signature of such a method is following:
 * <pre>
 *     {@code
 *     {@link Subscribe {@literal @}Subscribe}
 *     public void on(FailureMessage failure, CommandMessage command, CommandContext context);
 *     }
 * </pre>
 *
 * @author Dmytro Dashenkov
 */
final class CommandAwareRejectionSubscriberMethod extends RejectionSubscriberMethod {

    /**
     * Creates a new instance to wrap {@code method} on {@code target}.
     *
     * @param method subscriber method
     */
    CommandAwareRejectionSubscriberMethod(Method method) {
        super(method);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Invokes the wrapped {@link Method} upon all the passed params as follows:
     * {@code invoke(target, failureMessage, commandMessage, context)}.
     */
    @Override
    protected void doInvoke(Object target, Message rejectionMessage, CommandContext context,
                            Message commandMessage) throws IllegalArgumentException,
                                                           IllegalAccessException,
                                                           InvocationTargetException {
        getMethod().invoke(target, rejectionMessage, commandMessage, context);
    }
}
