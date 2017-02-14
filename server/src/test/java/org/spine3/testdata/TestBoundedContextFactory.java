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

package org.spine3.testdata;

import org.spine3.server.BoundedContext;
import org.spine3.server.command.CommandBus;
import org.spine3.server.event.EventBus;
import org.spine3.server.event.enrich.EventEnricher;
import org.spine3.server.stand.Stand;
import org.spine3.server.storage.memory.InMemoryStorageFactory;

/**
 * Creates stubs with instances of {@link BoundedContext} for testing purposes.
 *
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("UtilityClass")
public class TestBoundedContextFactory {

    private static final InMemoryStorageFactory FACTORY = InMemoryStorageFactory.getInstance();

    private TestBoundedContextFactory() {
    }

    public static BoundedContext newBoundedContext() {
        return BoundedContext.newBuilder()
                             .setMultitenant(true)
                             .build();
    }

    public static BoundedContext newBoundedContext(EventEnricher enricher) {
        final EventBus eventBus = EventBus.newBuilder()
                                          .setEnricher(enricher)
                                          .setStorageFactory(FACTORY)
                                          .build();
        return newBoundedContext(eventBus);
    }

    public static BoundedContext newBoundedContext(EventBus eventBus) {
        return BoundedContext.newBuilder()
                             .setMultitenant(true)
                             .setEventBus(eventBus)
                             .build();
    }

    public static BoundedContext newBoundedContext(Stand stand) {
        return BoundedContext.newBuilder()
                             .setStand(stand)
                             .build();
    }

    public static BoundedContext newBoundedContext(CommandBus commandBus) {
        return BoundedContext.newBuilder()
                             .setMultitenant(true)
                             .setCommandBus(commandBus)
                             .build();
    }

    public static BoundedContext newBoundedContext(CommandBus commandBus, EventBus eventBus) {
        return BoundedContext.newBuilder()
                             .setMultitenant(true)
                             .setCommandBus(commandBus)
                             .setEventBus(eventBus)
                             .build();
    }

    public static BoundedContext newBoundedContext(String name, Stand stand) {
        return BoundedContext.newBuilder()
                             .setMultitenant(true)
                             .setStand(stand)
                             .setName(name)
                             .build();
    }

}
