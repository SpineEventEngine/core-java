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

package org.spine3.server;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Converter;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.Metadata;
import io.grpc.Metadata.Key;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.spine3.annotations.Internal;
import org.spine3.base.Error;
import org.spine3.util.Exceptions;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.grpc.Metadata.BINARY_BYTE_MARSHALLER;
import static io.grpc.Status.INVALID_ARGUMENT;
import static org.spine3.util.Exceptions.newIllegalStateException;

/**
 * Utility class for working with {@link Status}es.
 *
 * @author Alexander Yevsyukov
 */
@Internal
public class Statuses {

    private static final MetadataConverter METADATA_CONVERTER = new MetadataConverter();

    private Statuses() {
    }

    /**
     * Creates an instance of {@code StatusRuntimeException} of status
     * {@code Status.INVALID_ARGUMENT}.
     *
     * <p>Resulting {@code StatusRuntimeException} will contain the passed {@link Error}
     * transformed to the {@linkplain StatusRuntimeException#getTrailers() metadata}.
     *
     * @param error the error representing metadata
     * @return the constructed {@code StatusRuntimeException}
     */
    public static StatusRuntimeException invalidArgumentWithMetadata(Error error) {
        checkNotNull(error);
        final Metadata metadata = METADATA_CONVERTER.doForward(error);
        final StatusRuntimeException result = INVALID_ARGUMENT.asRuntimeException(metadata);
        return result;
    }

    /**
     * Serves as converter from {@link Error} to {@link Metadata} and vice versa.
     */
    @VisibleForTesting
    static class MetadataConverter extends Converter<Error, Metadata> {

        private static final String ERROR_KEY_NAME = "Spine-Error-bin";
        private static final Key<byte[]> KEY = Key.of(ERROR_KEY_NAME, BINARY_BYTE_MARSHALLER);

        /**
         * Returns the {@link Metadata}, containing the {@link Error} as a byte array.
         *
         * @param error the error to convert
         * @return the metadata containing error
         */
        @Override
        protected Metadata doForward(Error error) {
            final Metadata metadata = new Metadata();
            metadata.put(KEY, error.toByteArray());
            return metadata;
        }

        /**
         * Returns the {@link Error} extracted from the {@link Metadata}.
         *
         * @param metadata the metadata to convert
         * @return the error extracted from the metadata
         */
        @Override
        protected Error doBackward(Metadata metadata) {
            final byte[] bytes = metadata.get(KEY);

            if (bytes == null) {
                throw newIllegalStateException("Specified metadata do not contain the $s", KEY);
            }

            try {
                return Error.parseFrom(bytes);
            } catch (InvalidProtocolBufferException e) {
                throw Exceptions.wrappedCause(e);
            }
        }
    }
}
