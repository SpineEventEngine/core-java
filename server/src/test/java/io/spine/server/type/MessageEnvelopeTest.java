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

package io.spine.server.type;

import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Message;
import io.spine.type.MessageClass;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.validate.Validate.isDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * An abstract test suite for all the tests of {@link MessageEnvelope} implementations.
 */
public abstract class MessageEnvelopeTest<O extends Message,
                                          E extends MessageEnvelope<?, O, ?>,
                                          C extends MessageClass> {

    protected abstract O outerObject();

    protected abstract E toEnvelope(O obj);

    protected abstract C getMessageClass(O obj);

    @Test
    @DisplayName("not accept nulls on construction")
    void notAcceptNullsOnConstruction() {
        O obj = outerObject();
        @SuppressWarnings("unchecked") // Due to generics.
        Class<O> objectClass = (Class<O>) obj.getClass();
        @SuppressWarnings("unchecked") // Due to generics.
        Class<E> envelopeClass = (Class<E>) toEnvelope(obj).getClass();
        new NullPointerTester()
                .setDefault(objectClass, obj)
                .testAllPublicStaticMethods(envelopeClass);
    }

    @Test
    @DisplayName("obtain outer object")
    void getOuterObject() {
        O obj = outerObject();
        E envelope = toEnvelope(obj);
        assertEquals(obj, envelope.getOuterObject());
    }

    @Test
    @DisplayName("extract message")
    void extractMessage() {
        E envelope = toEnvelope(outerObject());
        Message commandMessage = envelope.getMessage();
        assertNotNull(commandMessage);
        assertFalse(isDefault(commandMessage));
    }

    @Test
    @DisplayName("obtain message class")
    void getMessageClass() {
        O obj = outerObject();
        E envelope = toEnvelope(obj);
        assertEquals(getMessageClass(obj), envelope.getMessageClass());
    }

    @Test
    @DisplayName("support equality")
    void supportEquality() {
        O oneMessage = outerObject();
        O anotherMessage = outerObject();

        E oneEnvelope = toEnvelope(oneMessage);
        E anotherEnvelope = toEnvelope(anotherMessage);

        new EqualsTester().addEqualityGroup(oneEnvelope)
                          .addEqualityGroup(anotherEnvelope)
                          .testEquals();
    }
}
