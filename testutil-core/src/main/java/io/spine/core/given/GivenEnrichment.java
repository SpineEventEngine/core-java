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

package io.spine.core.given;

import com.google.protobuf.Any;
import io.spine.core.Enrichment;

import static com.google.protobuf.Any.pack;
import static io.spine.Identifier.newUuid;
import static io.spine.protobuf.TypeConverter.toMessage;

/**
 * Factory methods to create {@code Enrichment} instances for test purposes.
 *
 * @author Dmytro Grankin
 */
public class GivenEnrichment {

    private GivenEnrichment() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Creates a new enabled {@link Enrichment}.
     *
     * <p>A result will contain an enrichment {@linkplain Enrichment#getContainer() instruction}.
     *
     * <p>An enrichment instruction is invalid and has random values.
     *
     * @return an enabled enrichment
     * @see Enrichment.ModeCase#getDoNotEnrich()
     */
    public static Enrichment enabledEnrichment() {
        final String key = newUuid();
        final Any value = pack(toMessage(newUuid()));
        return Enrichment.newBuilder()
                         .setContainer(Enrichment.Container.newBuilder()
                                                           .putItems(key, value)
                                                           .build())
                         .build();
    }
}
