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
import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A factory of {@link Iterable} instances.
 *
 * @author Dmytro Dashenkov
 */
public final class MoreIterables {

    private MoreIterables() {
        // Prevent utility class instantiation.
    }

    /**
     * Creates a new {@link Iterable} on top of the passed {@link Iterator}.
     *
     * <p>The resulting {@link Iterable} is guaranteed to return the given instance of
     * {@link Iterator} in response to {@link Iterable#iterator() Iterable.iterator()} call.
     *
     * @param iterator the backing iterator
     * @param <E> the type of the values of both the backing iterator and the resulting iterable.
     * @return new instance of {@link Iterable}.
     */
    public static <E> Iterable<E> onTopOf(Iterator<E> iterator) {
        return new DelegateIterable<>(checkNotNull(iterator));
    }

    private static class DelegateIterable<E> implements Iterable<E> {

        private final Iterator<E> iterator;

        private DelegateIterable(Iterator<E> iterator) {
            this.iterator = iterator;
        }

        @Nonnull
        @Override
        public Iterator<E> iterator() {
            return iterator;
        }
    }
}
