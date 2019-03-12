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

package io.spine.server.aggregate;

import io.spine.annotation.Internal;
import io.spine.server.aggregate.model.AggregatePartClass;

import static io.spine.server.aggregate.model.AggregatePartClass.asAggregatePartClass;

/**
 * Default implementation of {@code AggregatePartRepository}.
 */
@Internal
public final class DefaultAggregatePartRepository<I,
                                                  A extends AggregatePart<I, ?, ?, R>,
                                                  R extends AggregateRoot<I>>
    extends AggregatePartRepository<I, A, R> {

    private final AggregatePartClass<A> modelClass;

    /**
     * Creates a new repository for managing aggregate parts of the passed class.
     */
    public DefaultAggregatePartRepository(Class<A> cls) {
        super();
        this.modelClass = asAggregatePartClass(cls);
    }

    /**
     * Obtains the class of aggregates parts managed by this repository.
     */
    @Override
    protected AggregatePartClass<A> entityModelClass() {
        return modelClass;
    }
}
