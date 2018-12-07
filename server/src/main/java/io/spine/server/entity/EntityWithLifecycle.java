/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.server.entity;

import com.google.protobuf.Message;
import io.spine.server.entity.storage.Column;

/**
 * An entity which has {@linkplain LifecycleFlags lifecycle flags}.
 *
 * <p>Lifecycle flags determine if an entity is active.
 * An entity is considered to be active if the lifecycle flags are missing.
 * If an entity is {@linkplain #isArchived() archived} or {@linkplain #isDeleted() deleted}, 
 * then it’s regarded to be inactive.
 */
public interface EntityWithLifecycle<I, S extends Message> extends Entity<I, S> {

    /**
     * Obtains the current lifecycle flags.
     */
    LifecycleFlags getLifecycleFlags();

    /**
     * Shows if current instance is archived or not.
     */
    @Column
    boolean isArchived();

    /**
     * Shows if current instance is deleted or not.
     */
    @Column
    boolean isDeleted();

    /**
     * Tells whether lifecycle flags of the entity changed since its initialization.
     */
    boolean lifecycleFlagsChanged();
}
