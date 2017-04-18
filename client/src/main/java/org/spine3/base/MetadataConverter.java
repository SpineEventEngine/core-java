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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.Metadata;
import io.grpc.Metadata.Key;
import org.spine3.util.Exceptions;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.grpc.Metadata.BINARY_BYTE_MARSHALLER;

/**
 * Serves as converter from {@link Error} to {@link Metadata} and vice versa.
 *
 * @author Dmytro Grankin
 */
public class MetadataConverter {

    private static final String ERROR_KEY_NAME = "Spine-Error-bin";

    @VisibleForTesting
    static final Key<byte[]> KEY = Key.of(ERROR_KEY_NAME, BINARY_BYTE_MARSHALLER);

    // Prevent instantiation of this utility class.
    private MetadataConverter() {
    }

    /**
     * Returns the {@link Metadata}, containing the {@link Error} as a byte array.
     *
     * @param error the error to convert
     * @return the metadata containing error
     */
    public static Metadata toMetadata(Error error) {
        checkNotNull(error);
        final Metadata metadata = new Metadata();
        metadata.put(KEY, error.toByteArray());
        return metadata;
    }

    /**
     * Returns the {@link Error} extracted from the {@link Metadata}.
     *
     * @param metadata the metadata to convert
     * @return the error extracted from the metadata or {@code Optional.absent()}
     *         if there is no error.
     */
    public static Optional<Error> toError(Metadata metadata) {
        checkNotNull(metadata);
        final byte[] bytes = metadata.get(KEY);

        if (bytes == null) {
            return Optional.absent();
        }

        try {
            final Error error = Error.parseFrom(bytes);
            return Optional.of(error);
        } catch (InvalidProtocolBufferException e) {
            throw Exceptions.wrappedCause(e);
        }
    }
}
