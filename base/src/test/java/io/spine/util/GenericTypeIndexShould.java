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

import com.google.common.testing.NullPointerTester;
import io.spine.test.Tests;
import org.junit.Test;

import static io.spine.util.GenericTypeIndex.Default.getArgument;
import static org.junit.Assert.assertEquals;

/**
 * @author Alexander Yevsyukov
 */
public class GenericTypeIndexShould {

    @Test
    public void have_utility_ctor_in_the_Default_class() {
        Tests.assertHasPrivateParameterlessCtor(GenericTypeIndex.Default.class);
    }

    @Test
    public void obtain_generic_argument_assuming_generic_superclass() {
        final Parametrized<Long, String> val = new Parametrized<Long, String>() {};
        assertEquals(Long.class, getArgument(val.getClass(), Base.class, 0));
        assertEquals(String.class, getArgument(val.getClass(), Base.class, 1));
    }

    @Test
    public void obtain_generic_argument_via_superclass() {
        assertEquals(String.class, getArgument(Leaf.class, Base.class, 0));
        assertEquals(Float.class, getArgument(Leaf.class, Base.class, 1));
    }

    @Test
    public void pass_null_tolerance_check() {
        new NullPointerTester()
                .testAllPublicStaticMethods(GenericTypeIndex.Default.class);
    }

    @SuppressWarnings({"EmptyClass", "unused"})
    private static class Base<T, K> {}

    private static class Parametrized<T, K> extends Base<T, K> {}

    private static class Leaf extends Base<String, Float> {}
}
