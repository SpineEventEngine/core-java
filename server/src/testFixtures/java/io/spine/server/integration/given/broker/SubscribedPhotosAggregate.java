/*
 * Copyright 2025, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.server.integration.given.broker;

import io.spine.core.External;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.event.React;
import io.spine.server.integration.broker.CreditsHeld;
import io.spine.server.integration.broker.PhotosAgg;
import io.spine.server.integration.broker.PhotosProcessed;
import io.spine.server.integration.broker.PhotosUploaded;
import io.spine.server.integration.broker.UploadPhotos;

final class SubscribedPhotosAggregate extends Aggregate<String, PhotosAgg, PhotosAgg.Builder> {

    @Assign
    PhotosUploaded handler(UploadPhotos command) {
        return PhotosUploaded.generate();
    }

    @Apply
    private void on(PhotosUploaded event) {
        builder().setId(event.getUuid());
    }

    @React
    PhotosProcessed on(@External CreditsHeld event) {
        return PhotosProcessed.generate();
    }

    @Apply
    private void on(PhotosProcessed event) {
        builder().setId(event.getUuid());
    }
}
