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

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Timestamp;
import io.spine.server.entity.storage.ColumnName;
import io.spine.server.storage.CustomColumn;
import io.spine.server.storage.CustomColumn.Getter;
import io.spine.server.storage.QueryableField;

/**
 * Columns stored along with each {@link InboxMessage}.
 */
public enum InboxColumn implements QueryableField<InboxMessage> {

    signal_id(InboxSignalId.class, InboxMessage::getSignalId),

    inbox_id(InboxId.class, InboxMessage::getInboxId),

    inbox_shard(ShardIndex.class, InboxMessage::shardIndex),

    is_event(Boolean.class, InboxMessage::hasEvent),

    is_command(Boolean.class, InboxMessage::hasCommand),

    label(InboxLabel.class, InboxMessage::getLabel),

    status(InboxMessageStatus.class, InboxMessage::getStatus),

    received_at(Timestamp.class, InboxMessage::getWhenReceived),

    version(Integer.class, InboxMessage::getVersion);

    @SuppressWarnings("NonSerializableFieldInSerializableClass")
    private final CustomColumn<?, InboxMessage> column;

    <T> InboxColumn(Class<T> type, Getter<InboxMessage, T> getter) {
        ColumnName name = ColumnName.of(name());
        this.column = new CustomColumn<>(name, type, getter);
    }

    static ImmutableList<CustomColumn<?, InboxMessage>> definitions() {
        ImmutableList.Builder<CustomColumn<?, InboxMessage>> list = ImmutableList.builder();
        for (InboxColumn value : InboxColumn.values()) {
            list.add(value.column);
        }
        return list.build();
    }

    @Override
    public CustomColumn<?, InboxMessage> column() {
        return column;
    }
}
