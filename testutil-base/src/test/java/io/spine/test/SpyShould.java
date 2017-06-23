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

package io.spine.test;

import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Alexander Yevsyukov
 */
public class SpyShould {

    private static final String FIELD_NAME = "list";

    private List<String> list;

    @Before
    public void setUp() {
        list = Lists.newArrayList("a", "b", "c");
    }

    @After
    public void tearDown() {
        list = null;
    }
    
    @Test
    public void inject_by_class() {
        final List spy = Spy.ofClass(List.class)
                            .on(this);
        assertSpy(spy);
    }

    @Test
    public void inject_by_name() {
        final List spy = Spy.ofClass(List.class)
                            .on(this, FIELD_NAME);
        assertSpy(spy);
    }

    private void assertSpy(List spy) {
        assertNotNull(spy);

        // Verify that the field is injected.
        assertSame(list, spy);

        // Check that we got a Mockito spy.
        assertEquals(3, list.size());
        verify(spy, times(1)).size();
    }

    @Test(expected = IllegalArgumentException.class)
    public void propagate_exception() {
        Spy.ofClass(Number.class)
           .on(this, FIELD_NAME);
    }
}
