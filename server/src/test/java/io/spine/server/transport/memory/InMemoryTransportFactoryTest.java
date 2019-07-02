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

package io.spine.server.transport.memory;

import com.google.common.testing.NullPointerTester;
import io.spine.base.Identifier;
import io.spine.server.integration.ChannelId;
import io.spine.server.transport.TransportFactory;
import io.spine.testing.TestValues;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("`InMemoryTransportFactory` should")
class InMemoryTransportFactoryTest {

    private TransportFactory transportFactory;

    @BeforeEach
    void createInstance() {
        transportFactory = InMemoryTransportFactory.newInstance();
    }

    @Test
    @DisplayName("reject requests when closed")
    void closing() throws Exception {
        transportFactory.close();

        assertThrows(
                IllegalStateException.class, () ->
                transportFactory.createPublisher(newChannelId())
        );
    }

    @Test
    @DisplayName("reject null arguments")
    void nulls() {
        new NullPointerTester().testAllPublicInstanceMethods(transportFactory);
    }

    static ChannelId newChannelId() {
        return ChannelId.newBuilder()
                        .setIdentifier(Identifier.pack(TestValues.newUuidValue()))
                        .build();
    }
}
