/*
 * Copyright 2020, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import io.spine.core.EventId;
import io.spine.core.MessageId;
import io.spine.core.Version;
import io.spine.system.server.MirrorId;
import io.spine.system.server.event.EntityArchived;
import io.spine.system.server.event.EntityDeleted;
import io.spine.system.server.event.EntityRestored;
import io.spine.system.server.event.EntityStateChanged;
import io.spine.system.server.event.EntityUnarchived;
import io.spine.test.system.server.MRVideo;
import io.spine.type.TypeUrl;

import static io.spine.base.Time.currentTime;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.protobuf.TypeConverter.toAny;

public final class MirrorProjectionTestEnv {

    public static final String RAW_ID = "42";

    public static final TypeUrl AGGREGATE_TYPE_URL = TypeUrl.of(MRVideo.class);

    public static final MirrorId ID = MirrorId
            .newBuilder()
            .setValue(toAny(RAW_ID))
            .setTypeUrl(AGGREGATE_TYPE_URL.value())
            .build();

    public static final Version VERSION = Version
            .newBuilder()
            .setNumber(42)
            .setTimestamp(currentTime())
            .build();

    /**
     * Prevents the utility class instantiation.
     */
    private MirrorProjectionTestEnv() {
    }

    public static EntityStateChanged entityStateChanged() {
        Any state = pack(Empty.getDefaultInstance());
        EntityStateChanged event = EntityStateChanged
                .newBuilder()
                .setEntity(messageId(RAW_ID))
                .setOldState(state)
                .setNewState(state)
                .setWhen(currentTime())
                .addSignalId(cause())
                .setNewVersion(VERSION)
                .build();
        return event;
    }

    public static EntityArchived entityArchived() {
        EntityArchived event = EntityArchived
                .newBuilder()
                .setEntity(messageId(RAW_ID))
                .setWhen(currentTime())
                .addSignalId(cause())
                .setVersion(VERSION)
                .build();
        return event;
    }

    public static EntityDeleted entityDeleted() {
        EntityDeleted event = EntityDeleted
                .newBuilder()
                .setEntity(messageId(RAW_ID))
                .setWhen(currentTime())
                .addSignalId(cause())
                .setVersion(VERSION)
                .setMarkedAsDeleted(true)
                .build();
        return event;
    }

    public static EntityUnarchived entityExtracted() {
        EntityUnarchived event = EntityUnarchived
                .newBuilder()
                .setEntity(messageId(RAW_ID))
                .setWhen(currentTime())
                .addSignalId(cause())
                .setVersion(VERSION)
                .build();
        return event;
    }

    public static EntityRestored entityRestored() {
        EntityRestored event = EntityRestored
                .newBuilder()
                .setEntity(messageId(RAW_ID))
                .setWhen(currentTime())
                .addSignalId(cause())
                .setVersion(VERSION)
                .build();
        return event;
    }

    private static MessageId messageId(String entityId) {
        MessageId historyId = MessageId
                .newBuilder()
                .setId(toAny(entityId))
                .setTypeUrl(AGGREGATE_TYPE_URL.value())
                .vBuild();
        return historyId;
    }

    private static MessageId cause() {
        EventId eventId = EventId
                .newBuilder()
                .setValue("Event for test")
                .build();
        MessageId cause = MessageId
                .newBuilder()
                .setId(pack(eventId))
                .setTypeUrl("example.com/example.Event")
                .build();
        return cause;
    }
}
