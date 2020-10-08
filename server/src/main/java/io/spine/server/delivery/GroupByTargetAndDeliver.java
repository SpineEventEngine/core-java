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

package io.spine.server.delivery;

import io.spine.server.model.ModelError;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A method object performing the delivery of the messages grouping them by the type of their
 * targets.
 */
final class GroupByTargetAndDeliver implements DeliveryAction {

    private final InboxDeliveries inboxDeliveries;

    GroupByTargetAndDeliver(InboxDeliveries deliveries) {
        inboxDeliveries = deliveries;
    }

    /**
     * Performs the delivery of the messages grouping them by the type of their targets.
     *
     * <p>If an exception is thrown during delivery, this method propagates it.
     * If many exceptions are thrown, all of them are added to the first one
     * as {@code suppressed}, and the first one is propagated.
     *
     * <p>In case of an exception, the messages are marked as delivered, in order to avoid
     * repetitive delivery. However, if a JVM {@link Error} is thrown, only the messages
     * which were delivered successfully are marked as delivered. Moreover, a JVM {@link Error}
     * halts the delivery for all the subsequent messages in the batch.
     * However, this is not true for {@link ModelError}s, which are treated
     * in the same way as exceptions.
     *
     * @return errors occurred during the delivery
     */
    @Override
    public DeliveryErrors executeFor(List<InboxMessage> messages) {
        List<Segment> segments = groupByTargetType(messages);
        DeliveryErrors.Builder errors = DeliveryErrors.newBuilder();
        for (Segment segment : segments) {
            ShardedMessageDelivery<InboxMessage> delivery = inboxDeliveries.get(segment.typeUrl);
            List<InboxMessage> deliveryPackage = segment.messages;
            try {
                delivery.deliver(deliveryPackage);
            } catch (RuntimeException exception) {
                errors.addException(exception);
            } catch (@SuppressWarnings("ErrorNotRethrown") /* False positive */ ModelError error) {
                errors.addError(error);
            }
        }
        return errors.build();
    }

    /**
     * Groups the messages into {@code Segments} keeping the original order across segments.
     *
     * @param source
     *         the messages to group
     * @return an ordered list of {@code Segment}s
     */
    private static List<Segment> groupByTargetType(Collection<InboxMessage> source) {
        List<Segment> result = new ArrayList<>();

        if (source.isEmpty()) {
            return result;
        }
        Segment segment = null;
        for (InboxMessage message : source) {
            String typeUrl = message.getInboxId()
                                    .getTypeUrl();
            if (segment == null) {
                segment = new Segment(typeUrl);
            } else {
                if (!segment.typeUrl.equals(typeUrl)) {
                    result.add(segment);
                    segment = new Segment(typeUrl);
                }
            }
            segment.add(message);
        }
        if (!segment.messages.isEmpty()) {
            result.add(segment);
        }
        return result;
    }

    /**
     * Portion of the inbox messages which are headed to the same target.
     */
    private static final class Segment {

        private final String typeUrl;
        private final List<InboxMessage> messages = new ArrayList<>();

        /**
         * Creates a new {@code Segment}.
         *
         * @param typeUrl
         *         type URL of the target common for all messages in this segment
         */
        private Segment(String typeUrl) {
            this.typeUrl = typeUrl;
        }

        private void add(InboxMessage message) {
            messages.add(message);
        }
    }
}
