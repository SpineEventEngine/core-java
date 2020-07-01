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
import io.spine.query.RecordColumn;

/**
 * Columns stored along with each {@link InboxMessage}.
 */
@SuppressWarnings("DuplicateStringLiteralInspection")  // column names may repeat across records.
final class InboxColumn {

    static final RecordColumn<InboxMessage, InboxSignalId> signal_id =
            new RecordColumn<>("signal_id", InboxSignalId.class, InboxMessage::getSignalId);

    static final RecordColumn<InboxMessage, InboxId> inbox_id =
            new RecordColumn<>("inbox_id", InboxId.class, InboxMessage::getInboxId);

    static final RecordColumn<InboxMessage, ShardIndex> inbox_shard =
            new RecordColumn<>("inbox_shard", ShardIndex.class, InboxMessage::shardIndex);

    static final RecordColumn<InboxMessage, Boolean> is_event =
            new RecordColumn<>("is_event", Boolean.class, InboxMessage::hasEvent);

    static final RecordColumn<InboxMessage, Boolean> is_command =
            new RecordColumn<>("is_command", Boolean.class, InboxMessage::hasCommand);

    static final RecordColumn<InboxMessage, InboxLabel> label =
            new RecordColumn<>("label", InboxLabel.class, InboxMessage::getLabel);

    static final RecordColumn<InboxMessage, InboxMessageStatus> status =
            new RecordColumn<>("status", InboxMessageStatus.class, InboxMessage::getStatus);

    static final RecordColumn<InboxMessage, Timestamp> received_at =
            new RecordColumn<>("received_at", Timestamp.class, InboxMessage::getWhenReceived);

    static final RecordColumn<InboxMessage, Integer> version =
            new RecordColumn<>("version", Integer.class, InboxMessage::getVersion);

    private InboxColumn() {
    }

    static ImmutableList<RecordColumn<InboxMessage, ?>> definitions() {
        return ImmutableList.of(signal_id, inbox_id, inbox_shard, is_event,
                                is_command, label, status, received_at, version);
    }
}
