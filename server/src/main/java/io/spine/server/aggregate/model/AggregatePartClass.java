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

package io.spine.server.aggregate.model;

import com.google.errorprone.annotations.concurrent.LazyInit;
import io.spine.server.BoundedContext;
import io.spine.server.aggregate.AggregatePart;
import io.spine.server.aggregate.AggregateRoot;
import io.spine.server.entity.EntityFactory;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.aggregate.AggregatePart.GenericParameter.AGGREGATE_ROOT;

/**
 * Provides type information on an aggregate part class.
 *
 * @param <A> the type of aggregate parts.
 */
public final class AggregatePartClass<A extends AggregatePart> extends AggregateClass<A> {

    private static final long serialVersionUID = 0L;

    /** The model class of the aggregate root to which the aggregate part belongs. */
    @LazyInit
    private transient volatile @MonotonicNonNull Class<? extends AggregateRoot<?>> rootClass;

    /** Creates new instance. */
    private AggregatePartClass(Class<A> cls) {
        super(cls);
    }

    /**
     * Obtains an aggregate part class for the passed raw class.
     */
    public static <A extends AggregatePart>
    AggregatePartClass<A> asAggregatePartClass(Class<A> cls) {
        checkNotNull(cls);
        AggregatePartClass<A> result = (AggregatePartClass<A>)
                get(cls, AggregatePartClass.class, () -> new AggregatePartClass<>(cls));
        return result;
    }

    /**
     * Obtains the aggregate root class of this part class.
     */
    @SuppressWarnings("unchecked") // The type is ensured by the class declaration.
    public Class<? extends AggregateRoot<?>> rootClass() {
        if (rootClass == null) {
            rootClass = (Class<? extends AggregateRoot<?>>) AGGREGATE_ROOT.argumentIn(value());
        }
        return rootClass;
    }

    /**
     * Creates a new instance of the factory for creating aggregate parts of this class.
     */
    @Override
    protected EntityFactory<A> createFactory() {
        return new PartFactory<>(value(), rootClass());
    }

    /**
     * Creates a new {@code AggregateRoot} within the passed Bounded Context.
     */
    @SuppressWarnings("TypeParameterUnusedInFormals")
    // The returned type _is_ bound with the type of identifiers.
    public <I, R extends AggregateRoot<I>> R createRoot(BoundedContext bc, I aggregateId) {
        checkNotNull(bc);
        checkNotNull(aggregateId);
        @SuppressWarnings("unchecked") // Protected by generic parameters of the calling code.
        Class<R> rootClass = (Class<R>) rootClass();
        R result;
        try {
            Constructor<R> ctor = rootClass.getDeclaredConstructor(BoundedContext.class,
                                                                   aggregateId.getClass());
            ctor.setAccessible(true);
            result = ctor.newInstance(bc, aggregateId);
        } catch (NoSuchMethodException | InvocationTargetException |
                InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        return result;
    }
}
