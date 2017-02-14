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

package org.spine3.net;

import org.junit.Test;
import org.spine3.test.NullToleranceTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.spine3.test.Tests.hasPrivateParameterlessCtor;

/**
 * @author Mikhail Mikhaylov
 */
@SuppressWarnings({"InstanceMethodNamingConvention"})
public class SchemasShould {

    @Test
    public void return_valid_schemas_on_valid_args() {
        assertEquals(Url.Record.Schema.DNS, Schemas.parse("dns"));
        assertEquals(Url.Record.Schema.DNS, Schemas.parse("DNS"));
    }

    @Test
    public void return_undefined_schema_on_invalid_args() {
        assertEquals(Url.Record.Schema.UNDEFINED, Schemas.parse("someunknownschema"));
    }

    @Test
    public void have_private_constructor() {
        assertTrue(hasPrivateParameterlessCtor(Schemas.class));
    }

    @Test
    public void pass_the_null_tolerance_check() {
        final NullToleranceTest nullToleranceTest =
                NullToleranceTest.newBuilder()
                                 .setClass(Schemas.class)
                                 .addDefaultValue(Url.Record.Schema.UNDEFINED)
                                 .build();
        final boolean passed = nullToleranceTest.check();
        assertTrue(passed);
    }
}
