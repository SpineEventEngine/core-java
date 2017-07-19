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

package io.spine.core;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.protobuf.AnyPacker;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utility class with utilities for this package.
 *
 * @author Alexander Yevsyukov
 */
class Messages {

    private Messages() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Extracts a message if the passed instance is an outer object or {@link Any},
     * otherwise returns the passed message.
     */
    @SuppressWarnings("ChainOfInstanceofChecks")
    static Message ensureMessage(Message outerOrMessage) {
        final Message input = checkNotNull(outerOrMessage);
        Message msg;

        //TODO:2017-07-19:alexander.yevsyukov: Replace this with default MessageClass behaviour in Java 8.
        if (input instanceof Command) {
            msg = Commands.getMessage((Command) outerOrMessage);
        } else if (input instanceof Event) {
            msg = Events.getMessage((Event) outerOrMessage);
        } else if (input instanceof Failure) {
            msg = Failures.getMessage((Failure) outerOrMessage);
        } else if (input instanceof Any) {
            msg = AnyPacker.unpack((Any) outerOrMessage);
        } else {
            msg = input;
        }
        return msg;
    }
}
