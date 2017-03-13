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

package org.spine3.type;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Descriptors;
import com.google.protobuf.StringValue;
import org.junit.Test;
import org.spine3.base.Command;
import org.spine3.validate.internal.IfMissingOption;

import static org.junit.Assert.assertEquals;

/**
 * Provides only class-level tests.
 *
 * <p>Other methods of {@link TypeName} are just over {@link TypeUrl} which are tested by
 * its own set of tests.
 */
public class TypeNameShould {

    @Test
    public void pass_the_null_tolerance_check() {
        new NullPointerTester()
                .setDefault(Command.class, Command.getDefaultInstance())
                .setDefault(Descriptors.Descriptor.class, Command.getDefaultInstance()
                                                                 .getDescriptorForType())
                .testAllPublicStaticMethods(TypeName.class);
    }

    @Test
    public void return_simple_type_name() {
        assertEquals(StringValue.class.getSimpleName(), TypeName.of(StringValue.class)
                                                                .getSimpleName());
    }

    @Test
    public void return_simple_name_if_no_package() {
        // A msg type without Protobuf package
        final String name = IfMissingOption.class.getSimpleName();
        final TypeUrl typeUrl = TypeUrl.of(name);

        final String actual = TypeName.from(typeUrl)
                                      .getSimpleName();

        assertEquals(name, actual);
    }
}
