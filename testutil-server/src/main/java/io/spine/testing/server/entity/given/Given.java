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

package io.spine.testing.server.entity.given;

import com.google.protobuf.Message;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregatePart;
import io.spine.server.aggregate.AggregateRoot;
import io.spine.server.entity.AbstractVersionableEntity;
import io.spine.server.procman.ProcessManager;
import io.spine.server.projection.Projection;
import io.spine.testing.server.aggregate.AggregateBuilder;
import io.spine.testing.server.aggregate.AggregatePartBuilder;
import io.spine.testing.server.entity.EntityBuilder;
import io.spine.testing.server.procman.ProcessManagerBuilder;
import io.spine.testing.server.projection.ProjectionBuilder;
import io.spine.validate.ValidatingBuilder;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utility class for building test objects.
 *
 * @author Alexander Yevsyukov
 */
public class Given {

    /** Prevents instantiation of this utility class. */
    private Given() {
    }

    /**
     * Creates a builder for an {@code Entity}.
     */
    public static <E extends AbstractVersionableEntity<I, S>, I, S extends Message>
    EntityBuilder<E, I, S> entityOfClass(Class<E> entityClass) {
        checkNotNull(entityClass);
        EntityBuilder<E, I, S> result = new EntityBuilder<>();
        result.setResultClass(entityClass);
        return result;
    }

    /**
     * Creates dynamic builder for building an {@code Aggregate}.
     */
    public static <A extends Aggregate<I, S, ?>, I, S extends Message>
    AggregateBuilder<A, I, S> aggregateOfClass(Class<A> aggregateClass) {
        checkNotNull(aggregateClass);
        AggregateBuilder<A, I, S> result = new AggregateBuilder<>();
        result.setResultClass(aggregateClass);
        return result;
    }

    /**
     * Creates a builder for an {@code AggregatePart}.
     */
    public static <A extends AggregatePart<I, S, ?, R>,
                   I,
                   S extends Message,
                   R extends AggregateRoot<I>>
    AggregatePartBuilder<A, I, S, R> aggregatePartOfClass(Class<A> partClass) {
        checkNotNull(partClass);
        AggregatePartBuilder<A, I, S, R> result = new AggregatePartBuilder<>();
        result.setResultClass(partClass);
        return result;
    }

    /**
     * Creates a builder for a {@code Projection}.
     */
    public static <P extends Projection<I, S, B>,
                   I,
                   S extends Message,
                   B extends ValidatingBuilder<S, ? extends Message.Builder>>
    ProjectionBuilder<P, I, S, B> projectionOfClass(Class<P> projectionClass) {
        checkNotNull(projectionClass);
        ProjectionBuilder<P, I, S, B> result = new ProjectionBuilder<>();
        result.setResultClass(projectionClass);
        return result;
    }

    /**
     * Creates a builder for a {@code ProcessManager}.
     */
    public static <P extends ProcessManager<I, S, B>,
                   I,
                   S extends Message,
                   B extends ValidatingBuilder<S, ?>>
    ProcessManagerBuilder<P, I, S, B> processManagerOfClass(Class<P> pmClass) {
        checkNotNull(pmClass);
        ProcessManagerBuilder<P, I, S, B> result = new ProcessManagerBuilder<>();
        result.setResultClass(pmClass);
        return result;
    }
}
