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

package org.spine3.server.internal;

import com.google.common.base.Predicate;
import org.spine3.Internal;

import java.lang.reflect.Method;

/**
 * The interface for classes that can declare command handling methods.
 *
 * @author Alexander Yevsyukov
 */
@Internal
public interface CommandHandler {

    //TODO:2016-01-24:alexander.yevsyukov: We cannot keep this interface @Internal because there can be cases
    // when custom logic (which doesn't fall into classes provided by the framework) would be needed.
    // We can have AbstractCommandHandler implementation, which would implement the below methods OR
    // we need to move them out of the exposed API.
    // Having the common interface (event if it would be marker interface) for command handlers is
    // beneficial because it would simplify finding all command handlers in an app.
    // The same applies to event handlers.

    /**
     * Creates a method wrapper, which holds reference to this object and the passed method.
     */
    CommandHandlerMethod createMethod(Method method);

    /**
     * Returns the predicate for filtering command handling methods.
     */
    Predicate<Method> getHandlerMethodPredicate();
}
