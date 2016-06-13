/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

import javax.annotation.Nullable;

import static org.spine3.protobuf.Values.pack;


/**
 * Factories for constructing {@link ValueMismatch} instances for different types of attributes.
 */
public class Mismatch {

    private Mismatch() {}

    /**
     * Creates a {@link ValueMismatch} instance for a string attribute.
     *
     * @param expected the value expected by a command, or {@code null} if the command expects not populated field
     * @param found the value found in an entity, or {@code null} if the value is not set
     * @param requested the value requested as new one in the original command
     * @param version the current version of the entity
     * @return info on the mismatch
     */
    public static ValueMismatch of(@Nullable String expected, @Nullable String found, String requested, int version) {
        final ValueMismatch.Builder builder = ValueMismatch.newBuilder();
        if (expected != null) {
            builder.setExpected(pack(expected));
        }

        if (found != null) {
            builder.setActual(pack(found));
        }

        builder.setRequested(pack(requested));
        builder.setVersion(version);

        return builder.build();
    }

    //TODO:2016-06-13:alexander.yevsyukov: Add versions for other values including Message.
    //TODO:2016-06-13:alexander.yevsyukov: Add tests.
}
