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

import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static com.google.common.collect.Lists.newArrayList;
import static io.spine.test.Verify.assertEmpty;
import static io.spine.test.Verify.assertNotEmpty;
import static io.spine.util.BigIterators.collect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Dmytro Dashenkov
 */
public class BigIteratorBasedCollectionShould {

    @Test
    public void be_mutable() {
        final Collection<String> col = empty();
        assertEmpty(col.iterator());

        final String newValue = "I am new.";
        assertTrue(col.add(newValue));

        final Iterator<String> iter = col.iterator();
        assertNotEmpty(iter);
        assertEquals(newValue, iter.next());
        assertEmpty(iter);
    }

    @Test(expected = ConcurrentModificationException.class)
    public void not_support_multiple_simultaneous_iterators() {
        final Collection<String> col = collect(newArrayList("1", "2", "3").iterator());
        final Iterator<String> first = col.iterator();
        final Iterator<String> second = col.iterator();

        first.next();
        second.next();
    }

    @SuppressWarnings("MethodWithMultipleLoops")
    @Test
    public void support_multiple_sequential_iterators() {
        final List<String> source = newArrayList("1", "2", "3");
        final Collection<String> col = collect(source.iterator());

        final Iterator<String> first = col.iterator();
        checkAllElements(first, source);

        final Iterator<String> second = col.iterator();
        checkAllElements(second, source);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void not_support_iterator_removal() {
        empty().iterator().remove();
    }

    @Test(expected = NoSuchElementException.class)
    public void fail_get_next_element_is_absent() {
        empty().iterator().next();
    }

    private static <E> Collection<E> empty() {
        return collect(Collections.<E>emptyIterator());
    }

    private static <E> void checkAllElements(Iterator<E> iterator, List<E> data) {
        for (E element : data) {
            assertTrue(iterator.hasNext());
            final E actualElement = iterator.next();
            assertEquals(element, actualElement);
        }
        assertFalse(iterator.hasNext());
    }
}
