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

package io.spine.server.bus;

import io.spine.annotation.SPI;
import io.spine.base.Error;
import io.spine.core.Ack;
import io.spine.server.event.RejectionEnvelope;
import io.spine.server.type.MessageEnvelope;

import java.util.Optional;

/**
 * The filter for the messages posted to a bus.
 *
 * <p>A bus may have several filters which can prevent a message from being posted.
 */
@SPI
@FunctionalInterface
public interface BusFilter<E extends MessageEnvelope<?, ?, ?>> extends AutoCloseable {

    /**
     * Accepts or rejects a passed message.
     *
     * <p>A filter can:
     * <ul>
     *     <li>accept the message (by returning {@code Optional.empty()};
     *     <li>reject the message with {@link io.spine.base.Error Error} status, for example, if
     *         it fails to pass the validation;
     *     <li>reject the message with the business rejection, for example, if the user who made
     *         the request does not have enough permissions in the system;
     *     <li>reject the message with {@code OK} status. For example, a scheduled command may not
     *         pass a filter.
     * </ul>
     *
     * @param envelope
     *         the envelope with the message to filter
     * @return {@code Optional.empty()} if the message passes the filter,
     *         {@linkplain Ack posting result} with either status otherwise
     */
    Optional<Ack> accept(E envelope);

    /**
     * Rejects the message with the {@code OK} status.
     *
     * <p>This method is a shortcut which can be used in {@link #accept(MessageEnvelope)} when the
     * message does not pass the filter and this is expected.
     *
     * @param envelope
     *          the envelope with the message to filter
     * @return the {@code Optional.of(Ack)} signaling that the message does not pass the filter
     */
    default Optional<Ack> reject(E envelope) {
        Ack ack = Acks.ok(envelope.id());
        return Optional.of(ack);
    }

    /**
     * Rejects the message with an {@link io.spine.base.Error Error} status.
     *
     * <p>This method is a shortcut which can be used in {@link #accept(MessageEnvelope)} when the
     * message does not pass the filter due to a technical error in the system.
     *
     * @param envelope
     *          the envelope with the message to filter
     * @return the {@code Optional.of(Ack)} signaling that the message does not pass the filter
     */
    default Optional<Ack> reject(E envelope, Error cause) {
        Ack ack = Acks.error(envelope.id(), cause);
        return Optional.of(ack);
    }

    /**
     * Rejects the message with a {@linkplain io.spine.base.RejectionMessage rejection} status.
     *
     * <p>This method is a shortcut which can be used in {@link #accept(MessageEnvelope)} when the
     * message does not pass the filter due to a business rejection.
     *
     * @param envelope
     *          the envelope with the message to filter
     * @return the {@code Optional.of(Ack)} signaling that the message does not pass the filter
     */
    default Optional<Ack> reject(E envelope, RejectionEnvelope cause) {
        Ack ack = Acks.rejection(envelope.id(), cause);
        return Optional.of(ack);
    }

    /**
     * {@inheritDoc}
     *
     * <p>By default, performs no action.
     */
    @Override
    default void close() throws Exception {
        // NoOp by default.
    }
}
