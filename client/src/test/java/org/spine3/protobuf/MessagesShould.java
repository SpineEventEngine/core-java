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
package org.spine3.protobuf;

import com.google.common.collect.Lists;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.util.JsonFormat;
import org.junit.Test;
import org.spine3.test.Tests;
import org.spine3.type.KnownTypes;
import org.spine3.type.TypeUrl;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.spine3.protobuf.Values.newStringValue;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;

public class MessagesShould {

    @Test
    public void have_private_utility_ctor() {
        assertHasPrivateParameterlessCtor(Messages.class);
    }

    @Test(expected = NullPointerException.class)
    public void toText_fail_on_null() {
        Messages.toText(Tests.<Message>nullRef());
    }

    @Test(expected = NullPointerException.class)
    public void toJson_fail_on_null() {
        Messages.toJson(Tests.<Message>nullRef());
    }

    @Test
    public void print_to_json() {
        final StringValue value = newStringValue("print_to_json");
        assertFalse(Messages.toJson(value)
                            .isEmpty());
    }

    @Test
    public void build_JsonFormat_registry_for_known_types() {
        final JsonFormat.TypeRegistry typeRegistry = Messages.forKnownTypes();

        final List<Descriptor> found = Lists.newLinkedList();
        for (TypeUrl typeUrl : KnownTypes.getAllUrls()) {
            final Descriptor descriptor = typeRegistry.find(typeUrl.getTypeName());
            if (descriptor != null) {
                found.add(descriptor);
            }
        }

        assertFalse(found.isEmpty());
    }
}
