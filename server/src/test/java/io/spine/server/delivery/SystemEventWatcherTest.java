/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.server.delivery;

import io.spine.server.BoundedContext;
import io.spine.server.delivery.given.SystemEventWatcherTestEnv.ExternalWatcher;
import io.spine.server.delivery.given.SystemEventWatcherTestEnv.NonSystemWatcher;
import io.spine.server.delivery.given.SystemEventWatcherTestEnv.ValidSystemWatcher;
import io.spine.server.event.EventBus;
import io.spine.server.integration.IntegrationBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("SystemEventWatcher should")
class SystemEventWatcherTest {

    private BoundedContext boundedContext;

    @BeforeEach
    void setUp() {
        boundedContext = BoundedContext.newBuilder().build();
    }

    @Test
    @DisplayName("not subscribe to external events")
    void notDomestic() {
        SystemEventWatcher watcher = new ExternalWatcher();
        IntegrationBus bus = boundedContext.getIntegrationBus();
        assertThrows(IllegalStateException.class, () -> bus.register(watcher));
    }

    @Test
    @DisplayName("not subscribe to external non-system events")
    void onlySystem() {
        SystemEventWatcher watcher = new NonSystemWatcher();
        EventBus bus = boundedContext.getEventBus();
        assertThrows(IllegalStateException.class, () -> bus.register(watcher));
    }

    @Test
    @DisplayName("subscribe to domestic system events")
    void subscribe() {
        SystemEventWatcher watcher = new ValidSystemWatcher();
        EventBus bus = boundedContext.getEventBus();
        bus.register(watcher);
    }
}
