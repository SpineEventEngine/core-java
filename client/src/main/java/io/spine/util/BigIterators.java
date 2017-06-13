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

import javax.annotation.Nonnull;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A utility class for working with the {@linkplain Iterator Iterators}.
 *
 * <p>This utility kit contains the methods for creating lazily constructed instances of
 * {@link Iterable} and {@link Collection} based on a passed {@link Iterator}.
 *
 * @author Dmytro Dashenkov
 */
public final class BigIterators {

    private BigIterators() {
        // Prevent utility class instantiation.
    }

    /**
     * Creates a new {@link Iterable} on top of the passed {@link Iterator}.
     *
     * <p>The resulting {@link Iterable} is guaranteed to return the given instance of
     * {@link Iterator} in response to the first {@link Iterable#iterator() Iterable.iterator()}
     * call.
     *
     * <p>The resulting {@link Iterable} is designed for one-time usage. The second and subsequent
     * calls to {@link Iterable#iterator() Iterable.iterator()} throw {@link IllegalStateException}.
     *
     * @param iterator the backing iterator
     * @param <E>      the type of the elements
     * @return new instance of a one-off {@link Iterable}
     * @see #collect(Iterator) for more long-living wrapper implementation
     */
    public static <E> Iterable<E> toOneOffIterable(Iterator<E> iterator) {
        return new OneOffIterable<>(checkNotNull(iterator));
    }

    /**
     * Creates a new {@link Collection} on top of the passed {@link Iterator}.
     *
     * <p>The {@link Iterator} is evaluated lazily, i.e. the iterator elements will not be retrieved
     * until they are needed.
     *
     * <p>The elements are evaluated all at one when calling:
     * <ul>
     *     <li>{@link Collection#size() Collection.size()};
     *     <li>one of {@link Collection#toArray() Collection.toArray} overloads.
     * </ul>
     *
     * <p>It other cases, the count of the elements which are evaluated is equal to the number of
     * times the {@link Iterator#next() Iterator.next()} is called on the {@link Iterator} of this
     * {@link Collection}.
     *
     * <p>Unlike the {@linkplain #toOneOffIterable(Iterator) one-off Iterable}, the resulting
     * {@link Collection} is designed to be reused multiple times. Though, the cost of that reuse is
     * in keeping in memory all the elements which have already been evaluated.
     *
     * <p>Note that it's illegal to use multiple {@linkplain Iterator Iterators} on a single
     * {@code Collection} produced by this method. Example:
     * <pre>
     * {@code
     * final Collection&lt;Order&gt; lazyView = BigIterators.collect(myDatabaseCursorIterator);
     * final Iterator&lt;Order&gt; first = lazyView.iterator();
     * final Iterator&lt;Order&gt; second = lazyView.iterator();
     *
     * // ...
     *
     * first.next();
     * second.next(); // <-- throws a ConcurrentModificationException
     * }
     * </pre>
     *
     * <p>The right way to use multiple iterators is to initialize and use them sequentially.
     * <pre>
     * {@code
     * final Collection&lt;Order&gt; lazyView = BigIterators.collect(myDatabaseCursorIterator);
     * final Iterator&lt;Order&gt; first = lazyView.iterator();
     *
     * // ...
     *
     * first.next();
     *
     * final Iterator&lt;Order&gt; second = lazyView.iterator();
     *
     * // ...
     *
     * second.next(); // OK
     * }
     * </pre>
     *
     * <p>The code from the example above won't throw
     * a {@link java.util.ConcurrentModificationException ConcurrentModificationException} unless
     * the {@code first} iterator is used after the {@code second} iterator is created.
     *
     * <p>Please also note that the {@linkplain Collection#size() size} of the {@code Collection} is
     * calculated using an instance of {@code Iterator}, i.e. calling
     * {@link Collection#size() Collection.size()} while iterating the {@code Collection} may also
     * cause {@link java.util.ConcurrentModificationException ConcurrentModificationException}.
     *
     * <p>The order of the elements in the resulting {@code Collection} when iterating over is
     * the same as in the passed {@link Iterator}. The new elements added into
     * the {@code Collection} via {@link Collection#add(Object) Collection.add)} and
     * {@link Collection#addAll(Collection) Collection.addAll} methods are added to the start of
     * the sequence.
     *
     * @param iterator the data source iterator
     * @param <E>      the type of the elements
     * @return new instance of a lazily constructed {@link Collection}
     */
    public static <E> Collection<E> collect(Iterator<E> iterator) {
        return new DelegatingCollection<>(iterator);
    }

    /**
     * @see #toOneOffIterable(Iterator)
     */
    private static final class OneOffIterable<E> implements Iterable<E> {

        private final Iterator<E> iterator;

        private boolean used;

        private OneOffIterable(Iterator<E> iterator) {
            this.iterator = iterator;
        }

        /**
         * {@inheritDoc}
         *
         * <p>After the first call to this method, the iterator may be changed (i.e. the pointer
         * may be moved or some elements may be {@linkplain Iterator#remove() removed}). That's why
         * all the subsequent calls throw {@link IllegalStateException}.
         *
         * @throws IllegalStateException if called more than once
         */
        @Nonnull
        @Override
        public Iterator<E> iterator() {
            if (used) {
                throw new IllegalStateException("Reusing one-time Iterable.");
            }
            used = true;
            return iterator;
        }
    }

    /**
     * @see #collect(Iterator)
     */
    private static final class DelegatingCollection<E> extends AbstractCollection<E> {

        private final Iterator<E> iterator;

        private final Deque<E> popped;

        private DelegatingCollection(Iterator<E> iterator) {
            super();
            this.iterator = iterator;
            this.popped = new LinkedList<>();
        }

        @Override
        public Iterator<E> iterator() {
            return new Iter();
        }

        @Override
        public int size() {
            while (iterator.hasNext()) {
                popped.offer(iterator.next());
            }
            return popped.size();
        }

        @Override
        public boolean add(E e) {
            popped.push(e);
            return true;
        }

        @Override
        public boolean isEmpty() {
            return popped.isEmpty() && !iterator.hasNext();
        }

        private class Iter implements Iterator<E> {

            private final Iterator<E> sourceIterator;

            private final Iterator<E> poppedIterator;

            private boolean readFromSource;

            private Iter() {
                this.sourceIterator = DelegatingCollection.this.iterator;
                this.poppedIterator = popped.iterator();
                this.readFromSource = false;
            }

            @Override
            public boolean hasNext() {
                return (!readFromSource && poppedIterator.hasNext()) || sourceIterator.hasNext();
            }

            @Override
            public E next() {
                final E element;
                if (!readFromSource && poppedIterator.hasNext()) {
                    element = poppedIterator.next();
                } else if (sourceIterator.hasNext()) {
                    element = sourceIterator.next();
                    popped.offer(element);
                    readFromSource = true;
                } else {
                    throw new NoSuchElementException("The backing iterator is empty.");
                }
                return element;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Iterator.remove");
            }
        }
    }
}
