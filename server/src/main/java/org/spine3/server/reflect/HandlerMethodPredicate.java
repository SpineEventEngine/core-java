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

package org.spine3.server.reflect;

import com.google.protobuf.Message;

import java.lang.reflect.Method;

/**
 * The predicate for filtering message handling methods.
 *
 * @author Alexander Yevsyukov
 */
public abstract class HandlerMethodPredicate extends MethodPredicate {

    /**
     * Returns the context parameter class.
     */
    protected abstract Class<? extends Message> getContextClass();

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean acceptsCorrectParams(Method method) {
        final Class<?>[] paramTypes = method.getParameterTypes();
        final int paramCount = paramTypes.length;
        final boolean isParamCountCorrect = (paramCount == 1) || (paramCount == 2);
        if (!isParamCountCorrect) {
            return false;
        }
        final boolean isFirstParamMsg = Message.class.isAssignableFrom(paramTypes[0]);
        if (paramCount == 1) {
            return isFirstParamMsg;
        } else {
            final Class<? extends Message> contextClass = getContextClass();
            final boolean paramsCorrect = isFirstParamMsg && contextClass.equals(paramTypes[1]);
            return paramsCorrect;
        }
    }
}
