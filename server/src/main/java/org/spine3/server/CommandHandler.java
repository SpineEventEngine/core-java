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

import com.google.common.base.Predicate;
import org.spine3.Internal;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

import static org.spine3.server.internal.CommandHandlerMethod.*;

/**
 * The interface for classes that have command handling methods.
 *
 * <p>A command handler is responsible for:
 * <ol>
 *     <li>Changing the state of the business model</li>
 *     <li>Producing corresponding events.</li>
 * </ol>
 *
 * <p>Unlike {@code CommandHandler} a {@link CommandDispatcher} is responsible for
 * delivering a command to its handler.
 *
 * @author Alexander Yevsyukov
 * @see CommandDispatcher
 */
public interface CommandHandler {

    //TODO:2016-01-24:alexander.yevsyukov: We cannot keep this interface @Internal because there can be cases
    // when custom logic (which doesn't fall into classes provided by the framework) would be needed.
    //
    // Having the common interface (event if it would be marker interface) for command handlers is
    // beneficial because it would simplify finding all command handlers in an app.
    // The same applies to event handlers.
    //
    // The methods are annotated as @Internal temporarily. We need to move them out of the interface.

    /**
     * Returns the predicate for filtering command handling methods.
     */
    @Internal
    Predicate<Method> getHandlerMethodPredicate();

    class MethodPredicate implements Predicate<Method> {

        @Override
        public boolean apply(@Nullable Method method) {
            //noinspection SimplifiableIfStatement
            if (method == null) {
                return false;
            }
            return isAnnotatedCorrectly(method)
                    && acceptsCorrectParams(method)
                    && returnsMessageListOrVoid(method);
        }
    }

    Predicate<Method> METHOD_PREDICATE = new MethodPredicate();
}
