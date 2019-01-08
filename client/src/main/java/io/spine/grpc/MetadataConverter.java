/*
 * Copyright 2019, TeamDev. All rights reserved.
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

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.Metadata;
import io.grpc.Metadata.Key;
import io.spine.annotation.Internal;
import io.spine.base.Error;
import io.spine.util.Exceptions;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.grpc.Metadata.BINARY_BYTE_MARSHALLER;

/**
 * Serves as a converter from {@link Error} to {@link Metadata} and vice versa.
 *
 * @author Dmytro Grankin
 */
@Internal
public class MetadataConverter {

    private static final String ERROR_KEY_NAME = "Spine-Error-bin";

    /**
     * The {@link Metadata.Key} to store and get an {@link Error} from a {@link Metadata}.
     */
    @VisibleForTesting
    static final Key<byte[]> KEY = Key.of(ERROR_KEY_NAME, BINARY_BYTE_MARSHALLER);

    private MetadataConverter() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Returns the {@link Metadata}, containing the {@link Error} as a byte array.
     *
     * @param error the error to convert
     * @return the metadata containing error
     */
    public static Metadata toMetadata(Error error) {
        checkNotNull(error);
        Metadata metadata = new Metadata();
        metadata.put(KEY, error.toByteArray());
        return metadata;
    }

    /**
     * Returns the {@link Error} extracted from the {@link Metadata}.
     *
     * @param metadata the metadata to convert
     * @return the error extracted from the metadata or {@code Optional.empty()}
     *         if there is no error.
     */
    public static Optional<Error> toError(Metadata metadata) {
        checkNotNull(metadata);
        byte[] bytes = metadata.get(KEY);

        if (bytes == null) {
            return Optional.empty();
        }

        try {
            Error error = Error.parseFrom(bytes);
            return Optional.of(error);
        } catch (InvalidProtocolBufferException e) {
            throw Exceptions.illegalStateWithCauseOf(e);
        }
    }
}
