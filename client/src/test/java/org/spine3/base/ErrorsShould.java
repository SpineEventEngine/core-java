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

package org.spine3.base;

import com.google.common.base.Optional;
import com.google.common.testing.NullPointerTester;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;

public class ErrorsShould {

    @Test
    public void have_private_constructor() {
        assertHasPrivateParameterlessCtor(Errors.class);
    }

    @Test
    public void create_Error_by_Exception() {
        final String msg = "create_error_by_exception";
        final Exception exception = new NullPointerException(msg);
        final Error error = Errors.fromException(exception);

        assertEquals(msg, error.getMessage());
        assertEquals(exception.getClass()
                              .getName(), error.getType());
    }

    @Test
    public void create_Error_by_Throwable() {
        final String msg = "create_Error_by_Throwable";
        final Throwable throwable = new IllegalStateException(msg);

        final Error error = Errors.fromThrowable(throwable);
        assertEquals(msg, error.getMessage());
        assertEquals(throwable.getClass()
                              .getName(), error.getType());
    }

    @Test
    public void return_Error_extracted_from_StatusRuntimeException_metadata() {
        final Error expectedError = Error.getDefaultInstance();
        final Metadata metadata = MetadataConverter.toMetadata(expectedError);
        final StatusRuntimeException statusRuntimeException =
                Status.INVALID_ARGUMENT.asRuntimeException(metadata);

        assertEquals(expectedError, Errors.fromResponseError(statusRuntimeException)
                                          .get());
    }

    @Test
    public void return_Error_extracted_form_StatusException_metadata() {
        final Error expectedError = Error.getDefaultInstance();
        final Metadata metadata = MetadataConverter.toMetadata(expectedError);
        final StatusException statusException = Status.INVALID_ARGUMENT.asException(metadata);

        assertEquals(expectedError, Errors.fromResponseError(statusException)
                                          .get());
    }

    @Test
    public void return_absent_if_passed_Throwable_is_not_status_exception() {
        final String msg = "Neither a StatusException nor a StatusRuntimeException.";
        final Exception exception = new Exception(msg);

        assertEquals(Optional.absent(), Errors.fromResponseError(exception));
    }

    @Test
    public void return_absent_if_there_is_no_error_in_metadata() {
        final Metadata emptyMetadata = new Metadata();
        final Throwable statusRuntimeEx = Status.INVALID_ARGUMENT.asRuntimeException(emptyMetadata);

        assertEquals(Optional.absent(), Errors.fromResponseError(statusRuntimeEx));
    }

    @Test
    public void pass_the_null_tolerance_check() {
        new NullPointerTester()
                .setDefault(Exception.class, new RuntimeException("null check"))
                .testAllPublicStaticMethods(Errors.class);
    }
}
