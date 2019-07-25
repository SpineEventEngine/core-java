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

package io.spine.testing.server.blackbox.verify.query.given;

import com.google.protobuf.Empty;
import io.spine.base.Error;
import io.spine.core.Event;
import io.spine.core.Status;

public final class GivenResponseStatus {

    /** Prevents instantiation of this test env class. */
    private GivenResponseStatus() {
    }

    public static Status ok() {
        Status status = Status
                .newBuilder()
                .setOk(Empty.getDefaultInstance())
                .vBuild();
        return status;
    }

    public static Status error() {
        Status status = Status
                .newBuilder()
                .setError(Error.getDefaultInstance())
                .vBuild();
        return status;
    }

    public static Status rejection() {
        Status status = Status
                .newBuilder()
                .setRejection(Event.getDefaultInstance())
                .vBuild();
        return status;
    }
}
