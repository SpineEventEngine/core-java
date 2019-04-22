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

package io.spine.server.delivery.given;

import com.google.protobuf.Empty;
import io.spine.core.Subscribe;
import io.spine.server.delivery.SystemEventWatcher;
import io.spine.system.server.event.EntityCreated;
import io.spine.test.delivery.SewProjectCreated;
import io.spine.type.TypeUrl;

import static org.junit.jupiter.api.Assertions.fail;

public class SystemEventWatcherTestEnv {

    /**
     * Prevents the utility class instantiation.
     */
    private SystemEventWatcherTestEnv() {
    }

    public static class ExternalWatcher extends SystemEventWatcher {

        public ExternalWatcher() {
            super(TypeUrl.of(Empty.class));
        }

        @Subscribe(external = true)
        void on(@SuppressWarnings("unused") SewProjectCreated ignored) {
            fail("External events are not allowed in SystemEventWatchers.");
        }
    }

    public static class NonSystemWatcher extends SystemEventWatcher {

        public NonSystemWatcher() {
            super(TypeUrl.of(Empty.class));
        }

        @Subscribe
        void on(@SuppressWarnings("unused") SewProjectCreated ignored) {
            fail("Only spine.system.server events are allowed.");
        }
    }

    public static class ValidSystemWatcher extends SystemEventWatcher {

        public ValidSystemWatcher() {
            super(TypeUrl.of(Empty.class));
        }

        @Subscribe
        void on(@SuppressWarnings("unused") EntityCreated unused) {
            // NoOp.
        }
    }
}
