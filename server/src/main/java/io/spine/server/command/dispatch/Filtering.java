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

package io.spine.server.command.dispatch;

import com.google.protobuf.Empty;
import com.google.protobuf.Message;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toList;

/**
 * Performs filtering of the events emitted during {@link Dispatch dispatch}.
 *
 * @author Mykhailo Drachuk
 */
class Filtering {

    private final Collection<? extends Message> messages;

    /**
     * A private constructor preventing instantiation outside of {@link #of(Collection) factory
     * method}.
     *
     * @param messages event messages which were emitted by a command handling entity and
     *                 require filtering
     */
    private Filtering(Collection<? extends Message> messages) {
        this.messages = messages;
    }

    /**
     * Performs the filtering of the event messages.
     *
     * @return provided messages with an exception of those not passing filter
     */
    List<? extends Message> perform() {
        return messages.stream()
                       .filter(nonEmpty())
                       .collect(toList());
    }

    /**
     * @param messages event messages which were emitted by a command handling entity and
     *                 require filtering
     * @return an instance of an {@link Filtering event filter} processing provided messages
     */
    static Filtering of(Collection<? extends Message> messages) {
        return new Filtering(messages);
    }

    /**
     * @return an instance of a {@linkplain NonEmpty non-empty predicate}
     */
    private static Predicate<Message> nonEmpty() {
        return NonEmpty.PREDICATE;
    }

    /**
     * A predicate checking that message is not {@linkplain Empty empty}.
     */
    private enum NonEmpty implements Predicate<Message> {

        PREDICATE;

        private static final Empty EMPTY = Empty.getDefaultInstance();

        /**
         * Checks that message is not {@linkplain Empty empty}.
         *
         * @param message the message being checked
         * @return {@code true} if the message is not empty, {@code false} otherwise
         */
        @Override
        public boolean test(Message message) {
            return !message.equals(EMPTY);
        }
    }
}
