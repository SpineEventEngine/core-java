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
package org.spine3.client;

import com.google.common.testing.NullPointerTester;
import org.junit.Test;
import org.spine3.test.queries.TestEntity;
import org.spine3.type.TypeUrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;

/**
 * @author Alex Tymchenko
 */
public class QueriesShould {

    private static final String TARGET_ENTITY_TYPE_URL =
            "type.spine3.org/spine.test.queries.TestEntity";

    @Test
    public void have_private_constructor() {
        assertHasPrivateParameterlessCtor(Queries.class);
    }

    @Test
    public void pass_null_tolerance_check() {
        new NullPointerTester()
                .testAllPublicStaticMethods(Queries.class);
    }

    @Test
    public void return_proper_type_for_known_target() {
        final Target target = Targets.allOf(TestEntity.class);
        final Query query = Query.newBuilder()
                                 .setTarget(target)
                                 .build();
        final TypeUrl type = Queries.typeOf(query);
        assertNotNull(type);
        assertEquals(TARGET_ENTITY_TYPE_URL, type.toString());
    }

    @Test(expected = IllegalStateException.class)
    public void throw_ISE_for_unknown_type() {
        final Target target = Target.newBuilder()
                                    .setType("Inexistent Message Type")
                                    .build();
        final Query query = Query.newBuilder()
                                 .setTarget(target)
                                 .build();
        Queries.typeOf(query);
    }
}
