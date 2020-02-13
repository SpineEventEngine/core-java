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

import com.google.protobuf.util.Timestamps;
import io.spine.annotation.Internal;

import java.io.Serializable;
import java.util.Comparator;

/**
 * A comparator comparing the {@link InboxMessage}s so that they appear in a chronological order
 * of their appearance in the corresponding {@code Inbox}.
 *
 * <p>If case the timestamps are equal, the {@linkplain InboxMessage#getVersion() versions}
 * are compared.
 *
 * <p>If the versions are the same too, the messages are compared according to their ID values
 * taken as UUID-strings.
 */
@Internal
public final class InboxMessageComparator implements Comparator<InboxMessage>, Serializable {

    private static final long serialVersionUID = 0L;
    public static final InboxMessageComparator chronologically = new InboxMessageComparator();

    private InboxMessageComparator() {
    }

    @Override
    public int compare(InboxMessage m1, InboxMessage m2) {
        int timeComparison = Timestamps.compare(m1.getWhenReceived(), m2.getWhenReceived());
        if (timeComparison != 0) {
            return timeComparison;
        }
        int versionComparison = Integer.compare(m1.getVersion(), m2.getVersion());
        if(versionComparison != 0) {
            return versionComparison;
        }
        return m1.getId().getUuid().compareTo(m2.getId().getUuid());
    }
}
