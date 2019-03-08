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

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.base.EventMessage;
import io.spine.base.Identifier;
import io.spine.client.EntityId;
import io.spine.core.Event;
import io.spine.core.EventId;
import io.spine.net.Url;
import io.spine.system.server.DispatchedMessageId;
import io.spine.system.server.EntityArchived;
import io.spine.system.server.EntityDeleted;
import io.spine.system.server.EntityHistoryId;
import io.spine.system.server.EntityStateChanged;
import io.spine.test.system.server.Photo;
import io.spine.test.system.server.PhotoId;
import io.spine.test.system.server.PhotoVBuilder;
import io.spine.testing.server.TestEventFactory;
import io.spine.type.TypeUrl;

import java.util.Map;

import static io.spine.base.Time.currentTime;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.testing.server.TestEventFactory.newInstance;

public class RepositoryTestEnv {

    public static final TestEventFactory events = newInstance(RepositoryTestEnv.class);

    /**
     * Prevents the utility class instantiation.
     */
    private RepositoryTestEnv() {
    }

    public static Map<EntityHistoryId, Photo> givenPhotos() {
        Photo spineLogo = newPhoto("spine.io/logo", "Spine Logo");
        Photo projectsLogo = newPhoto("projects.tm/logo", "Projects Logo");
        Photo jxBrowserLogo = newPhoto("teamdev.com/jxbrowser/logo", "JxBrowser Logo");
        Map<EntityHistoryId, Photo> map = ImmutableMap
                .<EntityHistoryId, Photo>builder()
                .put(historyIdOf(spineLogo), spineLogo)
                .put(historyIdOf(projectsLogo), projectsLogo)
                .put(historyIdOf(jxBrowserLogo), jxBrowserLogo)
                .build();
        return map;
    }

    private static Photo newPhoto(String url, String altText) {
        PhotoId id = Identifier.generate(PhotoId.class);
        Url fullSizeUrl = Url
                .newBuilder()
                .setSpec(url)
                .build();
        Url thumbnail = Url
                .newBuilder()
                .setSpec(url + "-thumbnail")
                .build();
        Photo photo = PhotoVBuilder
                .newBuilder()
                .setId(id)
                .setFullSizeUrl(fullSizeUrl)
                .setThumbnailUrl(thumbnail)
                .setAltText(altText)
                .build();
        return photo;
    }

    private static EntityHistoryId historyIdOf(Photo photo) {
        Any id = pack(photo.getId());
        EntityId entityId = EntityId
                .newBuilder()
                .setId(id)
                .build();
        TypeUrl typeUrl = TypeUrl.of(Photo.class);
        EntityHistoryId historyId = EntityHistoryId
                .newBuilder()
                .setEntityId(entityId)
                .setTypeUrl(typeUrl.value())
                .build();
        return historyId;
    }

    public static Event entityStateChanged(EntityHistoryId historyId, Message state) {
        EntityStateChanged stateChanged = EntityStateChanged
                .newBuilder()
                .setId(historyId)
                .setNewState(pack(state))
                .setWhen(currentTime())
                .addMessageId(cause())
                .build();
        return event(stateChanged);
    }

    public static DispatchedMessageId cause() {
        EventId causeOfChange = EventId
                .newBuilder()
                .setValue("For tests")
                .build();
        DispatchedMessageId messageId = DispatchedMessageId
                .newBuilder()
                .setEventId(causeOfChange)
                .build();
        return messageId;
    }

    public static Event archived(Photo aggregate) {
        EntityHistoryId historyId = historyIdOf(aggregate);
        EntityArchived archived = EntityArchived
                .newBuilder()
                .setId(historyId)
                .setWhen(currentTime())
                .addMessageId(cause())
                .build();
        return event(archived);
    }

    public static Event deleted(Photo aggregate) {
        EntityHistoryId historyId = historyIdOf(aggregate);
        EntityDeleted deleted = EntityDeleted
                .newBuilder()
                .setId(historyId)
                .setWhen(currentTime())
                .addMessageId(cause())
                .build();
        return event(deleted);
    }

    public static Event event(EventMessage eventMessage) {
        return events.createEvent(eventMessage);
    }
}
