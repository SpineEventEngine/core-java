/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.server;

import com.google.protobuf.Message;
import org.spine3.base.EventContext;

import java.lang.reflect.Method;

import static com.google.common.base.Throwables.propagate;

/**
 * {@code EventDispatcher} delivers events to handlers.
 *
 * @author Alexander Yevsyukov
 */
public interface EventDispatcher {

    void dispatch(Message event, EventContext context);

    /**
     * Utility class for obtaining reference to {@link #dispatch(Message, EventContext)} methods of implementations.
     */
    class DispatchMethod {

        /**
         * The name of the method used for dispatching events.
         *
         * @see #dispatch(Message, EventContext)
         */
        @SuppressWarnings("DuplicateStringLiteralInspection") // CommandDispatcher has also such method.
        private static final String DISPATCH_METHOD_NAME = "dispatch";

        public static Method of(EventDispatcher dispatcher) {
            try {
                return dispatcher.getClass().getMethod(DISPATCH_METHOD_NAME, Message.class, EventContext.class);
            } catch (NoSuchMethodException e) {
                throw propagate(e);
            }
        }

        private DispatchMethod() {}
    }
}
