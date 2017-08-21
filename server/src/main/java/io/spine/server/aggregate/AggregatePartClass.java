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
import io.spine.server.entity.AbstractEntity;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.aggregate.AggregatePart.GenericParameter.AGGREGATE_ROOT;
import static java.lang.String.format;

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

    /**
     * Creates a new {@code AggregatePart} entity and sets it to the default state.
     *
     * @param ctor the constructor to use
     * @param <I>  the type of entity IDs
     * @param <A>  the type of the entity
     * @return an {@code AggregatePart} instance
     */
    static <I, A extends AbstractEntity<I, ?>> A create(Constructor<A> ctor,
                                                        AggregateRoot<I> root) {
        checkNotNull(ctor);
        checkNotNull(root);

        try {
            final A result = ctor.newInstance(root);
            return result;
        } catch (InvocationTargetException | InstantiationException |
                 IllegalAccessException | IllegalArgumentException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Obtains the constructor for the passed aggregate part class.
     *
     * <p>The part class must have a constructor with ID and {@code AggregateRoot} parameters.
     *
     * <p>Returns the constructor if the first parameter is aggregate ID
     * and the second constructor parameter is subtype of the {@code AggregateRoot}
     * For example:
     * <pre>{@code
     *
     * // A user-defined AggregateRoot:
     * class CustomAggregateRoot extends AggregateRoot{...}
     *
     * // An AggregatePart for the CustomAggregateRoot:
     * class CustomAggregatePart extends AggregatePart<...>{
     *
     *     // The expected constructor:
     *     CustomAggregatePart(AnAggregateId id, CustomAggregateRoot root){...}
     *     }
     * }
     * </pre>
     *
     * <p>Throws {@code IllegalStateException} in other cases.
     *
     * @param cls the {@code AggregatePart} class
     * @param <A> the {@code AggregatePart} type
     * @param <I> the ID type
     * @return the constructor
     * @throws IllegalStateException if the entity class does not have the required constructor
     */
    static <A extends AggregatePart<I, ?, ?, R>, I, R extends AggregateRoot<I>>
    Constructor<A> getConstructor(Class<A> cls) {
        checkNotNull(cls);

        final Class<R> aggregateRootClass = getRootClass(cls);
        try {
            final Constructor<A> ctor = cls.getDeclaredConstructor(aggregateRootClass);
            ctor.setAccessible(true);
            return ctor;
        } catch (NoSuchMethodException ignored) {
            throw noSuchConstructor(cls, aggregateRootClass);
        }
    }

    @Override
    protected Constructor<A> findConstructor() {
        @SuppressWarnings("unchecked") // The cast is protected by generic params.
        final Constructor<A> ctor = getConstructor(value());
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

    private static IllegalStateException noSuchConstructor(Class<?> aggregatePartClass,
                                                           Class<?> aggregateRootClass) {
        final String errMsg =
                format("%s class must declare a constructor with one parameter of the %s type.",
                       aggregatePartClass.getName(),
                       aggregateRootClass.getName());
        final NoSuchMethodException cause = new NoSuchMethodException(errMsg);
        throw new IllegalStateException(cause);
    }
}
