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

package io.spine.system.server.given.mirror;

import com.google.protobuf.Empty;
import io.spine.client.EntityId;
import io.spine.core.EventId;
import io.spine.core.Version;
import io.spine.system.server.DispatchedMessageId;
import io.spine.system.server.EntityArchived;
import io.spine.system.server.EntityDeleted;
import io.spine.system.server.EntityExtractedFromArchive;
import io.spine.system.server.EntityHistoryId;
import io.spine.system.server.EntityRestored;
import io.spine.system.server.EntityStateChanged;
import io.spine.system.server.MirrorId;
import io.spine.test.system.server.Video;
import io.spine.type.TypeUrl;

import static io.spine.base.Time.getCurrentTime;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.protobuf.TypeConverter.toAny;

public final class ProjectionTestEnv {

    public static final String RAW_ID = "42";

    public static final MirrorId ID = MirrorId
            .newBuilder()
            .setValue(toAny(RAW_ID))
            .build();

    public static final Version VERSION = Version
            .newBuilder()
            .setNumber(42)
            .setTimestamp(getCurrentTime())
            .build();

    private static final TypeUrl AGGREGATE_TYPE_URL = TypeUrl.of(Video.class);

    /**
     * Prevents the utility class instantiation.
     */
    private ProjectionTestEnv() {
    }

    public static EntityStateChanged entityStateChanged() {
        EntityStateChanged event = EntityStateChanged
                .newBuilder()
                .setId(historyId(RAW_ID))
                .setNewState(pack(Empty.getDefaultInstance()))
                .setWhen(getCurrentTime())
                .addMessageId(cause())
                .setNewVersion(VERSION)
                .build();
        return event;
    }

    public static EntityArchived entityArchived() {
        EntityArchived event = EntityArchived
                .newBuilder()
                .setId(historyId(RAW_ID))
                .setWhen(getCurrentTime())
                .addMessageId(cause())
                .setVersion(VERSION)
                .build();
        return event;
    }

    public static EntityDeleted entityDeleted() {
        EntityDeleted event = EntityDeleted
                .newBuilder()
                .setId(historyId(RAW_ID))
                .setWhen(getCurrentTime())
                .addMessageId(cause())
                .setVersion(VERSION)
                .build();
        return event;
    }

    public static EntityExtractedFromArchive entityExtracted() {
        EntityExtractedFromArchive event = EntityExtractedFromArchive
                .newBuilder()
                .setId(historyId(RAW_ID))
                .setWhen(getCurrentTime())
                .addMessageId(cause())
                .setVersion(VERSION)
                .build();
        return event;
    }

    public static EntityRestored entityRestored() {
        EntityRestored event = EntityRestored
                .newBuilder()
                .setId(historyId(RAW_ID))
                .setWhen(getCurrentTime())
                .addMessageId(cause())
                .setVersion(VERSION)
                .build();
        return event;
    }

    private static EntityHistoryId historyId(String entityId) {
        EntityId id = EntityId
                .newBuilder()
                .setId(toAny(entityId))
                .build();
        EntityHistoryId historyId = EntityHistoryId
                .newBuilder()
                .setEntityId(id)
                .setTypeUrl(AGGREGATE_TYPE_URL.value())
                .build();
        return historyId;
    }

    private static DispatchedMessageId cause() {
        EventId eventId = EventId
                .newBuilder()
                .setValue("Event for test")
                .build();
        DispatchedMessageId cause = DispatchedMessageId
                .newBuilder()
                .setEventId(eventId)
                .build();
        return cause;
    }
}
