/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.server.event;

import com.google.protobuf.Message;
import io.spine.core.EventContext;
import io.spine.server.model.HandlerMethodPredicate;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import static io.spine.core.Rejections.isRejection;

/**
 * Abstract base for methods that accept an event message as the first parameter, and
 * {@link EventContext} as the second optional parameter.
 *
 * @author Alexander Yevsyukov
 */
abstract class EventMethodPredicate extends HandlerMethodPredicate<EventContext> {

    EventMethodPredicate(Class<? extends Annotation> annotationClass) {
        super(annotationClass, EventContext.class);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Filters out methods that accept rejection messages as the first parameter.
     */
    @Override
    protected boolean verifyParams(Method method) {
        if (super.verifyParams(method)) {
            @SuppressWarnings("unchecked") // The case is safe since super returned `true`.
            final Class<? extends Message> firstParameter =
                    (Class<? extends Message>) method.getParameterTypes()[0];
            final boolean isRejection = isRejection(firstParameter);
            return !isRejection;
        }
        return false;
    }
}
