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

package io.spine.core;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.spine.annotation.GeneratedMixin;
import io.spine.base.MessageContext;
import io.spine.base.SerializableMessage;
import io.spine.protobuf.AnyPacker;
import io.spine.time.TimestampTemporal;
import io.spine.type.TypeUrl;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protobuf.AnyPacker.pack;

/**
 * A message which can be dispatched and cause other messages.
 *
 * <p>A signal message travels through the system just like an electronic signal travels through
 * a neural network. It cases the system to change its state either directly or by exciting other
 * signals.
 *
 * <p>A signal message may originate from outside the system, from a user, or from a policy
 * implemented in the system.
 *
 * @param <I>
 *         the type of the signal identifier
 * @param <M>
 *         the type of the enclosed messages
 * @param <C>
 *         the type of the message context
 */
@GeneratedMixin
public interface Signal<I extends SignalId,
                        M extends SerializableMessage,
                        C extends MessageContext>
        extends Message {

    /**
     * Obtains the identifier of the message.
     */
    I getId();

    /**
     * Obtains the packed version of the enclosed message.
     *
     * @see #enclosedMessage()
     */
    Any getMessage();

    /**
     * Obtains the context of the enclosed message.
     */
    @SuppressWarnings("override") // Overridden in generated code.
    C getContext();

    /**
     * Obtains the identifier of the message.
     */
    default I id() {
        return getId();
    }

    /**
     * Obtains the unpacked form of the enclosed message.
     *
     * @see #getMessage()
     */
    @SuppressWarnings("unchecked") // protected by generic params of extending interfaces
    default M enclosedMessage() {
        Message enclosed = AnyPacker.unpack(getMessage());
        return (M) enclosed;
    }

    /**
     * Obtains the context of the enclosed message.
     */
    default C context() {
        return getContext();
    }

    /**
     * Obtains the ID of the tenant under which the message was created.
     */
    TenantId tenant();

    /**
     * Obtains the time when the message was created.
     */
    Timestamp time();

    /**
     * Obtains the data about the actor who started the message chain.
     */
    ActorContext actorContext();

    /**
     * Obtains the type URL of the enclosed message.
     */
    default TypeUrl typeUrl() {
        return TypeUrl.ofEnclosed(getMessage());
    }

    /**
     * Verifies if the enclosed message has the same type as the passed, or the passed type
     * is the super-type of the message.
     */
    default boolean is(Class<? extends Message> enclosedMessageClass) {
        checkNotNull(enclosedMessageClass);
        Message enclosed = enclosedMessage();
        boolean result = enclosedMessageClass.isAssignableFrom(enclosed.getClass());
        return result;
    }

    /**
     * Verifies if the message was created after the point in time.
     */
    default boolean isAfter(Timestamp bound) {
        checkNotNull(bound);
        TimestampTemporal timeTemporal = TimestampTemporal.from(time());
        TimestampTemporal boundTemporal = TimestampTemporal.from(bound);
        return timeTemporal.isLaterThan(boundTemporal);
    }

    /**
     * Verifies if the message was created before the point in time.
     */
    default boolean isBefore(Timestamp bound) {
        checkNotNull(bound);
        TimestampTemporal timeTemporal = TimestampTemporal.from(time());
        TimestampTemporal boundTemporal = TimestampTemporal.from(bound);
        return timeTemporal.isEarlierThan(boundTemporal);
    }

    /**
     * Verifies if the message was created within the passed period of time.
     *
     * @param periodStart
     *         lower bound, exclusive
     * @param periodEnd
     *         higher bound, inclusive
     * @return {@code true} if the time point of the command creation lies in between the given two
     */
    default boolean isBetween(Timestamp periodStart, Timestamp periodEnd) {
        checkNotNull(periodStart);
        checkNotNull(periodEnd);
        TimestampTemporal timeTemporal = TimestampTemporal.from(time());
        TimestampTemporal start = TimestampTemporal.from(periodStart);
        TimestampTemporal end = TimestampTemporal.from(periodEnd);
        return timeTemporal.isBetween(start, end);
    }

    /**
     * Obtains the ID of this message.
     */
    default MessageId messageId() {
        return MessageId
                .newBuilder()
                .setId(pack(id()))
                .setTypeUrl(typeUrl().value())
                .vBuild();
    }

    /**
     * Obtains the ID of the first message in the chain.
     *
     * <p>The root message is always produced by an actor directly. Tenant and actor of the root
     * message define the tenant and actor of the whole chain.
     */
    MessageId rootMessage();

    Optional<MessageId> parent();
}
