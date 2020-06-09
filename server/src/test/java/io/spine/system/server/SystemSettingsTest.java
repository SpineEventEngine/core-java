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

import io.spine.base.Environment;
import io.spine.base.Production;
import io.spine.base.Tests;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("SystemFeatures should")
class SystemSettingsTest {

    @Nested
    @DisplayName("by default")
    class Defaults {

        @AfterEach
        void resetEnv() {
            Environment.instance()
                       .reset();
        }

        @Test
        @DisplayName("enable aggregate mirroring")
        void mirrors() {
            SystemSettings features = SystemSettings.defaults();
            assertTrue(features.includeAggregateMirroring());
        }

        @Test
        @DisplayName("disable command log")
        void commands() {
            SystemSettings features = SystemSettings.defaults();
            assertFalse(features.includeCommandLog());
        }

        @Test
        @DisplayName("disable event store")
        void events() {
            SystemSettings features = SystemSettings.defaults();
            assertFalse(features.includePersistentEvents());
        }

        @Test
        @DisplayName("allow parallel posting for system events")
        void parallelism() {
            Environment env = Environment.instance();

            assumeTrue(env.is(Tests.class));
            assertFalse(SystemSettings.defaults()
                                      .postEventsInParallel());

            env.setTo(new Production());
            assertTrue(SystemSettings.defaults()
                                     .postEventsInParallel());
        }
    }

    @Nested
    @DisplayName("configure")
    class Configure {

        @Test
        @DisplayName("aggregate mirroring")
        void mirrors() {
            SystemSettings features = SystemSettings
                    .defaults()
                    .disableAggregateQuerying();
            assertFalse(features.includeAggregateMirroring());
        }

        @Test
        @DisplayName("command log")
        void commands() {
            SystemSettings features = SystemSettings
                    .defaults()
                    .enableCommandLog();
            assertTrue(features.includeCommandLog());
        }

        @Test
        @DisplayName("event store")
        void events() {
            SystemSettings features = SystemSettings
                    .defaults()
                    .persistEvents();
            assertTrue(features.includePersistentEvents());
        }

        @Test
        @DisplayName("system events to be posted in synch")
        void parallelism() {
            SystemSettings features = SystemSettings
                    .defaults()
                    .disableParallelPosting();
            assertFalse(features.postEventsInParallel());
        }
    }
}
