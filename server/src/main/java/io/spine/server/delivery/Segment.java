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

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Portion of the inbox messages which are headed to the same target.
 */
final class Segment {

    private final String typeUrl;
    private final List<InboxMessage> messages = new ArrayList<>();

    /**
     * Creates a new {@code Segment}.
     *
     * @param typeUrl
     *         type URL of the target common for all messages in this segment
     */
    Segment(String typeUrl) {
        this.typeUrl = typeUrl;
    }

    /**
     * Groups the messages into {@code Segments} keeping the original order across segments.
     *
     * @param source
     *         the messages to group
     * @return an ordered list of {@code Segment}s
     */
    static List<Segment> groupByTargetType(Collection<InboxMessage> source) {
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
                if (!segment.typeUrl().equals(typeUrl)) {
                    result.add(segment);
                    segment = new Segment(typeUrl);
                }
            }
            segment.add(message);
        }
        if (segment.hasMessages()) {
            result.add(segment);
        }
        return result;
    }

    /**
     * Adds another message into this segment.
     */
    private void add(InboxMessage message) {
        messages.add(message);
    }

    /**
     * Tells if this segment has any messages.
     */
    boolean hasMessages() {
        return !messages.isEmpty();
    }

    /**
     * Returns the type URL of the target, to which the messages in this segments need
     * to be delivered.
     */
    String typeUrl() {
        return typeUrl;
    }

    /**
     * Returns the messages of this segment.
     */
    ImmutableList<InboxMessage> messages() {
        return ImmutableList.copyOf(messages);
    }
}
