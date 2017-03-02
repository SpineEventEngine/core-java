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

package org.spine3.server.aggregate;

import com.google.protobuf.Message;
import org.spine3.server.entity.EntityBuilder;

import java.lang.reflect.Constructor;

import static org.spine3.server.aggregate.AggregatePart.create;

/**
 * Utility class for building {@code AggregatePart}s for tests.
 *
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("MethodDoesntCallSuperMethod") // The call of the super method is not needed.
public class AggregatePartBuilder<A extends AggregatePart<I, S, ?, R>,
                                  I,
                                  S extends Message,
                                  R extends AggregateRoot<I>>
             extends AggregateBuilder<A, I, S> {

    private R aggregateRoot;

    /**
     * {@inheritDoc}
     */
    public AggregatePartBuilder() {
        super();
        // Have the constructor for easier location of usages.
    }

    public EntityBuilder<A, I, S> withRoot(R aggregateRoot) {
        this.aggregateRoot = aggregateRoot;
        return this;
    }

    @Override
    public AggregatePartBuilder<A, I, S, R> setResultClass(Class<A> entityClass) {
        super.setResultClass(entityClass);
        return this;
    }

    @Override
    protected A createEntity(I id) {
        final A result = create(getConstructor(), aggregateRoot);
        return result;
    }

    @Override
    protected Constructor<A> getConstructor() {
        final Constructor<A> constructor = AggregatePart.getConstructor(getResultClass());
        return constructor;
    }
}
