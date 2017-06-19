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

package io.spine.envelope;

import com.google.protobuf.Message;
import io.spine.type.MessageClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * An abstract test suite for all the tests of {@link MessageWithIdEnvelope} derived types.
 *
 * @author Dmytro Dashenkov
 */
public abstract class MessageWithIdEnvelopeShould<O extends Message,
                                                  I extends Message,
                                                  E extends MessageWithIdEnvelope<I, O>,
                                                  C extends MessageClass>
        extends MessageEnvelopeShould<O, E, C> {

    protected abstract I getId(O obj);

    @Test
    public void obtain_message_id() {
        final O obj = outerObject();
        final E envelope = toEnvelope(obj);
        assertEquals(getId(obj), envelope.getId());
    }


}
