/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

import com.google.common.collect.ImmutableSet;
import io.spine.core.MessageEnvelope;
import io.spine.test.Tests;
import io.spine.type.MessageClass;
import org.junit.Test;

import java.util.Set;

import static io.spine.Identifier.newUuid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Yevsyukov
 */
public class MulticastDispatcherIdentityShould {

    @Test
    public void have_utility_ctor() {
        Tests.assertHasPrivateParameterlessCtor(MulticastDispatcher.Identity.class);
    }

    @Test
    public void return_dispatcher_identity() throws Exception {
        final Set<String> set = MulticastDispatcher.Identity.of(new IdentityTest());

        assertTrue(set.contains(IdentityTest.ID));
        assertEquals(1, set.size());
    }

    private static class IdentityTest
            implements MulticastDispatcher<MessageClass, MessageEnvelope, String> {

        private static final String ID = newUuid();

        @Override
        public Set<MessageClass> getMessageClasses() {
            return ImmutableSet.of();
        }

        @Override
        public Set<String> dispatch(MessageEnvelope envelope) {
            return Identity.of(this);
        }

        @Override
        public void onError(MessageEnvelope envelope, RuntimeException exception) {
            // Do nothing.
        }

        @Override
        public String toString() {
            return ID;
        }
    }
}
