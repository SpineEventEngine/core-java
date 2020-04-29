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

package io.spine.server.integration.given;

import io.spine.core.External;
import io.spine.server.command.Assign;
import io.spine.server.event.React;
import io.spine.server.integration.CreditsHeld;
import io.spine.server.integration.PhotosPm;
import io.spine.server.integration.PhotosProcessed;
import io.spine.server.integration.PhotosUploaded;
import io.spine.server.integration.UploadPhotos;
import io.spine.server.procman.ProcessManager;

public class PhotosProcMan extends ProcessManager<String, PhotosPm, PhotosPm.Builder> {

    @Assign
    PhotosUploaded handle(UploadPhotos command) {
        return PhotosUploaded
                .newBuilder()
                .setUuid(command.getUuid())
                .vBuild();
    }

    @React
    PhotosProcessed on(@External CreditsHeld event) {
        return PhotosProcessed
                .newBuilder()
                .setUuid(event.getUuid())
                .vBuild();
    }
}
