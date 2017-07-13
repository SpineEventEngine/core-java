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

package io.spine.server.event;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.protobuf.Message;
import io.spine.Identifier;
import io.spine.core.EventId;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates a sequence of identifiers of events produced in response to a message which caused
 * these events.
 *
 * @author Alexander Yevsyukov
 * @see EventId
 */
final class EventIdSequence {

    /**
     * The separator between command ID part and sequence number part.
     */
    @VisibleForTesting
    static final char SEPARATOR = '-';

    /**
     * The radix for sequence number output.
     */
    private static final int RADIX = Character.MAX_RADIX;

    /**
     * The maximum number of events that fit into one digit sequence number.
     */
    static final int MAX_ONE_DIGIT_SIZE = RADIX;

    /**
     * The character appearing before the sequence number if the sequence size is bigger
     * than {@link #RADIX}.
     */
    @VisibleForTesting
    static final char LEADING_ZERO = '0';

    /**
     * The prefix with an origin message ID.
     */
    private final String originPrefix;

    /**
     * The length of the sequence number suffix.
     */
    private final int suffixLength;

    /**
     * The number of the next ID in the sequence.
     */
    private final AtomicInteger count = new AtomicInteger(0);

    /**
     * Creates a new one digit event ID sequence for the origin message ID.
     */
    static EventIdSequence on(Message originId) {
        return new EventIdSequence(originId);
    }

    private EventIdSequence(Message originId, int maxSize) {
        this(createPrefix(originId), maxSize);
    }

    private EventIdSequence(String prefix, int maxSize) {
        this.originPrefix = prefix;
        this.suffixLength = maxSize % RADIX + 1;
    }

    private EventIdSequence(Message originId) {
        this(originId, MAX_ONE_DIGIT_SIZE);
    }

    private static String createPrefix(Message originId) {
        final String result = Identifier.toString(originId) + SEPARATOR;
        return result;
    }

    /**
     * Creates a new sequence with the same command ID and the passed maximum size.
     */
    EventIdSequence withMaxSize(int maxSize) {
        return new EventIdSequence(this.originPrefix, maxSize);
    }

    /**
     * Generates the next event ID in the sequence.
     */
    EventId next() {
        count.incrementAndGet();
        final String value = buildValue();
        final EventId result = EventId.newBuilder()
                                      .setValue(value)
                                      .build();
        return result;
    }

    private String buildValue() {
        final String sequenceNumber = Integer.toString(count.get(), RADIX);
        final String suffix = (suffixLength == 1)
                    ? sequenceNumber
                    : Strings.padStart(sequenceNumber, suffixLength, LEADING_ZERO);
        return originPrefix + suffix;
    }
}
