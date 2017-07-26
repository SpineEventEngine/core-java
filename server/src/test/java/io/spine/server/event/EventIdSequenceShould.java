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

import io.spine.core.Commands;
import org.junit.Test;

import static io.spine.server.event.EventIdSequence.LEADING_ZERO;
import static io.spine.server.event.EventIdSequence.MAX_ONE_DIGIT_SIZE;
import static io.spine.server.event.EventIdSequence.SEPARATOR;
import static io.spine.server.event.EventIdSequence.on;
import static io.spine.validate.Validate.isDefault;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Alexander Yevsyukov
 */
public class EventIdSequenceShould {

    @Test
    public void create_sequence_with_default_size() {
        final EventIdSequence sequence = on(Commands.generateId());
        for (int i = 1; i <= MAX_ONE_DIGIT_SIZE; i++) {
            assertFalse(isDefault(sequence.next()));
        }
    }

    @Test
    public void create_sequence_with_custom_size() {
        final EventIdSequence sequence = on(Commands.generateId())
                                         .withMaxSize(MAX_ONE_DIGIT_SIZE + 1);
        final String eventId = sequence.next().getValue();
        final int separatorIndex = eventId.lastIndexOf(SEPARATOR);
        assertEquals(LEADING_ZERO, eventId.charAt(separatorIndex + 1));
    }

    @Test
    public void generate_id_event_when_going_over_max_size() throws Exception {
        final EventIdSequence sequence = on(Commands.generateId());
        for (int i = 1; i <= MAX_ONE_DIGIT_SIZE + 5; i++) {
            sequence.next();
        }
    }
}
