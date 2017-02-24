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

import org.spine3.server.BoundedContext;
import org.spine3.server.entity.AbstractEntity;
import org.spine3.server.reflect.Classes;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * The utility class for working with aggregates.
 *
 * @author Illia Shepilov
 */
class Aggregates {

    private Aggregates() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Obtains the constructor for the passed aggregate part class.
     *
     * <p>The part class must have a constructor with ID and {@code AggregateRoot} parameters.
     *
     * <p>Returns the constructor if the first parameter is aggregate ID
     * and the second constructor parameter is subtype of the {@code AggregateRoot}
     *      For example:
     *          <pre>{@code
     *
     *              // A user-defined AggregateRoot:
     *              class CustomAggregateRoot extends AggregateRoot{...}
     *
     *              // An AggregatePart for the CustomAggregateRoot:
     *              class CustomAggregatePart extends AggregatePart<...>{
     *
     *                  // The expected constructor:
     *                  CustomAggregatePart(AnAggregateId id, CustomAggregateRoot root){...}
     *              }
     *          }
     *          </pre>
     *
     * <p>Throws {@code IllegalStateException} in other cases.
     *
     * @param aggregatePartClass the {@code AggregatePart} class
     * @param <A>                the {@code AggregatePart} type
     * @param <I>                the ID type
     * @return the {@code AggregatePart} constructor
     * @throws IllegalStateException if the entity class does not have the required constructor
     */
    static <A extends AggregatePart<I, ?, ?, ?>, I> Constructor<A>
    getAggregatePartConstructor(Class<A> aggregatePartClass) {
        checkNotNull(aggregatePartClass);

        final Class<?> aggregateRootClass = Classes.getGenericParameterType(aggregatePartClass, 3);
        try {
            final Constructor<A> constructor =
                    aggregatePartClass.getDeclaredConstructor(aggregateRootClass);
            constructor.setAccessible(true);
            return constructor;
        } catch (NoSuchMethodException ignored) {
            throw noSuchConstructor(aggregatePartClass.getName(), aggregateRootClass.getName());
        }
    }

    private static IllegalStateException noSuchConstructor(String aggregatePartClass,
                                                           String aggregateRootClass) {
        final String errMsg =
                format("%s class must declare a constructor " +
                       "with %s parameter type.", aggregatePartClass, aggregateRootClass);
        throw new IllegalStateException(new NoSuchMethodException(errMsg));
    }

    /**
     * Creates a new {@code AggregateRoot} entity and sets it to the default state.
     *
     * @param id        the ID of the {@code AggregatePart} is managed by {@code AggregateRoot}
     * @param ctx       the {@code BoundedContext} to use
     * @param rootClass the class of the {@code AggregateRoot}
     * @param <I>       the type of entity IDs
     * @return an {@code AggregateRoot} instance
     */
    static <I, R extends AggregateRoot<I>> R createAggregateRoot(I id,
                                                                 BoundedContext ctx,
                                                                 Class<R> rootClass) {
        checkNotNull(id);
        checkNotNull(ctx);
        checkNotNull(rootClass);

        try {
            final Constructor<R> rootConstructor =
                    rootClass.getDeclaredConstructor(ctx.getClass(), id.getClass());
            rootConstructor.setAccessible(true);
            final R root = rootConstructor.newInstance(ctx, id);
            return root;
        } catch (NoSuchMethodException | InvocationTargetException |
                InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Creates a new {@code AggregatePart} entity and sets it to the default state.
     *
     * @param ctor the constructor to use
     * @param <I>  the type of entity IDs
     * @param <A>  the type of the entity
     * @return an {@code AggregatePart} instance
     */
    static <I, A extends AbstractEntity<I, ?>> A createAggregatePart(Constructor<A> ctor,
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
}
