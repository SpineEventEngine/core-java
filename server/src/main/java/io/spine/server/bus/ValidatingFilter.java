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

package io.spine.server.bus;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.base.Error;
import io.spine.base.Identifier;
import io.spine.core.Ack;
import io.spine.core.MessageEnvelope;
import io.spine.core.MessageInvalid;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.bus.Buses.reject;

/**
 * A filter validating the {@linkplain MessageEnvelope envelopes} with the given
 * {@link EnvelopeValidator}.
 */
final class ValidatingFilter<E extends MessageEnvelope<?, T, ?>, T extends Message>
        implements BusFilter<E> {

    private final EnvelopeValidator<E> validator;

    ValidatingFilter(EnvelopeValidator<E> validator) {
        super();
        this.validator = checkNotNull(validator);
    }

    @Override
    public Optional<Ack> accept(E envelope) {
        checkNotNull(envelope);
        Optional<MessageInvalid> violation = validator.validate(envelope);
        if (violation.isPresent()) {
            Error error = violation.get()
                                   .asError();
            Any packedId = Identifier.pack(envelope.getId());
            Ack result = reject(packedId, error);
            return Optional.of(result);
        } else {
            return Optional.empty();
        }
    }
}
