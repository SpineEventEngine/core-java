/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.grpc;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.Metadata;
import io.spine.base.Error;
import org.junit.Test;

import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/**
 * @author Dmytro Grankin
 */
public class MetadataConverterShould {

    @Test
    public void have_private_constructor() {
        assertHasPrivateParameterlessCtor(MetadataConverter.class);
    }

    @SuppressWarnings("ConstantConditions") // A part of the test.
    @Test
    public void return_metadata_containing_error() throws InvalidProtocolBufferException {
        final Error error = Error.getDefaultInstance();
        final Metadata metadata = MetadataConverter.toMetadata(error);
        final byte[] bytes = metadata.get(MetadataConverter.KEY);
        assertEquals(error, Error.parseFrom(bytes));
    }

    @Test
    public void return_error_extracted_form_metadata() {
        final Error expectedError = Error.getDefaultInstance();
        final Metadata metadata = MetadataConverter.toMetadata(expectedError);

        assertEquals(expectedError, MetadataConverter.toError(metadata)
                                                     .get());
    }

    @Test
    public void return_absent_if_metadata_is_empty() {
        final Metadata metadata = new Metadata();

        assertFalse(MetadataConverter.toError(metadata)
                                     .isPresent());
    }

    @Test
    public void throw_wrapped_InvalidProtocolBufferException_if_bytes_are_invalid() {
        final Metadata metadata = new Metadata();
        metadata.put(MetadataConverter.KEY, new byte[]{(byte) 1});

        try {
            MetadataConverter.toError(metadata);
            fail("InvalidProtocolBufferException was not thrown.");
        } catch (IllegalStateException e) {
            assertTrue(e.getCause() instanceof InvalidProtocolBufferException);
        }
    }

    @Test
    public void pass_the_null_tolerance_check() {
        new NullPointerTester()
                .testAllPublicStaticMethods(MetadataConverter.class);
    }
}
