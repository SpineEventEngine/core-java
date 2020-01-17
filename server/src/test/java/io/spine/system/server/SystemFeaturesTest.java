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

package io.spine.system.server;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SystemFeatures should")
class SystemFeaturesTest {

    @Nested
    @DisplayName("by default")
    class Defaults {

        @Test
        @DisplayName("enable aggregate mirroring")
        void mirrors() {
            SystemFeatures features = SystemFeatures.defaults();
            assertTrue(features.includeAggregateMirroring());
        }

        @Test
        @DisplayName("disable command log")
        void commands() {
            SystemFeatures features = SystemFeatures.defaults();
            assertFalse(features.includeCommandLog());
        }

        @Test
        @DisplayName("disable event store")
        void events() {
            SystemFeatures features = SystemFeatures.defaults();
            assertFalse(features.includePersistentEvents());
        }
    }

    @Nested
    @DisplayName("configure")
    class Configure {
        @Test
        @DisplayName("aggregate mirroring")
        void mirrors() {
            SystemFeatures features = SystemFeatures
                    .defaults()
                    .disableAggregateQuerying();
            assertFalse(features.includeAggregateMirroring());
        }

        @Test
        @DisplayName("command log")
        void commands() {
            SystemFeatures features = SystemFeatures
                    .defaults()
                    .enableCommandLog();
            assertTrue(features.includeCommandLog());
        }

        @Test
        @DisplayName("event store")
        void events() {
            SystemFeatures features = SystemFeatures
                    .defaults()
                    .persistEvents();
            assertTrue(features.includePersistentEvents());
        }
    }
}
