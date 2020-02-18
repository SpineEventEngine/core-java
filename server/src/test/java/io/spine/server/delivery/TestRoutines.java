/*
 * Copyright 2020, TeamDev. All rights reserved.
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

import com.google.common.truth.Truth8;
import io.spine.server.projection.Projection;
import io.spine.server.projection.ProjectionRepository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.google.common.truth.Truth.assertThat;

/**
 * Test-only routines to use when testing the catch-up and delivery API.
 */
public final class TestRoutines {

    private TestRoutines() {
    }

    public static <P extends Projection<String, ?, ?>> P
    findView(ProjectionRepository<String, P, ?> repo, String id) {
        Optional<P> view = repo.find(id);
        Truth8.assertThat(view)
              .isPresent();
        return view.get();
    }

    public static void post(List<Callable<Object>> jobs, int threads) throws InterruptedException {
        ExecutorService service = Executors.newFixedThreadPool(threads);
        service.invokeAll(jobs);
        List<Runnable> leftovers = service.shutdownNow();
        assertThat(leftovers).isEmpty();
    }
}
