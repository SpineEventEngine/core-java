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

package io.spine.server.event.storage;

import io.spine.annotation.SPI;
import io.spine.core.EventContext;
import io.spine.server.storage.StorageField;

/**
 * Enumeration of storage fields required for storing
 * {@link EventContext EventContext}.
 *
 * @author Dmytro Dashenkov
 * @see StorageField
 */
@SPI
public enum EventContextField implements StorageField {

    /**
     * A field representing the {@link EventContext#getTimestamp()
     * event posting time}.
     */
    context_timestamp,

    /**
     * A field representing a {@link io.spine.core.CommandContext CommandContext}.
     *
     * <p>Presumably the field would contain a serialized format, but other storage options
     * (e.g. Json) are possible.
     */
    context_of_command,

    /**
     * A field representing a {@link EventContext#getVersion() version} of the
     * entity, which produced the event.
     */
    context_version
}
