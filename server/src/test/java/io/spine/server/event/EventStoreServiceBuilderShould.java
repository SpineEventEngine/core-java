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

package io.spine.server.event;

import com.google.common.util.concurrent.MoreExecutors;
import io.spine.server.BoundedContext;
import io.spine.server.storage.StorageFactory;
import io.spine.test.Tests;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Executor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EventStoreServiceBuilderShould {

    private StorageFactory storageFactory;
    private EventStore.ServiceBuilder builder;

    @Before
    public void setUp() {
        final BoundedContext bc = BoundedContext.newBuilder()
                                                .setMultitenant(true)
                                                .build();
        storageFactory = bc.getStorageFactory();
        builder = EventStore.newServiceBuilder();
    }

    @Test(expected = NullPointerException.class)
    public void throw_NPE_on_null_executor() {
        builder.setStreamExecutor(Tests.<Executor>nullRef());
    }

    @Test(expected = NullPointerException.class)
    public void throw_NPE_on_null_EventStorage() {
        builder.setStreamExecutor(Tests.<Executor>nullRef());
    }

    @Test(expected = NullPointerException.class)
    public void throw_NPE_on_non_set_streamExecutor() {
        builder.setStorageFactory(storageFactory)
               .build();
    }

    @Test(expected = NullPointerException.class)
    public void throw_NPE_on_non_set_eventStorage() {
        builder.setStreamExecutor(newExecutor())
               .build();
    }

    @Test
    public void return_set_streamExecutor() {
        final Executor executor = newExecutor();
        assertEquals(executor, builder.setStreamExecutor(executor).getStreamExecutor());
    }

    @Test
    public void return_set_eventStorage() {
        assertEquals(storageFactory, builder.setStorageFactory(storageFactory)
                                            .getStorageFactory());
    }

    @Test
    public void build_service_definition() {
        assertNotNull(builder.setStreamExecutor(newExecutor())
                             .setStorageFactory(storageFactory)
                             .build());
    }

    private static Executor newExecutor() {
        return MoreExecutors.directExecutor();
    }
}
