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
import org.spine3.server.entity.Entity;
import org.spine3.server.entity.EntityBuilder;

/**
 * Utility class for building test objects.
 *
 * @author Alexander Yevsyukov
 */
public class Given {

    private Given() {
        // Prevent instantiation of this utility class.
    }

    public static <E extends Entity<I, S>, I, S extends Message>
    EntityBuilder<E, I, S> entityOfClass(Class<E> entityClass) {
        final EntityBuilder<E, I, S> result = new EntityBuilder<>();
        result.setResultClass(entityClass);
        return result;
    }

    public static <A extends Aggregate<I, S, ?>, I, S extends Message>
           AggregateBuilder<A, I, S> aggregateOfClass(Class<A> aggregateClass) {
        final AggregateBuilder<A, I, S> result = new AggregateBuilder<>();
        result.setResultClass(aggregateClass);
        return result;
    }
}
