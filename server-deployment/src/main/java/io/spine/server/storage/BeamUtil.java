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

package io.spine.server.storage;

import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.spine.Identifier;
import org.apache.beam.sdk.coders.BigEndianIntegerCoder;
import org.apache.beam.sdk.coders.BigEndianLongCoder;
import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.extensions.protobuf.ProtoCoder;
import org.joda.time.Instant;

import static com.google.protobuf.util.Timestamps.toMillis;
import static io.spine.util.Exceptions.illegalStateWithCauseOf;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * Utilities for working with Apache Beam.
 *
 * @author Alexander Yevsyukov
 */
public class BeamUtil {

    private BeamUtil() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Converts Protobuf {@link Timestamp} instance to Joda Time {@link Instant}.
     *
     * <p>Nanoseconds are lost during the conversion as {@code Instant} keeps time with the
     * millis precision.
     */
    public static Instant toInstant(Timestamp timestamp) {
        final long millis = toMillis(timestamp);
        return new Instant(millis);
    }

    /**
     * Creates a deterministic coder for the passed class of {@linkplain Identifier identifiers}.
     *
     * @param idClass the class of identifiers
     * @param <I>     the type of identifiers
     * @return new coder instance
     */
    @SuppressWarnings("unchecked") // the cast is preserved by ID type checking
    public static <I> Coder<I> crateIdCoder(Class<I> idClass) {
        final Identifier.Type idType = Identifier.getType(idClass);
        Coder<I> idCoder;
        switch (idType) {
            case INTEGER:
                idCoder = (Coder<I>) BigEndianIntegerCoder.of();
                break;
            case LONG:
                idCoder = (Coder<I>) BigEndianLongCoder.of();
                break;
            case STRING:
                idCoder = (Coder<I>) StringUtf8Coder.of();
                break;
            case MESSAGE:
                idCoder = (Coder<I>) ProtoCoder.of((Class<? extends Message>) idClass);
                break;
            default:
                throw newIllegalStateException("Unsupported ID type: %s", idType.name());
        }

        // Check that the key coder is deterministic.
        try {
            idCoder.verifyDeterministic();
        } catch (Coder.NonDeterministicException e) {
            throw illegalStateWithCauseOf(e);
        }
        return idCoder;
    }
}
