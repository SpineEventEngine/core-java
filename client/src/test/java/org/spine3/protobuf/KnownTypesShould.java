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
import com.google.protobuf.StringValue;
import org.junit.Test;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.protobuf.error.UnknownTypeException;
import org.spine3.type.ClassName;

import static org.junit.Assert.*;
import static org.spine3.protobuf.TypeUrl.composeTypeUrl;
import static org.spine3.test.Tests.hasPrivateUtilityConstructor;

/**
 * @author Alexander Litus
 */
@SuppressWarnings("InstanceMethodNamingConvention")
public class KnownTypesShould {

    @Test
    public void have_private_constructor() {
        assertTrue(hasPrivateUtilityConstructor(KnownTypes.class));
    }

    @Test
    public void return_known_proto_message_types() {
        final ImmutableSet<TypeUrl> types = KnownTypes.typeNames();

        assertFalse(types.isEmpty());
    }

    @Test
    public void return_java_class_name_by_proto_type_url() {
        final TypeUrl typeUrl = TypeUrl.of(Command.getDescriptor());

        final ClassName className = KnownTypes.getClassName(typeUrl);

        assertEquals(ClassName.of(Command.class), className);
    }

    @Test
    public void return_java_inner_class_name_by_proto_type_url() {
        final TypeUrl typeUrl = TypeUrl.of(CommandContext.Schedule.getDescriptor());

        final ClassName className = KnownTypes.getClassName(typeUrl);

        assertEquals(ClassName.of(CommandContext.Schedule.class), className);
    }

    @Test
    public void return_proto_type_url_by_java_class_name() {
        final ClassName className = ClassName.of(Command.class);

        final TypeUrl typeUrl = KnownTypes.getTypeUrl(className);

        assertEquals(TypeUrl.of(Command.getDescriptor()), typeUrl);
    }

    @Test
    public void return_proto_type_url_by_proto_type_name() {
        final TypeUrl typeUrlExpected = TypeUrl.of(StringValue.getDescriptor());

        final TypeUrl typeUrlActual = KnownTypes.getTypeUrl(typeUrlExpected.getTypeName());

        assertEquals(typeUrlExpected, typeUrlActual);
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_no_proto_type_url_by_java_class_name() {
        KnownTypes.getTypeUrl(ClassName.of(Exception.class));
    }

    @Test(expected = UnknownTypeException.class)
    public void throw_exception_if_no_java_class_name_by_type_url() {
        final TypeUrl unexpectedUrl = TypeUrl.of(composeTypeUrl("prefix", "unexpected.type"));
        KnownTypes.getClassName(unexpectedUrl);
    }
}
