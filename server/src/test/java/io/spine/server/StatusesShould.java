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

package io.spine.server;

import com.google.common.testing.NullPointerTester;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.spine.base.Error;
import io.spine.grpc.MetadataConverter;
import io.spine.server.event.UnsupportedEventException;
import io.spine.core.MessageRejection;
import io.spine.server.transport.Statuses;
import io.spine.test.event.ProjectCreated;
import io.spine.testdata.Sample;
import org.junit.Test;

import static io.spine.server.transport.Statuses.invalidArgumentWithCause;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static org.junit.Assert.assertEquals;

public class StatusesShould {

    @Test
    public void have_private_constructor() {
        assertHasPrivateParameterlessCtor(Statuses.class);
    }

    @Test
    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public void create_invalid_argument_status_exception() {
        final MessageRejection rejection =
                new UnsupportedEventException(Sample.messageOfType(ProjectCreated.class));
        final StatusRuntimeException statusRuntimeEx = invalidArgumentWithCause(rejection);
        final Error actualError = MetadataConverter.toError(statusRuntimeEx.getTrailers())
                                                   .get();
        assertEquals(Status.INVALID_ARGUMENT.getCode(), statusRuntimeEx.getStatus()
                                                                       .getCode());
        assertEquals(rejection, statusRuntimeEx.getCause());
        assertEquals(rejection.asError(), actualError);
    }

    @Test
    public void pass_the_null_tolerance_check() {
        new NullPointerTester()
                .setDefault(Exception.class, new RuntimeException("Statuses test"))
                .setDefault(Error.class, Error.getDefaultInstance())
                .testAllPublicStaticMethods(Statuses.class);
    }
}
