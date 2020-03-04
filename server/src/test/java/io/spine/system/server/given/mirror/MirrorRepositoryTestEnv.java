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

package io.spine.system.server.given.mirror;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Any;
import io.spine.base.EntityState;
import io.spine.base.EventMessage;
import io.spine.core.Event;
import io.spine.core.EventId;
import io.spine.core.MessageId;
import io.spine.core.Versions;
import io.spine.net.Url;
import io.spine.protobuf.AnyPacker;
import io.spine.server.DefaultRepository;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.entity.Repository;
import io.spine.system.server.MRUploadPhoto;
import io.spine.system.server.event.EntityArchived;
import io.spine.system.server.event.EntityDeleted;
import io.spine.system.server.event.EntityStateChanged;
import io.spine.test.system.server.MRPhoto;
import io.spine.test.system.server.MRPhotoId;
import io.spine.test.system.server.MRPhotoUploaded;
import io.spine.testing.server.TestEventFactory;
import io.spine.type.TypeUrl;

import java.util.Map;

import static io.spine.base.Time.currentTime;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.testing.server.TestEventFactory.newInstance;

public final class MirrorRepositoryTestEnv {

    public static final TestEventFactory events = newInstance(MirrorRepositoryTestEnv.class);

    /**
     * Prevents the utility class instantiation.
     */
    private MirrorRepositoryTestEnv() {
    }

    public static Map<MessageId, MRPhoto> givenPhotos() {
        MRPhoto spineLogo = newPhoto("spine.io/logo", "Spine Logo");
        MRPhoto projectsLogo = newPhoto("projects.tm/logo", "Projects Logo");
        MRPhoto jxBrowserLogo = newPhoto("teamdev.com/jxbrowser/logo", "JxBrowser Logo");
        Map<MessageId, MRPhoto> map = ImmutableMap
                .<MessageId, MRPhoto>builder()
                .put(historyIdOf(spineLogo), spineLogo)
                .put(historyIdOf(projectsLogo), projectsLogo)
                .put(historyIdOf(jxBrowserLogo), jxBrowserLogo)
                .build();
        return map;
    }

    private static MRPhoto newPhoto(String url, String altText) {
        Url fullSizeUrl = Url
                .newBuilder()
                .setSpec(url)
                .buildPartial();
        Url thumbnail = Url
                .newBuilder()
                .setSpec(url + "-thumbnail")
                .buildPartial();
        MRPhoto photo = MRPhoto
                .newBuilder()
                .setId(MRPhotoId.generate())
                .setFullSizeUrl(fullSizeUrl)
                .setThumbnailUrl(thumbnail)
                .setAltText(altText)
                .vBuild();
        return photo;
    }

    private static MessageId historyIdOf(MRPhoto photo) {
        Any id = pack(photo.getId());
        TypeUrl typeUrl = TypeUrl.of(MRPhoto.class);
        MessageId historyId = MessageId
                .newBuilder()
                .setId(id)
                .setTypeUrl(typeUrl.value())
                .vBuild();
        return historyId;
    }

    public static Event entityStateChanged(MessageId entityId, EntityState state) {
        EntityStateChanged stateChanged = EntityStateChanged
                .newBuilder()
                .setEntity(entityId)
                .setOldState(pack(state))
                .setNewState(pack(state))
                .setWhen(currentTime())
                .addSignalId(cause())
                .vBuild();
        return event(stateChanged);
    }

    public static MessageId cause() {
        EventId causeOfChange = EventId
                .newBuilder()
                .setValue("For tests")
                .build();
        MessageId messageId = MessageId
                .newBuilder()
                .setId(AnyPacker.pack(causeOfChange))
                .setVersion(Versions.zero())
                .setTypeUrl("example.org/test.Type")
                .vBuild();
        return messageId;
    }

    public static Event archived(MRPhoto aggregate) {
        MessageId entityId = historyIdOf(aggregate);
        EntityArchived archived = EntityArchived
                .newBuilder()
                .setEntity(entityId)
                .setWhen(currentTime())
                .addSignalId(cause())
                .vBuild();
        return event(archived);
    }

    public static Event deleted(MRPhoto aggregate) {
        MessageId historyId = historyIdOf(aggregate);
        EntityDeleted deleted = EntityDeleted
                .newBuilder()
                .setEntity(historyId)
                .setWhen(currentTime())
                .addSignalId(cause())
                .setMarkedAsDeleted(true)
                .vBuild();
        return event(deleted);
    }

    public static Event event(EventMessage eventMessage) {
        return events.createEvent(eventMessage);
    }

    public static AggregateRepository<MRPhotoId, PhotoAggregate> newPhotosRepository() {
        Repository<MRPhotoId, PhotoAggregate> repository = DefaultRepository.of(PhotoAggregate.class);
        return (AggregateRepository<MRPhotoId, PhotoAggregate>) repository;
    }

    private static class PhotoAggregate extends Aggregate<MRPhotoId, MRPhoto, MRPhoto.Builder> {

        @Assign
        MRPhotoUploaded handle(MRUploadPhoto cmd) {
            MRPhotoUploaded event = MRPhotoUploaded
                    .newBuilder()
                    .setId(cmd.getId())
                    .setFullSizeUrl(cmd.getFullSizeUrl())
                    .setThumbnailUrl(cmd.getThumbnailUrl())
                    .setAltText(cmd.getAltText())
                    .vBuild();
            return event;
        }

        @Apply
        private void on(MRPhotoUploaded event) {
            builder().setId(event.getId())
                     .setFullSizeUrl(event.getFullSizeUrl())
                     .setThumbnailUrl(event.getThumbnailUrl())
                     .setAltText(event.getAltText());
        }
    }
}
