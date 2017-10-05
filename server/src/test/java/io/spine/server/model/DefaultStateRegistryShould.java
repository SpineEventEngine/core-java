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

package io.spine.server.model;

import com.google.protobuf.Timestamp;
import io.spine.server.entity.AbstractEntity;
import io.spine.server.entity.TestEntity;
import io.spine.server.entity.given.Given;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.google.common.collect.Lists.newArrayListWithExpectedSize;
import static com.google.common.collect.Maps.newConcurrentMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
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
        injectField(registry, DEFAULT_STATES_FIELD_NAME, spyMap);
    }

    @Test
    public void verify_put_invoked_one_time_when_invoke_get_default_state_in_multithreaded_environment() {
        final int numberOfEntities = 1000;
        final Collection<Callable<Object>> tasks = newArrayListWithExpectedSize(numberOfEntities);
        for (int i = 0; i < numberOfEntities; i++) {
            tasks.add(Executors.callable(new Runnable() {
                @Override
                public void run() {
                    final TestEntity testEntity = Given.entityOfClass(TestEntity.class)
                                                       .build();
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
        final Collection<Callable<Object>> tasks = newArrayListWithExpectedSize(numberOfEntities);
        for (int i = 0; i < numberOfEntities; i++) {
            tasks.add(Executors.callable(new Runnable() {
                @Override
                public void run() {
                    registry.putOrGet(TimerSnapshot.class);
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

    private void injectField(Object target, String fieldName, Object injectableValue) {
        try {
            final Field defaultStates = target.getClass()
                                                .getDeclaredField(fieldName);
            defaultStates.setAccessible(true);
            defaultStates.set(target, injectableValue);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            fail("Field " + fieldName + " should exist");
        }
    }

    private static class TimerSnapshot extends AbstractEntity<Long, Timestamp> {
        protected TimerSnapshot(Long id) {
            super(id);
        }
    }
}
