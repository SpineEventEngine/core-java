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

import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Message;
import io.spine.core.MessageEnvelope;
import io.spine.type.MessageClass;
import org.junit.Test;

import static io.spine.validate.Validate.isDefault;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * An abstract test suite for all the tests of {@link MessageEnvelope} implementations.
 *
 * @author Dmytro Dashenkov
 */
public abstract class MessageEnvelopeShould<O extends Message,
                                            E extends MessageEnvelope<O>,
                                            C extends MessageClass> {

    protected abstract O outerObject();

    protected abstract E toEnvelope(O obj);

    protected abstract C getMessageClass(O obj);

    @Test
    public void not_accept_nulls_on_construction() {
        final O obj = outerObject();
        @SuppressWarnings("unchecked") // Due to generics.
        final Class<O> objectClass = (Class<O>) obj.getClass();
        @SuppressWarnings("unchecked") // Due to generics.
        final Class<E> envelopeClass = (Class<E>) toEnvelope(obj).getClass();
        new NullPointerTester()
                .setDefault(objectClass, obj)
                .testAllPublicStaticMethods(envelopeClass);
    }

    @Test
    public void obtain_outer_object() {
        final O obj = outerObject();
        final E envelope = toEnvelope(obj);
        assertEquals(obj, envelope.getOuterObject());
    }

    @Test
    public void extract_message() {
        final E envelope = toEnvelope(outerObject());
        final Message commandMessage = envelope.getMessage();
        assertNotNull(commandMessage);
        assertFalse(isDefault(commandMessage));
    }

    @Test
    public void obtain_message_class() {
        final O obj  = outerObject();
        final E envelope = toEnvelope(obj);
        assertEquals(getMessageClass(obj), envelope.getMessageClass());
    }

    @Test
    public void support_equality() {
        final O oneMessage = outerObject();
        final O anotherMessage = outerObject();

        final E oneEnvelope = toEnvelope(oneMessage);
        final E anotherEnvelope = toEnvelope(anotherMessage);

        new EqualsTester().addEqualityGroup(oneEnvelope)
                          .addEqualityGroup(anotherEnvelope)
                          .testEquals();
    }
}
