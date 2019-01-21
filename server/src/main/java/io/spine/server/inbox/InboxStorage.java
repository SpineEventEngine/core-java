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

package io.spine.server.inbox;

import io.spine.server.storage.AbstractStorage;

import java.util.List;
import java.util.Optional;

/**
 * Abstract base for the storage of {@link Inbox} messages.
 */
public abstract class InboxStorage
        extends AbstractStorage<InboxId, InboxContentRecord, InboxReadRequest> {

    protected InboxStorage(boolean multitenant) {
        super(multitenant);
    }

    @Override
    public Optional<InboxContentRecord> read(InboxReadRequest request) {
        return Optional.empty();
    }

    protected abstract void write(InboxId id, InboxMessage message);

    @Override
    public void write(InboxId id, InboxContentRecord content) {

        List<InboxMessage> messages = content.getMessageList();
        for (InboxMessage message : messages) {
            write(id, message);
        }
    }
}
