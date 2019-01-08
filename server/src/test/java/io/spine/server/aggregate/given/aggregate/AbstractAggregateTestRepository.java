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

package io.spine.server.aggregate.given.aggregate;

import io.spine.core.TenantId;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.tenant.TenantAwareRunner;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * A test repository which can load an aggregate by its ID and fail the test if the
 * aggregate with such ID is not found.
 *
 * @param <I> the type of aggregate identifiers
 * @param <A> the type of aggregates
 */
public class AbstractAggregateTestRepository<I, A extends Aggregate<I, ?, ?>>
        extends AggregateRepository<I, A> {

    public A loadAggregate(I id) {
        Optional<A> optional = find(id);
        if (!optional.isPresent()) {
            fail("Aggregate not found.");
        }
        return optional.get();
    }

    public A loadAggregate(TenantId tenantId, I id) {
        TenantAwareRunner runner = TenantAwareRunner.with(tenantId);
        A result = runner.evaluate(() -> loadAggregate(id));
        return result;
    }
}
