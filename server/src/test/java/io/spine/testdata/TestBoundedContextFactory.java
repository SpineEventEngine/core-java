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

package io.spine.testdata;

import io.spine.server.BoundedContext;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.event.EventBus;
import io.spine.server.event.enrich.EventEnricher;
import io.spine.server.stand.Stand;
import io.spine.server.storage.StorageFactory;
import io.spine.server.storage.StorageFactorySwitch;

/**
 * Creates stubs with instances of {@link BoundedContext} for testing purposes.
 *
 * @author Alexander Yevsyukov
 */
public class TestBoundedContextFactory {

    private TestBoundedContextFactory() {
        // Prevent instantiation of this utility class.
    }

    public static class SingleTenant {

        private SingleTenant() {
            // Prevent instantiation of this utility class.
        }

        public static BoundedContext newBoundedContext(Stand.Builder stand) {
            return BoundedContext.newBuilder()
                                 .setStand(stand)
                                 .build();
        }
    }

    public static class MultiTenant {

        private MultiTenant() {
            // Prevent instantiation of this utility class.
        }

        private static BoundedContext.Builder newBuilder() {
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

        public static BoundedContext newBoundedContext(EventEnricher enricher) {
            final StorageFactory factory = StorageFactorySwitch.get(true);
            final EventBus.Builder eventBus = EventBus.newBuilder()
                                                      .setEnricher(enricher)
                                                      .setStorageFactory(factory);
            return newBoundedContext(eventBus);
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
