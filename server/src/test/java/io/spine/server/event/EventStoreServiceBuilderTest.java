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

package io.spine.server.event;

import com.google.common.util.concurrent.MoreExecutors;
import io.spine.server.BoundedContext;
import io.spine.server.storage.StorageFactory;
import io.spine.test.Tests;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("EventStore ServiceBuilder")
class EventStoreServiceBuilderTest {

    private StorageFactory storageFactory;
    private EventStore.ServiceBuilder builder;

    @BeforeEach
    void setUp() {
        BoundedContext bc = BoundedContext
                .newBuilder()
                .setMultitenant(true)
                .build();
        storageFactory = bc.getStorageFactory();
        builder = EventStore.newServiceBuilder();
    }

    @Test
    @DisplayName("throw NPE on null executor")
    void throwOnNullExecutor() {
        assertThrows(NullPointerException.class, () -> builder.setStreamExecutor(Tests.nullRef()));
    }

    @Test
    @DisplayName("throw NPE on null EventStorage")
    void throwOnNullEventStorage() {
        assertThrows(NullPointerException.class, () -> builder.setStreamExecutor(Tests.nullRef()));
    }

    @Test
    @DisplayName("throw NPE on non set streamExecutor")
    void throwOnNonSetStreamExecutor() {
        assertThrows(NullPointerException.class, () -> builder.setStorageFactory(storageFactory)
                                                              .build());
    }

    @Test
    @DisplayName("throw NPE on non set eventStorage")
    void throwOnNonSetEventStorage() {
        assertThrows(NullPointerException.class, () -> builder.setStreamExecutor(newExecutor())
                                                              .build());
    }

    @Test
    @DisplayName("return set streamExecutor")
    void returnSetStreamExecutor() {
        Executor executor = newExecutor();
        assertEquals(executor, builder.setStreamExecutor(executor)
                                      .getStreamExecutor());
    }

    @Test
    @DisplayName("return set eventStorage")
    void returnSetEventStorage() {
        assertEquals(storageFactory, builder.setStorageFactory(storageFactory)
                                            .getStorageFactory());
    }

    @Test
    @DisplayName("build service definition")
    void buildServiceDefinition() {
        assertNotNull(builder.setStreamExecutor(newExecutor())
                             .setStorageFactory(storageFactory)
                             .build());
    }

    private static Executor newExecutor() {
        return MoreExecutors.directExecutor();
    }
}
