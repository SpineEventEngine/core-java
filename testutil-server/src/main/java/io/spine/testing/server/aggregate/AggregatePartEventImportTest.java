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

import com.google.protobuf.Message;
import io.spine.base.EventMessage;
import io.spine.server.aggregate.AggregatePart;
import io.spine.server.aggregate.AggregateRoot;

/**
 * The implementation base for testing import of a single event into an {@link AggregatePart}.
 *
 * @param <I> ID message of the aggregate
 * @param <E> type of the event to test
 * @param <S> the aggregate part state type
 * @param <P> the {@link AggregatePart} type
 * @param <R> the {@link AggregateRoot} type
 *
 * @see io.spine.server.aggregate.Apply#allowImport()
 */
public abstract class AggregatePartEventImportTest<I,
                                                   E extends EventMessage,
                                                   S extends Message,
                                                   P extends AggregatePart<I, S, ?, R>,
                                                   R extends AggregateRoot<I>>
        extends AggregateEventImportTest<I, E, S, P> {

    protected AggregatePartEventImportTest(I aggregateId, E commandMessage) {
        super(aggregateId, commandMessage);
    }

    /**
     * Instantiates a new aggregate root with the given ID.
     *
     * <p>A typical implementation:
     * <pre>
     *     {@code
     *     \@Override
     *     protected MyAggregateRoot newRoot(MyId id) {
     *         return new MyAggregateRoot(id);
     *     }
     *     }
     * </pre>
     *
     * @param id the aggregate ID
     * @return new instance of root
     */
    protected abstract R newRoot(I id);

    /**
     * Instantiates a new aggregate part with the given root.
     *
     * <p>A typical implementation:
     * <pre>
     *     {@code
     *     \@Override
     *     protected MyAggregatePart newRoot(MyAggregateRoot root) {
     *         return new MyAggregatePart(root);
     *     }
     *     }
     * </pre>
     *
     * @param root the aggregate root
     * @return new instance of part
     */
    protected abstract P newPart(R root);

    /**
     * Creates a new aggregate part with the given ID.
     *
     * <p>The resulting part has default state and
     * {@link io.spine.core.Versions#zero() Versions.zero()} version.
     *
     * @param id the aggregate ID
     * @return new part instance
     */
    protected final P newPart(I id) {
        R root = newRoot(id);
        P part = newPart(root);
        return part;
    }
}
