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

package io.spine.server.entity.rejection;

import com.google.errorprone.annotations.Immutable;
import com.google.protobuf.Any;
import io.spine.annotation.GeneratedMixin;
import io.spine.base.RejectionMessage;
import io.spine.protobuf.AnyPacker;

/**
 * Interface common for standard rejections which is used during routing.
 */
@Immutable
@GeneratedMixin
public interface StandardRejection extends RejectionMessage {

    /**
     * Obtains the packed version of ID of the entity which caused the rejection.
     */
    Any getEntityId();

    /**
     * Obtains the ID of the entity from the {@linkplain #getEntityId() packed form}.
     */
    default Object entityId() {
        Object result = AnyPacker.unpack(getEntityId());
        return result;
    }
}
