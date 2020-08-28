/*
 * Copyright 2020, TeamDev. All rights reserved.
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

import io.spine.base.Error;
import io.spine.base.ThrowableMessage;
import io.spine.core.Ack;
import io.spine.server.bus.BusFilter;
import io.spine.server.type.CommandEnvelope;
import io.spine.server.type.EventEnvelope;
import io.spine.test.bus.ShareId;
import io.spine.test.bus.command.ShareCannotBeTraded;

import java.util.Optional;

import static io.spine.base.Identifier.newUuid;

public final class BusFilters {

    /**
     * Prevents instantiation of this test env class.
     */
    private BusFilters() {
    }

    public static final class Accepting implements BusFilter<CommandEnvelope> {

        @Override
        public Optional<Ack> doFilter(CommandEnvelope envelope) {
            return letPass();
        }
    }

    public static final class RejectingWithOk implements BusFilter<CommandEnvelope> {

        @Override
        public Optional<Ack> doFilter(CommandEnvelope envelope) {
            return reject(envelope);
        }
    }

    public static final class RejectingWithError implements BusFilter<CommandEnvelope> {

        private final Error error;

        public RejectingWithError(Error error) {
            this.error = error;
        }

        @Override
        public Optional<Ack> doFilter(CommandEnvelope envelope) {
            return reject(envelope, error);
        }
    }

    public static final class RejectingWithThrowableMessage implements BusFilter<CommandEnvelope> {

        private final ThrowableMessage rejection;

        public RejectingWithThrowableMessage(ThrowableMessage rejection) {
            this.rejection = rejection;
        }

        @Override
        public Optional<Ack> doFilter(CommandEnvelope envelope) {
            return reject(envelope, rejection);
        }
    }

    public static final class Throwing implements BusFilter<EventEnvelope> {

        @Override
        public Optional<Ack> doFilter(EventEnvelope envelope) {
            ShareCannotBeTraded rejection = ShareCannotBeTraded
                    .newBuilder()
                    .setShare(ShareId.newBuilder().setValue(newUuid()).build())
                    .setReason("Test filter rejection.")
                    .build();
            return reject(envelope, rejection);
        }
    }
}
