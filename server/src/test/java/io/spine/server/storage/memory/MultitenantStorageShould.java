/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.server.storage.memory;

import io.spine.test.storage.ProjectId;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.google.common.collect.Lists.newArrayListWithExpectedSize;
import static com.google.common.collect.Sets.newHashSetWithExpectedSize;
import static org.junit.Assert.assertEquals;

/**
 * @author Dmytry Ganzha
 */
public class MultitenantStorageShould {

    private static final boolean IS_MULTITENANT = false;

    private MultitenantStorage<TenantRecords<ProjectId>> multitenantStorage;

    @Before
    public void setUp() {
        multitenantStorage = new MultitenantStorage<TenantRecords<ProjectId>>(IS_MULTITENANT) {
            @Override
            TenantRecords<ProjectId> createSlice() {
                return new TenantRecords<>();
            }
        };
    }

    @Test
    public void should_return_same_slice_when_single_tenant_and_multithreaded_environment()
            throws InterruptedException, ExecutionException {
        final int numberOfTasks = 1000;
        final Collection<Callable<TenantRecords>> tasks = newArrayListWithExpectedSize(numberOfTasks);

        for (int i = 0; i < numberOfTasks; i++) {
            tasks.add(new Callable<TenantRecords>() {
                @Override
                public TenantRecords call() throws Exception {
                    final TenantRecords<ProjectId> storage = multitenantStorage.getStorage();
                    return storage;
                }
            });
        }

        final List<Future<TenantRecords>> futures = executeInMultithreadedEnvironment(tasks);
        final Set<TenantRecords> tenantRecords = convertFuturesToSetOfCompletedResults(futures);

        final int expected = 1;
        assertEquals(expected, tenantRecords.size());
    }

    private <R> Set<R> convertFuturesToSetOfCompletedResults(List<Future<R>> futures)
            throws ExecutionException, InterruptedException {
        final Set<R> tenantRecords = newHashSetWithExpectedSize(futures.size());
        for (Future<R> future : futures) {
            tenantRecords.add(future.get());
        }
        return tenantRecords;
    }

    private <R> List<Future<R>> executeInMultithreadedEnvironment(Collection<Callable<R>> tasks)
            throws InterruptedException {
        final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime()
                                                                             .availableProcessors() *
                                                                              2);
        final List<Future<R>> futures = executor.invokeAll(tasks);
        return futures;
    }
}
