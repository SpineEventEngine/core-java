/*
 * Copyright 2018, TeamDev. All rights reserved.
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
import io.spine.client.EntityId;
import io.spine.net.Url;
import io.spine.system.server.EntityHistoryId;
import io.spine.test.system.server.Photo;
import io.spine.test.system.server.PhotoId;
import io.spine.test.system.server.PhotoVBuilder;
import io.spine.type.TypeUrl;

import java.util.Map;

import static io.spine.base.Identifier.newUuid;
import static io.spine.protobuf.AnyPacker.pack;

/**
 * @author Dmytro Dashenkov
 */
public class RepositoryTestEnv {

    /**
     * Prevents the utility class instantiation.
     */
    private RepositoryTestEnv() {
    }

    public static Photo newPhoto(String url, String altText) {
        PhotoId id = PhotoId
                .newBuilder()
                .setUuid(newUuid())
                .build();
        Url fullSizeUrl = Url
                .newBuilder()
                .setRaw(url)
                .build();
        Url thumbnail = Url
                .newBuilder()
                .setRaw(url + "-thumbnail")
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

    public static EntityHistoryId historyIdOf(Photo photo) {
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
}
