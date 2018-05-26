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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.client.CommonTestNames.NULL_TOLERANCE;
import static io.spine.client.CommonTestNames.UTILITY_CTOR;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static junit.framework.TestCase.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Dmytro Grankin
 */
@DisplayName("Metadata converter should")
class MetadataConverterTest {

    @Test
    @DisplayName(UTILITY_CTOR)
    void haveUtilityConstructor() {
        assertHasPrivateParameterlessCtor(MetadataConverter.class);
    }

    @Test
    @DisplayName(NULL_TOLERANCE)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .testAllPublicStaticMethods(MetadataConverter.class);
    }

    @SuppressWarnings("ConstantConditions") // A part of the test.
    @Test
    @DisplayName("convert Error to Metadata")
    void convertError() throws InvalidProtocolBufferException {
        final Error error = Error.getDefaultInstance();
        final Metadata metadata = MetadataConverter.toMetadata(error);
        final byte[] bytes = metadata.get(MetadataConverter.KEY);
        assertEquals(error, Error.parseFrom(bytes));
    }

    @Test
    @DisplayName("convert Metadata to Error")
    void convertMetadata() {
        final Error expectedError = Error.getDefaultInstance();
        final Metadata metadata = MetadataConverter.toMetadata(expectedError);

        assertEquals(expectedError, MetadataConverter.toError(metadata)
                                                     .get());
    }

    @Test
    @DisplayName("return absent when converting empty Metadata")
    void processEmptyMetadata() {
        final Metadata metadata = new Metadata();

        assertFalse(MetadataConverter.toError(metadata)
                                     .isPresent());
    }

    @Test
    @DisplayName("throw wrapped InvalidProtocolBufferException when Metadata bytes are invalid")
    void throwOnInvalidBytes() {
        final Metadata metadata = new Metadata();
        metadata.put(MetadataConverter.KEY, new byte[]{(byte) 1});

        try {
            MetadataConverter.toError(metadata);
            fail("InvalidProtocolBufferException was not thrown.");
        } catch (IllegalStateException e) {
            assertTrue(e.getCause() instanceof InvalidProtocolBufferException);
        }
    }
}
