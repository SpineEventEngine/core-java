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
import com.google.protobuf.Any;
import com.google.protobuf.Duration;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import org.junit.Test;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.protobuf.error.UnknownTypeException;
import org.spine3.type.ClassName;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
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
    public void return_known_proto_message_type_urls() {
        final ImmutableSet<TypeUrl> typeUrls = KnownTypes.getTypeUrls();

        assertFalse(typeUrls.isEmpty());
    }

    @Test
    public void return_spine_java_class_names_by_proto_type_urls() {
        assertHasClassNameByTypeUrlOf(Command.class);
        assertHasClassNameByTypeUrlOf(CommandContext.class);
        assertHasClassNameByTypeUrlOf(Event.class);
        assertHasClassNameByTypeUrlOf(EventContext.class);
        assertHasClassNameByTypeUrlOf(org.spine3.base.Error.class);
    }

    @Test
    public void return_google_java_class_names_by_proto_type_urls() {
        assertHasClassNameByTypeUrlOf(Any.class);
        assertHasClassNameByTypeUrlOf(Timestamp.class);
        assertHasClassNameByTypeUrlOf(Duration.class);
        assertHasClassNameByTypeUrlOf(Empty.class);
    }

    private static void assertHasClassNameByTypeUrlOf(Class<? extends Message> msgClass) {
        final TypeUrl typeUrl = TypeUrl.of(msgClass);

        final ClassName className = KnownTypes.getClassName(typeUrl);

        assertEquals(ClassName.of(msgClass), className);
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

    @Test
    public void have_all_valid_java_class_names() {
        final Set<ClassName> classes = KnownTypes.getJavaClasses();
        assertFalse(classes.isEmpty());
        for (ClassName name : classes) {
            try {
                Class.forName(name.value());
            } catch (ClassNotFoundException e) {
                fail("Invalid Java class name in the '.properties' file: " + name.value());
            }
        }
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
