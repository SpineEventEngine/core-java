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

import org.junit.Test;

import java.util.Collections;
import java.util.Iterator;

import static com.google.common.collect.Lists.newArrayList;
import static io.spine.util.BigIterators.toOneOffIterable;
import static org.junit.Assert.assertSame;

/**
 * @author Dmytro Dashenkov
 */
public class BigIteatorBasedOneOffIterableShould {

    @Test(expected = IllegalStateException.class)
    public void fail_to_be_reused() {
        final Iterable<?> emptyIterable = empty();
        emptyIterable.iterator();
        emptyIterable.iterator();
    }

    @Test
    public void return_the_same_iterator() {
        final Iterator<?> iterator = newArrayList("el1", "el2").iterator();
        final Iterable<?> iterable = toOneOffIterable(iterator);
        assertSame(iterator, iterable.iterator());
    }

    private static <E> Iterable<E> empty() {
        return toOneOffIterable(Collections.<E>emptyIterator());
    }
}
