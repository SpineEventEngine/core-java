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
import org.spine3.base.CommandContext;
import org.spine3.base.EventRecord;

import java.lang.reflect.Method;
import java.util.List;

import static com.google.common.base.Throwables.propagate;

/**
 * {@code CommandDispatcher} delivers commands to command handlers and returns results
 * of their execution.
 *
 * @author Alexander Yevsyukov
 */
public interface CommandDispatcher {

    /**
     * The name of the method used for dispatching commands to aggregate roots.
     * <p/>
     * <p>This constant is used for obtaining {@code Method} instance via reflection.
     *
     * @see #dispatch(Message, CommandContext)
     */
    @SuppressWarnings("DuplicateStringLiteralInspection")
    String DISPATCH_METHOD_NAME = "dispatch";

    /**
     * Dispatches the command for processing and returns the generated events.
     *
     * @param command the command to dispatch
     * @param context context info of the command
     * @return a list of event records generated during the command execution, or
     *         an empty list if no events were generated
     * @throws Exception if an exception occurs during command execution
     * @throws FailureThrowable if a business failure occurred during the command execution
     */
    List<EventRecord> dispatch(Message command, CommandContext context) throws Exception, FailureThrowable;

    /**
     * Utility class for obtaining the reference to the dispatch method of the implementations.
     */
    @SuppressWarnings("UtilityClass")
    class DispatchMethod {

        /**
         * Obtains the reference to the {@link #dispatch(Message, CommandContext)} method of the implementation
         * @param dispatcher the instance of the dispatcher
         * @return {@code Method} instance
         */
        public static Method of(CommandDispatcher dispatcher) {
            try {
                return dispatcher.getClass().getMethod(DISPATCH_METHOD_NAME, Message.class, CommandContext.class);
            } catch (NoSuchMethodException e) {
                throw propagate(e);
            }
        }

        private DispatchMethod() {
        }
    }
}
