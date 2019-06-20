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

package io.spine.server.delivery;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;

/**
 * Classifies the {@code InboxMessage}s messages by their type.
 */
class MessageClassifier {

    private final ImmutableList<InboxMessage> delivery;
    private final ImmutableList<InboxMessage> idempotence;
    private final ImmutableList<InboxMessage> removal;

    private MessageClassifier(
            ImmutableList<InboxMessage> delivery,
            ImmutableList<InboxMessage> idempotence,
            ImmutableList<InboxMessage> removal) {
        this.delivery = delivery;
        this.idempotence = idempotence;
        this.removal = removal;
    }

    /**
     * Classifies the messages taking into the account the idempotence window start and
     * the {@linkplain InboxMessage#getStatus() status} of each message.
     *
     * @return the instance of the {@code Classifier}.
     */
    static MessageClassifier of(ImmutableList<InboxMessage> messages,
                                Timestamp idempotenceWndStart) {

        ImmutableList.Builder<InboxMessage> deliveryBuilder = ImmutableList.builder();
        ImmutableList.Builder<InboxMessage> idempotenceBuilder = ImmutableList.builder();
        ImmutableList.Builder<InboxMessage> removalBuilder = ImmutableList.builder();

        for (InboxMessage message : messages) {
            Timestamp msgTime = message.getWhenReceived();
            boolean insideIdempotentWnd =
                    Timestamps.compare(msgTime, idempotenceWndStart) > 0;
            InboxMessageStatus status = message.getStatus();

            if (insideIdempotentWnd) {
                if (InboxMessageStatus.TO_DELIVER != status) {
                    idempotenceBuilder.add(message);
                }
            } else {
                removalBuilder.add(message);
            }

            if (InboxMessageStatus.TO_DELIVER == status) {
                deliveryBuilder.add(message);
            }
        }

        return new MessageClassifier(deliveryBuilder.build(),
                                     idempotenceBuilder.build(),
                                     removalBuilder.build());
    }

    /**
     * Returns the subset of the messages, which should be delivered right away.
     */
    ImmutableList<InboxMessage> toDeliver() {
        return delivery;
    }

    /**
     * Returns the subset of the messages, which should be used as a source for idempotence.
     */
    ImmutableList<InboxMessage> idempotenceSource() {
        return idempotence;
    }

    /**
     * Returns those messages which should be removed.
     */
    ImmutableList<InboxMessage> removals() {
        return removal;
    }
}
