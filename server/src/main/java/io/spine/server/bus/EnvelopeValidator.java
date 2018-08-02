/*
 * Copyright 2018, TeamDev. All rights reserved.
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

import io.spine.core.MessageEnvelope;
import io.spine.core.MessageInvalid;

import java.util.Optional;

/**
 * An interface defining the validator for a {@link MessageEnvelope}.
 *
 * @param <E> the type of the {@link MessageEnvelope} to validate
 * @author Dmytro Dashenkov
 */
public interface EnvelopeValidator<E extends MessageEnvelope<?, ?, ?>> {

    /**
     * Validates the given {@link MessageEnvelope} by some specific rules.
     *
     * @param envelope the envelope to validate
     * @return {@link Optional#empty()} if the envelope passes the validation or the cause of
     *         the validation error
     * @see MessageInvalid for the detailed description of the returned value
     */
    Optional<MessageInvalid> validate(E envelope);
}
