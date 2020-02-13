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

import com.google.protobuf.Duration;
import com.google.protobuf.util.Durations;
import io.spine.server.delivery.CatchUpProcess.DispatchCatchingUp;
import io.spine.server.projection.ProjectionRepository;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Preconditions2.checkPositive;

/**
 * A builder for {@link CatchUpProcess}.
 */
public final class CatchUpProcessBuilder<I> {

    private static final Duration DEFAULT_TURBULENCE_PERIOD = Durations.fromMillis(500);

    private final ProjectionRepository<I, ?, ?> repository;
    private @MonotonicNonNull CatchUpStorage storage;
    private @MonotonicNonNull DispatchCatchingUp<I> dispatchOp;
    private @MonotonicNonNull Duration turbulencePeriod;
    private int pageSize;

    CatchUpProcessBuilder(ProjectionRepository<I, ?, ?> repository) {
        this.repository = repository;
    }

    public ProjectionRepository<I, ?, ?> repository() {
        return repository;
    }

    CatchUpStorage storage() {
        return checkNotNull(storage);
    }

    CatchUpProcessBuilder<I> withStorage(CatchUpStorage storage) {
        this.storage = checkNotNull(storage);
        return this;
    }

    CatchUpProcessBuilder<I> withPageSize(int pageSize) {
        checkPositive(pageSize);
        this.pageSize = pageSize;
        return this;
    }

    int pageSize() {
        return pageSize;
    }

    Duration turbulencePeriod() {
        return checkNotNull(turbulencePeriod);
    }

    public CatchUpProcessBuilder<I> withDispatchOp(DispatchCatchingUp<I> operation) {
        this.dispatchOp = checkNotNull(operation);
        return this;
    }

    DispatchCatchingUp<I> dispatchOp() {
        return checkNotNull(dispatchOp);
    }

    public CatchUpProcess<I> build() {
        checkNotNull(storage);
        checkNotNull(dispatchOp);
        checkPositive(pageSize);

        if(turbulencePeriod == null) {
            turbulencePeriod = DEFAULT_TURBULENCE_PERIOD;
        }
        return new CatchUpProcess<>(this);
    }
}
