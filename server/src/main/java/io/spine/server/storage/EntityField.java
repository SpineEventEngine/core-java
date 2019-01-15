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
import com.google.protobuf.Timestamp;
import io.spine.core.Event;
import io.spine.type.TypeUrl;

/**
 * A container for the storage fields used in multiple {@link Storage storages}.
 *
 * <p>Basic usage is:
 * <ul>
 *     <li>Storing custom {@link io.spine.server.entity.Entity Entity} state fields.
 *     <li>Storing custom {@link Event Event} and
 *                 {@link io.spine.core.Command Command} messages fields.
 * </ul>
 *
 * @author Dmytro Dashenkov
 * @see StorageField
 */
public enum EntityField implements StorageField {

    /**
     * A field representing a timestamp in seconds.
     *
     * @see Timestamp#getSeconds()
     */
    timestamp,

    /**
     * A field for storing the part of a timestamp representing the amount of nanoseconds.
     *
     * @see Timestamp#getNanos()
     */
    timestamp_nanos,

    /**
     * A field for storing the serialized bytes of the entity state.
     *
     * <p>This is the way to store objects of custom types within a strongly-typed storage
     * (e.g. relational database) and make the read/write operations easy requiring no reflection.
     *
     * @see Message#toByteArray()
     * @see Any#getValue()
     */
    bytes,

    /**
     * A field representing a {@link TypeUrl} of a certain {@link Message} type.
     *
     * <p>This field is commonly used in pair with {@link EntityField#bytes}
     * to store the fully qualified type name with the type prefix.
     *
     * @see TypeUrl
     */
    type_url,

    /**
     * A field representing the {@link io.spine.server.entity.Entity#getVersion Entity version}.
     *
     * <p>All the Spine basic {@linkplain io.spine.server.entity.Entity Entities} (e.g.
     * {@linkplain io.spine.server.aggregate.Aggregate Aggregates},
     * {@linkplain io.spine.server.projection.Projection Projections}, etc.) do have this field.
     *
     * @see io.spine.core.Version
     */
    version
}
