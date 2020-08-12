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

package io.spine.server.aggregate.given.mirror;

import com.google.common.collect.ImmutableList;
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
import io.spine.system.server.MRArchivePhoto;
import io.spine.system.server.MRDeletePhoto;
import io.spine.system.server.MRUploadPhoto;
import io.spine.test.system.server.MRPhoto;
import io.spine.test.system.server.MRPhotoArchived;
import io.spine.test.system.server.MRPhotoDeleted;
import io.spine.test.system.server.MRPhotoId;
import io.spine.test.system.server.MRPhotoUploaded;
import io.spine.testing.server.TestEventFactory;

import java.util.Collection;

import static io.spine.testing.server.TestEventFactory.newInstance;

public final class AggregateMirroringTestEnv {

    public static final TestEventFactory events = newInstance(AggregateMirroringTestEnv.class);

    /**
     * Prevents the utility class instantiation.
     */
    private AggregateMirroringTestEnv() {
    }

    public static Collection<MRPhoto> givenPhotos() {
        MRPhoto spineLogo = newPhoto("spine.io/logo", "Spine Logo");
        MRPhoto projectsLogo = newPhoto("projects.tm/logo", "Projects Logo");
        MRPhoto jxBrowserLogo = newPhoto("teamdev.com/jxbrowser/logo", "JxBrowser Logo");
        return ImmutableList.of(spineLogo, projectsLogo, jxBrowserLogo);
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

    public static Event event(EventMessage eventMessage) {
        return events.createEvent(eventMessage);
    }

    public static AggregateRepository<MRPhotoId, PhotoAggregate, MRPhoto> newPhotosRepository() {
        Repository<MRPhotoId, PhotoAggregate> repo = DefaultRepository.of(PhotoAggregate.class);
        return (AggregateRepository<MRPhotoId, PhotoAggregate, MRPhoto>) repo;
    }

    public static MRUploadPhoto upload(MRPhoto state) {
        return MRUploadPhoto
                .newBuilder()
                .setId(state.getId())
                .setFullSizeUrl(state.getFullSizeUrl())
                .setThumbnailUrl(state.getThumbnailUrl())
                .setAltText(state.getAltText())
                .vBuild();
    }

    public static MRArchivePhoto archive(MRPhoto photo) {
        return MRArchivePhoto.newBuilder()
                             .setId(photo.getId())
                             .vBuild();
    }

    public static MRDeletePhoto delete(MRPhoto photo) {
        return MRDeletePhoto.newBuilder()
                            .setId(photo.getId())
                            .vBuild();
    }

    public static class PhotoAggregate extends Aggregate<MRPhotoId, MRPhoto, MRPhoto.Builder> {

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

        @Assign
        MRPhotoArchived handle(MRArchivePhoto photo) {
            return MRPhotoArchived.newBuilder()
                                  .setId(photo.getId())
                                  .vBuild();
        }

        @Apply
        private void on(MRPhotoArchived event) {
            setArchived(true);
        }

        @Assign
        MRPhotoDeleted handle(MRDeletePhoto photo) {
            return MRPhotoDeleted.newBuilder()
                                 .setId(photo.getId())
                                 .vBuild();
        }

        @Apply
        private void on(MRPhotoDeleted event) {
            setDeleted(true);
        }
    }
}
