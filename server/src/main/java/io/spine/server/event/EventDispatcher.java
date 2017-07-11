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

package io.spine.server.event;

import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import io.spine.server.bus.MessageDispatcher;

import static io.spine.util.Exceptions.newIllegalArgumentException;

/**
 * {@code EventDispatcher} delivers events to subscribers.
 *
 * @author Alexander Yevsyukov
 */
public interface EventDispatcher extends MessageDispatcher<EventClass, EventEnvelope> {

    /**
     * Utility class for reporting event dispatching errors.
     */
    class Error {

        private Error() {
            // Prevent instantiation of this utility class.
        }

        /**
         * Throws {@link IllegalArgumentException} to report unexpected event passed.
         *
         * @param eventClass the event class which name will be used in the exception message
         * @return nothing ever
         * @throws IllegalArgumentException always
         */
        public static IllegalArgumentException unexpectedEventEncountered(EventClass eventClass)
                throws IllegalArgumentException {
            final String eventClassName = eventClass.value()
                                                    .getName();
            throw newIllegalArgumentException("Unexpected event of class: %s", eventClassName);
        }
    }
}
