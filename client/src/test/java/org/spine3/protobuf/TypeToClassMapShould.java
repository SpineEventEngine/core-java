/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.protobuf;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.spine3.base.Command;
import org.spine3.protobuf.error.UnknownTypeException;
import org.spine3.type.ClassName;
import org.spine3.type.TypeName;

import static org.junit.Assert.*;
import static org.spine3.test.Tests.hasPrivateUtilityConstructor;

/**
 * @author Alexander Litus
 */
@SuppressWarnings("InstanceMethodNamingConvention")
public class TypeToClassMapShould {

    @Test
    public void test() {
        assertTrue(hasPrivateUtilityConstructor(TypeToClassMap.class));
    }

    @Test
    public void return_known_proto_message_types() {
        final ImmutableSet<TypeName> types = TypeToClassMap.knownTypes();

        assertFalse(types.isEmpty());
    }

    @Test
    public void return_java_class_name_by_proto_type_name() {
        final TypeName typeName = TypeName.of(Command.getDescriptor());

        final ClassName className = TypeToClassMap.get(typeName);

        assertEquals(ClassName.of(Command.class), className);
    }

    @Test
    public void return_proto_type_name_by_java_class_name() {
        final ClassName className = ClassName.of(Command.class);

        final TypeName typeName = TypeToClassMap.get(className);

        assertEquals(TypeName.of(Command.getDescriptor()), typeName);
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_no_proto_type_name_by_java_class_name() {
        TypeToClassMap.get(ClassName.of(Exception.class));
    }

    @Test(expected =  UnknownTypeException.class)
    public void throw_exception_if_no_java_class_name_by_proto_type_name() {
        TypeToClassMap.get(TypeName.of("unexpected.type"));
    }
}
