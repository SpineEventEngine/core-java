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
import org.spine3.server.entity.AbstractEntity;
import org.spine3.server.reflect.GenericTypeIndex;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.spine3.server.reflect.Classes.getGenericParameterType;

/**
 * A part of a larger aggregate.
 *
 * <p>Some business logic objects may be big enough.
 * If not all parts of such a business object need to be preserved at the same
 * time as business logic invariants, such an object can be split into several parts.
 *
 * <p>Each such part would:
 * <ul>
 *     <li>be a class derived from {@code AggregatePart}
 *     <li>have the same aggregate ID as other parts belonging to the same business object
 *     <li>have own state defined as a Protobuf message
 *     <li>managed by a separate repository class derived from {@link AggregateRepository}
 * </ul>
 *
 * <p>In order to access parts of the aggregate {@link AggregateRoot} should be used.
 *
 * <p>If your business logic cannot be split into parts that can be modified separately,
 * consider extending {@link Aggregate} instead of several {@code AggregatePart}s.
 *
 * @param <I> the type for IDs of this class of aggregates
 * @param <S> the type of the state held by the aggregate part
 * @param <B> the type of the aggregate part state builder
 * @param <R> the type of the aggregate root
 * @author Alexander Yevsyukov
 * @see Aggregate
 */
public abstract class AggregatePart<I,
                                    S extends Message,
                                    B extends Message.Builder,
                                    R extends AggregateRoot<I>>
                      extends Aggregate<I, S, B> {

    private final R root;

    /**
     * {@inheritDoc}
     */
    protected AggregatePart(R root) {
        super(root.getId());
        this.root = root;
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
     * Obtains a state of another {@code AggregatePart} by its class.
     *
     * @param partStateClass the class of the state of the part
     * @param <P>            the type of the part state
     * @return the state of the part or a default state if the state was not found
     * @throws IllegalStateException if a repository was not found,
     *                               or the ID type of the part state does not match
     *                               the ID type of the {@code root}
     */
    protected <P extends Message> P getPartState(Class<P> partStateClass) {
        final P partState = root.getPartState(partStateClass);
        return partState;
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

        final Class<R> aggregateRootClass = TypeInfo.getRootClass(cls);
        try {
            final Constructor<A> ctor = cls.getDeclaredConstructor(aggregateRootClass);
            ctor.setAccessible(true);
            return ctor;
        } catch (NoSuchMethodException ignored) {
            throw noSuchConstructor(cls, aggregateRootClass);
        }
    }

    private static IllegalStateException noSuchConstructor(Class<?> aggregatePartClass,
                                                           Class<?> aggregateRootClass) {
        final String errMsg =
                format("%s class must declare a constructor with %s parameter type.",
                       aggregatePartClass.getName(),
                       aggregateRootClass.getName());
        final NoSuchMethodException cause = new NoSuchMethodException(errMsg);
        throw new IllegalStateException(cause);
    }

    /**
     * Enumeration of generic type parameters of this class.
     */
    enum GenericParameter implements GenericTypeIndex {

        /** The index of the generic type {@code <I>}. */
        ID(0),

        /** The index of the generic type {@code <S>}. */
        STATE(1),

        /** The index of the generic type {@code <B>}. */
        STATE_BUILDER(2),

        /** The index of the generic type {@code <R>}. */
        AGGREGATE_ROOT(3);

        private final int index;

        GenericParameter(int index) {
            this.index = index;
        }

        @Override
        public int getIndex() {
            return this.index;
        }
    }

    /**
     * Provides type information on classes extending {@code AggregatePart}.
     */
    static class TypeInfo {

        private TypeInfo() {
            // Prevent construction from outside.
        }

        static <I, R extends AggregateRoot<I>> Class<R>
        getRootClass(Class<? extends AggregatePart<I, ?, ?, R>> aggregatePartClass) {
            checkNotNull(aggregatePartClass);
            final Class<R> rootClass = getGenericParameterType(
                    aggregatePartClass,
                    GenericParameter.AGGREGATE_ROOT.getIndex());
            return rootClass;
        }
    }
}
