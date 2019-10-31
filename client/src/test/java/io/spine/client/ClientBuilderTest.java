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

package io.spine.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.client.Client.connectTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("`ClientBuilder` should")
class ClientBuilderTest {

    @Nested
    @DisplayName("be created with host/port combination")
    class HostPort {

        @Test
        @DisplayName("creating a `ManagedChannel` by these values ")
        void creating() {
            String host = "localhost";
            int port = 100;
            Client.Builder builder = connectTo(host, port);
            assertThat(builder.host())
                    .isEqualTo(host);
            assertThat(builder.port())
                    .isEqualTo(port);
            assertThat(builder.channel())
                    .isNull();

            Client client = builder.build();
            assertThat(client.channel())
                    .isNotNull();
        }

        @Test
        @DisplayName("rejecting empty or blank host name")
        void arguments() {
            assertRejects("");
            assertRejects(" ");
            assertRejects("  ");
        }

        private void assertRejects(String illegalHostName) {
            assertThrows(IllegalArgumentException.class,
                         () -> connectTo(illegalHostName, 404));
        }
    }
}
