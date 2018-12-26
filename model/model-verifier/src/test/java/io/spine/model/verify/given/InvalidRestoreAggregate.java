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

package io.spine.model.verify.given;

import io.spine.server.aggregate.Aggregate;
import io.spine.server.command.Assign;
import io.spine.test.model.verify.command.RestorePhoto;
import io.spine.test.model.verify.event.PhotoRestored;
import io.spine.test.model.verify.given.EditState;
import io.spine.test.model.verify.given.EditStateVBuilder;

/**
 * This aggregate breaks {@linkplain Assign command handler methods} by declaring a command handler
 * a {@code private} command handler. This should produce a warning.
 */
public class InvalidRestoreAggregate extends Aggregate<String, EditState, EditStateVBuilder> {

    protected InvalidRestoreAggregate(String id) {
        super(id);
    }

    @Assign
    private PhotoRestored handle(RestorePhoto restorePhoto) {
        return PhotoRestored
                .newBuilder()
                .setTitle(restorePhoto.getTitle())
                .build();
    }
}
