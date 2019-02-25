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

package io.spine.server.bus.given;

import com.google.common.collect.ImmutableSet;
import io.spine.server.bus.MulticastDispatcher;
import io.spine.server.type.MessageEnvelope;
import io.spine.type.MessageClass;

import java.util.Set;

import static io.spine.base.Identifier.newUuid;

public class MulticastDispatcherIdentityTestEnv {

    /** Prevents instantiation of this utility class. */
    private MulticastDispatcherIdentityTestEnv() {
    }

    public static class IdentityDispatcher
            implements MulticastDispatcher<MessageClass, MessageEnvelope, String> {

        public static final String ID = newUuid();

        @Override
        public Set<MessageClass> messageClasses() {
            return ImmutableSet.of();
        }

        @Override
        public Set<String> dispatch(MessageEnvelope envelope) {
            return identity();
        }

        @Override
        public void onError(MessageEnvelope envelope, RuntimeException exception) {
            // Do nothing.
        }

        @Override
        public String toString() {
            return ID;
        }
    }
}
