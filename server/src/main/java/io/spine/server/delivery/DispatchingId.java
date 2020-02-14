/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.server.delivery;

import com.google.common.base.MoreObjects;

import java.util.Objects;

/**
 * An identifier of the signal-to-target dispatched as {@code InboxMessage}.
 */
final class DispatchingId {

    private final InboxSignalId signal;
    private final InboxId inbox;

    /**
     * Creates the dispatching identifier for the passed message.
     */
    DispatchingId(InboxMessage message) {
        this.signal = message.getSignalId();
        this.inbox = message.getInboxId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DispatchingId id = (DispatchingId) o;
        return Objects.equals(signal, id.signal) &&
                Objects.equals(inbox, id.inbox);
    }

    @Override
    public int hashCode() {
        return Objects.hash(signal, inbox);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("signal", signal)
                          .add("inbox", inbox)
                          .toString();
    }
}
