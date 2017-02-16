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

package org.spine3.server.storage;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.SPI;
import org.spine3.base.Command;
import org.spine3.base.Event;
import org.spine3.protobuf.TypeUrl;
import org.spine3.server.entity.AbstractVersionableEntity;

/**
 * A container for the storage fields used in multiple {@link Storage storages}.
 *
 * <p>Basic usage is:
 * <ul>
 *     <li>Storing custom {@link AbstractVersionableEntity} state fields.
 *     <li>Storing custom {@link Event} and {@link Command} messages fields.
 * </ul>
 *
 * @author Dmytro Dashenkov
 * @see StorageField
 */
@SPI
public enum EntityField implements StorageField {

    /**
     * A field representing a timestamp in seconds.
     *
     * @see Timestamp#getSeconds()
     */
    timestamp,

    /**
     * A field for storing the part of a timestamp representing the amount of nanoseconds
     * which is less then {@link org.spine3.protobuf.Timestamps#NANOS_PER_SECOND 10^9}.
     *
     * @see Timestamp#getNanos()
     */
    timestamp_nanos,

    /**
     * A field for storing the serialized {@link Message} bytes.
     *
     * <p>This is the way to store objects of custom types within a strongly-typed storage
     * (e.g. relational database) and make the read/write operations easy requiring no reflection.
     *
     * @see Message#toByteArray()
     * @see Any#getValue()
     */
    value,

    /**
     * A field representing a {@link TypeUrl} of a certain {@link Message} type.
     *
     * <p>This field is commonly used in pair with {@link EntityField#value}
     * to store the fully qualified type name with the type prefix.
     *
     * @see TypeUrl
     */
    type_url

}
