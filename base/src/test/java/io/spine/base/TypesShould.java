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

package io.spine.base;

import com.google.common.reflect.TypeToken;
import com.google.common.testing.NullPointerTester;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import static io.spine.base.Types.listTypeOf;
import static io.spine.base.Types.mapTypeOf;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static org.junit.Assert.assertEquals;

/**
 * @author Illia Shepilov
 */
public class TypesShould {

    @Test
    public void pass_the_null_tolerance_check() {
        final NullPointerTester tester = new NullPointerTester();
        tester.testStaticMethods(Types.class, NullPointerTester.Visibility.PACKAGE);
    }

    @Test
    public void have_private_constructor() {
        assertHasPrivateParameterlessCtor(Types.class);
    }

    @Test
    @SuppressWarnings({"SerializableNonStaticInnerClassWithoutSerialVersionUID",
                       "SerializableInnerClassWithNonSerializableOuterClass"})
                        // It is OK for test method.
    public void create_map_type() {
        final Type type = mapTypeOf(String.class, Integer.class);
        final Type expectedType = new TypeToken<Map<String, Integer>>(){}.getType();
        assertEquals(expectedType, type);
    }

    @Test
    @SuppressWarnings({"SerializableNonStaticInnerClassWithoutSerialVersionUID",
                       "SerializableInnerClassWithNonSerializableOuterClass"})
                        // It is OK for test method.
    public void create_list_type() {
        final Type type = listTypeOf(String.class);
        final Type expectedType = new TypeToken<List<String>>(){}.getType();
        assertEquals(expectedType, type);
    }
}
