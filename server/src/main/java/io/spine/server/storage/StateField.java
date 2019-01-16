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

package io.spine.server.storage;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.type.TypeUrl;

/**
 * Storage fields for persisting entity {@linkplain io.spine.server.entity.Entity#getState state}.
 *
 * @see StorageField
 */
public enum StateField implements StorageField {

    /**
     * The field to store {@link TypeUrl} of an entity state.
     *
     * <p>This type information is used to deserialize binary data stored in the
     * {@link StateField#bytes} field.
     *
     * @see TypeUrl
     */
    type_url,

    /**
     * The field to store the serialized bytes of the entity state.
     *
     * <p>This is the way to store objects of custom types within a strongly-typed storage
     * (e.g. relational database) and make the read/write operations easy requiring no reflection.
     *
     * @see Message#toByteArray()
     * @see Any#getValue()
     */
    bytes
}
