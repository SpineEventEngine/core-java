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

package io.spine.testdata;

import io.spine.server.BoundedContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.enrich.Enricher;
import io.spine.server.event.EventBus;
import io.spine.server.stand.Stand;

/**
 * Creates stubs with instances of {@link BoundedContext} for testing purposes.
 *
 * @author Alexander Yevsyukov
 */
public class TestBoundedContextFactory {

    private TestBoundedContextFactory() {
        // Prevent instantiation of this utility class.
    }

    public static class MultiTenant {

        private MultiTenant() {
            // Prevent instantiation of this utility class.
        }

        private static BoundedContextBuilder newBuilder() {
            return BoundedContext.newBuilder()
                                 .setMultitenant(true);
        }

        public static BoundedContext newBoundedContext() {
            return newBuilder().build();
        }

        public static BoundedContext newBoundedContext(EventBus.Builder eventBus) {
            return newBuilder()
                    .setEventBus(eventBus)
                    .build();
        }

        public static BoundedContext newBoundedContext(CommandBus.Builder commandBus) {
            return newBuilder()
                    .setCommandBus(commandBus)
                    .build();
        }

        public static BoundedContext newBoundedContext(String name, Stand.Builder stand) {
            return newBuilder()
                    .setStand(stand)
                    .setName(name)
                    .build();
        }

        public static BoundedContext newBoundedContext(Stand.Builder stand) {
            return newBuilder()
                    .setStand(stand)
                    .build();
        }

        public static BoundedContext newBoundedContext(Enricher enricher) {
            return BoundedContext.newBuilder()
                                 .setMultitenant(true)
                                 .setEventBus(EventBus.newBuilder()
                                                      .setEnricher(enricher))
                                 .build();
        }

        public static BoundedContext newBoundedContext(CommandBus.Builder commandBus,
                                                       EventBus.Builder eventBus) {
            return newBuilder()
                    .setCommandBus(commandBus)
                    .setEventBus(eventBus)
                    .build();
        }
    }
}
