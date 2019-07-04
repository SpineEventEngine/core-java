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

package io.spine.server.command;

import io.spine.logging.Logging;
import io.spine.server.commandbus.CommandDispatcher;
import io.spine.server.event.EventBus;

/**
 * The abstract base for non-aggregate classes that dispatch commands to their methods
 * and post resulting events to {@link EventBus}.
 */
public abstract class AbstractCommandDispatcher implements CommandDispatcher<String>, Logging {

    /**
     * Obtains identity string of the dispatcher.
     *
     * <p>Default implementation returns fully-qualified name of the class.
     *
     * @return the string with the handler identity
     */
    public String id() {
        return getClass().getName();
    }

    /**
     * Indicates whether some other command handler is "equal to" this one.
     *
     * <p>Two command handlers are equal if they handle the same set of commands.
     *
     * @return if the passed {@code CommandHandler} handles the same
     * set of command classes.
     * @see #messageClasses()
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AbstractCommandDispatcher)) {
            return false;
        }
        AbstractCommandDispatcher otherHandler = (AbstractCommandDispatcher) o;
        boolean equals = id().equals(otherHandler.id());
        return equals;
    }

    @Override
    public int hashCode() {
        return id().hashCode();
    }
}
