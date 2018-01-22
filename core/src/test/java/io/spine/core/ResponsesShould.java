/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.core;

import io.spine.base.Error;
import org.junit.Test;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ResponsesShould {

    @Test
    public void have_private_constructor() {
        assertHasPrivateParameterlessCtor(Responses.class);
    }

    @Test
    public void return_OK_response() {
        checkNotNull(Responses.ok());
    }

    @Test
    public void recognize_OK_response() {
        assertTrue(Responses.isOk(Responses.ok()));
    }

    @Test
    public void return_false_if_not_OK_response() {
        final Status status = Status.newBuilder()
                                    .setError(Error.getDefaultInstance())
                                    .build();
        final Response error = Response.newBuilder()
                                       .setStatus(status)
                                       .build();
        assertFalse(Responses.isOk(error));
    }
}
