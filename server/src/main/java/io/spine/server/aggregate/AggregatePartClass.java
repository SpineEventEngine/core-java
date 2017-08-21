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

package io.spine.server.aggregate;

import io.spine.annotation.Internal;
import io.spine.server.BoundedContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.aggregate.AggregatePart.GenericParameter.AGGREGATE_ROOT;

/**
 * Provides type information on an aggregate part class.
 *
 * @param <A> the type of aggregate parts.
 * @author Alexander Yevsyukov
 */
@Internal
public final class AggregatePartClass<A extends AggregatePart> extends AggregateClass<A> {

    private static final long serialVersionUID = 0L;

    private final Class<? extends AggregateRoot> rootClass;

    /** Creates new instance. */
    public AggregatePartClass(Class<? extends A> cls) {
        super(cls);
        @SuppressWarnings("unchecked") // Protected by generic parameters of the calling code.
        final Class<? extends AggregatePart<Object, ?, ?, AggregateRoot<Object>>> cast =
                (Class<? extends AggregatePart<Object, ?, ?, AggregateRoot<Object>>>) cls;
        this.rootClass = getRootClass(cast);
    }

    static <I, R extends AggregateRoot<I>> Class<R>
    getRootClass(Class<? extends AggregatePart<I, ?, ?, R>> aggregatePartClass) {
        checkNotNull(aggregatePartClass);
        @SuppressWarnings("unchecked") // The type is ensured by the class declaration.
        final Class<R> rootClass =
                (Class<R>) AGGREGATE_ROOT.getArgumentIn(aggregatePartClass);
        return rootClass;
    }

    @Override
    protected Constructor<A> findConstructor() {
        @SuppressWarnings("unchecked") // The cast is protected by generic params.
        final Constructor<A> ctor = AggregatePart.getConstructor(value());
        return ctor;
    }

    /**
     * Obtains the aggregate root class of this part class.
     */
    public Class<? extends AggregateRoot> getRootClass() {
        return rootClass;
    }

    /**
     * Creates a new {@code AggregateRoot} withing the passed Bounded Context.
     */
    public <I, R extends AggregateRoot<I>> R createRoot(BoundedContext bc, I aggregateId) {
        checkNotNull(bc);
        checkNotNull(aggregateId);
        @SuppressWarnings("unchecked") // Protected by generic parameters of the calling code.
        final Class<R> rootClass = (Class<R>) getRootClass();
        R result;
        try {
            final Constructor<R> ctor =
                    rootClass.getDeclaredConstructor(bc.getClass(), aggregateId.getClass());
            ctor.setAccessible(true);
            result = ctor.newInstance(bc, aggregateId);
        } catch (NoSuchMethodException | InvocationTargetException |
                 InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        return result;
    }
}
