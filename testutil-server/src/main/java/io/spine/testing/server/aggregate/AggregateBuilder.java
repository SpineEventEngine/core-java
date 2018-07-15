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

package io.spine.testing.server.aggregate;

import com.google.common.annotations.VisibleForTesting;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;
import io.spine.core.Version;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateTransaction;
import io.spine.testing.server.entity.EntityBuilder;

/**
 * Utility class for building aggregates for tests.
 *
 * @param <A> the type of the aggregate to build
 * @param <I> the type of aggregate IDs
 * @param <S> the type of the aggregate state
 * @author Alexander Yevsyukov
 */
@VisibleForTesting
public class AggregateBuilder<A extends Aggregate<I, S, ?>,
                              I,
                              S extends Message> extends EntityBuilder<A, I, S> {

    /**
     * {@inheritDoc}
     */
    public AggregateBuilder() {
        super();
        // Have the constructor for easier location of usages.
    }

    @CanIgnoreReturnValue
    @Override
    public AggregateBuilder<A, I, S> setResultClass(Class<A> entityClass) {
        super.setResultClass(entityClass);
        return this;
    }

    @Override
    protected void setState(A result, S state, Version version) {
        AggregateTransaction.startWith(result, state, version)
                            .commit();
    }
}
