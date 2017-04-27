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

package org.spine3.json;

import com.google.common.collect.Lists;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.util.JsonFormat;
import org.junit.Test;
import org.spine3.test.Tests;
import org.spine3.type.KnownTypes;
import org.spine3.type.TypeUrl;
import org.spine3.users.UserId;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.json.Json.fromJson;
import static org.spine3.json.Json.toCompactJson;
import static org.spine3.json.Json.toJson;
import static org.spine3.protobuf.Wrappers.newStringValue;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;

/**
 * @author Alexander Yevsyukov
 */
public class JsonShould {

    @Test
    public void have_utility_ctor() {
        assertHasPrivateParameterlessCtor(Json.class);
    }

    @Test
    public void build_JsonFormat_registry_for_known_types() {
        final JsonFormat.TypeRegistry typeRegistry = Json.typeRegistry();

        final List<Descriptors.Descriptor> found = Lists.newLinkedList();
        for (TypeUrl typeUrl : KnownTypes.getAllUrls()) {
            final Descriptors.Descriptor descriptor = typeRegistry.find(typeUrl.getTypeName());
            if (descriptor != null) {
                found.add(descriptor);
            }
        }

        assertFalse(found.isEmpty());
    }

    @Test(expected = NullPointerException.class)
    public void toJson_fail_on_null() {
        toJson(Tests.<Message>nullRef());
    }

    @Test
    public void print_to_json() {
        final StringValue value = newStringValue("print_to_json");
        assertFalse(toJson(value).isEmpty());
    }

    @Test
    public void print_to_compact_json() {
        final String idValue = newUuid();
        final UserId userId = UserId.newBuilder()
                                    .setValue(idValue)
                                    .build();
        final String result = toCompactJson(userId);
        assertFalse(result.isEmpty());
        assertFalse(result.contains(System.lineSeparator()));
    }

    @Test
    public void parse_from_json() {
        final String idValue = newUuid();
        final String jsonMessage = String.format("{value:%s}", idValue);
        final UserId userId = fromJson(jsonMessage, UserId.class);
        assertNotNull(userId);
        assertEquals(idValue, userId.getValue());
    }
}
