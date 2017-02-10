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

package org.spine3.server.event.storage;

import org.spine3.SPI;
import org.spine3.base.CommandContext;
import org.spine3.base.EventContext;
import org.spine3.server.event.EventStorage;
import org.spine3.server.storage.StorageField;

/**
 * A container for the storage fields specific for the {@link EventStorage} and its implementations and connected to
 * the {@link EventContext}.
 *
 * @author Dmytro Dashenkov
 * @see StorageField
 */
@SPI
public enum EventContextField implements StorageField {

    /**
     * A field representing the {@link EventContext#getEventId() event ID} stored in the {@link EventContext}.
     */
    context_event_id,

    /**
     * A field representing the {@link EventContext#getTimestamp() event posting time}.
     */
    context_timestamp,

    /**
     * A field representing the serialized {@link CommandContext}. This is not the only ay to store it, but it's
     * acceptable.
     */
    context_of_command,

    /**
     * A field representing the {@link EventContext#getVersion() producer entity version}.
     */
    context_version

}
