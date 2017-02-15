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

package org.spine3.test;

import com.google.protobuf.Message;
import org.spine3.server.aggregate.Aggregate;
import org.spine3.server.aggregate.AggregateBuilder;
import org.spine3.server.aggregate.AggregatePart;
import org.spine3.server.aggregate.AggregatePartBuilder;
import org.spine3.server.entity.Entity;
import org.spine3.server.entity.EntityBuilder;
import org.spine3.server.entity.status.EntityStatus;
import org.spine3.server.procman.ProcessManager;
import org.spine3.server.procman.ProcessManagerBuilder;
import org.spine3.server.projection.Projection;
import org.spine3.server.projection.ProjectionBuilder;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utility class for building test objects.
 *
 * @author Alexander Yevsyukov
 */
public class Given {

    private Given() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Creates a builder for an {@code Entity}.
     */
    public static <E extends Entity<I, S, EntityStatus>, I, S extends Message>
    EntityBuilder<E, I, S> entityOfClass(Class<E> entityClass) {
        checkNotNull(entityClass);
        final EntityBuilder<E, I, S> result = new EntityBuilder<>();
        result.setResultClass(entityClass);
        return result;
    }

    /**
     * Creates dynamic builder for building an {@code Aggregate}.
     */
    public static <A extends Aggregate<I, S, ?>, I, S extends Message>
           AggregateBuilder<A, I, S> aggregateOfClass(Class<A> aggregateClass) {
        checkNotNull(aggregateClass);
        final AggregateBuilder<A, I, S> result = new AggregateBuilder<>();
        result.setResultClass(aggregateClass);
        return result;
    }

    /**
     * Creates a builder for an {@code AggregatePart}.
     */
    public static <A extends AggregatePart<I, S, ?>, I, S extends Message>
           AggregatePartBuilder<A, I, S> aggregatePartOfClass(Class<A> partClass) {
        checkNotNull(partClass);
        final AggregatePartBuilder<A, I, S> result = new AggregatePartBuilder<>();
        result.setResultClass(partClass);
        return result;
    }

    /**
     * Creates a builder for a {@code Projection}.
     */
    public static <P extends Projection<I, S>, I, S extends Message>
           ProjectionBuilder<P, I, S> projectionOfClass(Class<P> projectionClass) {
        checkNotNull(projectionClass);
        final ProjectionBuilder<P, I, S> result = new ProjectionBuilder<>();
        result.setResultClass(projectionClass);
        return result;
    }

    /**
     * Creates a builder for a {@code ProcessManager}.
     */
    public static <P extends ProcessManager<I, S>, I, S extends Message>
           ProcessManagerBuilder<P, I, S> processManagerOfClass(Class<P> pmClass) {
        checkNotNull(pmClass);
        final ProcessManagerBuilder<P, I, S> result = new ProcessManagerBuilder<>();
        result.setResultClass(pmClass);
        return result;
    }
}
