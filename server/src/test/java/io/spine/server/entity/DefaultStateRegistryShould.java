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

package io.spine.server.entity;

import com.google.protobuf.Timestamp;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.google.common.collect.Maps.newConcurrentMap;
import static io.spine.Identifier.newUuid;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Alexander Yevsyukov
 * @author Dmitry Ganzha
 */
public class DefaultStateRegistryShould {

    private static final String DEFAULT_STATES_FIELD_NAME = "defaultStates";
    private DefaultStateRegistry registry;
    private Map<Object, Object> spyMap;

    @Before
    public void setUp() {
        spyMap = spy(newConcurrentMap());
        registry = DefaultStateRegistry.getInstance();
        try {
            final Field defaultStates = registry.getClass()
                                          .getDeclaredField(DEFAULT_STATES_FIELD_NAME);
            defaultStates.setAccessible(true);
            defaultStates.set(registry, spyMap);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }
    }

    @Test
    public void verify_put_invoked_one_time_when_invoke_get_default_state_in_multithreaded_environment() {
        final int numberOfEntities = 1000;
        final Collection<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < numberOfEntities; i++) {
            tasks.add(Executors.callable(new Runnable() {
                @Override
                public void run() {
                    final TestEntity testEntity = TestEntity.newInstance(newUuid());
                    testEntity.getDefaultState();
                }
            }));
        }

        executeInMultithreadedEnvironment(tasks);

        final int expected = 1;
        verify(spyMap, times(expected)).put(any(), any());
        assertEquals(expected, spyMap.size());
    }

    @Test
    public void verify_put_invoked_one_time_when_invoke_put_or_get_in_multithreaded_environment() {
        final int numberOfEntities = 1000;
        final Collection<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < numberOfEntities; i++) {
            tasks.add(Executors.callable(new Runnable() {
                @Override
                public void run() {
                    registry.putOrGet(TimerSnapshot.class, Timestamp.getDefaultInstance());
                }
            }));
        }

        executeInMultithreadedEnvironment(tasks);

        final int expected = 1;
        verify(spyMap, times(expected)).put(any(), any());
        assertEquals(expected, spyMap.size());
    }

    private void executeInMultithreadedEnvironment(Collection<Callable<Object>> tasks) {
        final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime()
                                                                             .availableProcessors() *
                                                                              2);
        try {
            executor.invokeAll(tasks);
        } catch (InterruptedException ignored) {
        }
    }

    private static class TimerSnapshot extends AbstractEntity<Long, Timestamp> {
        protected TimerSnapshot(Long id) {
            super(id);
        }
    }
}
