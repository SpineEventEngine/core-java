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

package io.spine.util;

import com.google.common.reflect.TypeToken;

import javax.annotation.CheckReturnValue;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base interface for enumerations on generic parameters of types.
 *
 * <p>Example of implementing an enumeration for generic parameters:
 * <pre>
 * {@code
 * public abstract class Tuple<K, V> {
 *      ...
 *     public enum GenericParameter extends GenericTypeIndex<Tuple> {
 *
 *         // <K> param has index 0
 *         KEY(0),
 *
 *         // <V> param has index 1
 *         VALUE(1);
 *
 *         private final int index;
 *
 *         GenericParameter(int index) { this.index = index; }
 *
 *         {@literal @}Override
 *         public int getIndex() { return index; }
 *
 *         {@literal @}Override
 *         public Class<?> getArgumentIn(Class<? extends Tuple> derivedClass) {
 *             return Default.getArgument(this, derivedClass);
 *         }
 *     }
 * }
 * }
 * </pre>
 * @param <C> the type in which class the generic index is declared
 * @author Alexander Yevsyukov
 */
public interface GenericTypeIndex<C> {

    /**
     * Obtains a zero-based index of a generic parameter of a type.
     */
    int getIndex();

    /**
     * Obtains the class of the generic type argument.
     *
     * @param cls the class to inspect
     * @return the argument class
     */
    Class<?> getArgumentIn(Class<? extends C> cls);

    /**
     * Allows to obtain argument value of the {@code <C>} generic parameter for
     * {@code GenericTypeIndex} implementations.
     */
    class Default {

        //
        // Replace this class with the default interface method when migrating to Java 8.
        //

        private Default() {
            // Prevent instantiation of this utility class.
        }

        /**
         * Obtains a generic argument of the passed class.
         *
         * @param index the index of the generic argument
         * @param cls   the class in which inheritance chain the argument is specified
         * @param <C>   the type of the class
         * @return      the value of the generic argument
         */
        public static <C> Class<?> getArgument(GenericTypeIndex<C> index, Class<? extends C> cls) {
            checkNotNull(index);
            checkNotNull(cls);
            @SuppressWarnings("unchecked") /* The type is ensured by the declaration of
                                              the GenericTypeIndex interface. */
            final Class<C> superclass = (Class<C>)
                    getArgument(index.getClass(), GenericTypeIndex.class, 0);
            final Class<?> result =
                    getArgument(cls, superclass, index.getIndex());
            return result;
        }

        /**
         * Obtains the class of a generic type argument which is specified in the inheritance chain
         * of the passed class.
         *
         * @param cls               the end class for which we find the generic argument
         * @param genericSuperclass the superclass of the passed which has generic parameters
         * @param argNumber         the index of the generic parameter in the superclass
         * @param <T>               the type of superclass
         * @return the class of the generic type argument
         */
        @CheckReturnValue
        static <T> Class<?> getArgument(Class<? extends T> cls,
                                        Class<T> genericSuperclass,
                                        int argNumber) {
            checkNotNull(cls);
            checkNotNull(genericSuperclass);

            final TypeToken<?> supertypeToken = TypeToken.of(cls)
                                                         .getSupertype(genericSuperclass);
            final ParameterizedType genericSupertype =
                    (ParameterizedType) supertypeToken.getType();
            final Type[] typeArguments = genericSupertype.getActualTypeArguments();
            final Type typeArgument = typeArguments[argNumber];
            @SuppressWarnings("unchecked") // The type is ensured by the calling code.
            final Class<?> result = (Class<?>) typeArgument;
            return result;
        }
    }
}
