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

import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import io.spine.client.EntityStateWithVersion;
import io.spine.client.QueryResponse;
import io.spine.core.Response;
import io.spine.core.Status;
import io.spine.core.Version;
import io.spine.protobuf.AnyPacker;
import io.spine.testing.server.blackbox.BbTask;
import io.spine.testing.server.blackbox.BbTaskId;

import static io.spine.base.Identifier.newUuid;
import static io.spine.base.Time.currentTime;

public final class GivenQueryResponse {

    public static final String TASK_TITLE = "Clean dishes";
    public static final String TASK_DESCRIPTION = "Do clean all the dishes";

    private GivenQueryResponse() {
    }

    public static QueryResponse responseWithSingleState() {
        Status status = Status
                .newBuilder()
                .setOk(Empty.getDefaultInstance())
                .vBuild();
        Response response = Response
                .newBuilder()
                .setStatus(status)
                .vBuild();
        BbTaskId id = BbTaskId
                .newBuilder()
                .setUuid(newUuid())
                .vBuild();
        BbTask state = BbTask
                .newBuilder()
                .setTaskId(id)
                .setTitle(TASK_TITLE)
                .setDescription(TASK_DESCRIPTION)
                .vBuild();
        Any packed = AnyPacker.pack(state);
        Version version = Version
                .newBuilder()
                .setNumber(15)
                .setTimestamp(currentTime())
                .vBuild();
        EntityStateWithVersion stateWithVersion = EntityStateWithVersion
                .newBuilder()
                .setState(packed)
                .setVersion(version)
                .vBuild();
        QueryResponse queryResponse = QueryResponse
                .newBuilder()
                .setResponse(response)
                .addMessage(stateWithVersion)
                .vBuild();
        return queryResponse;
    }
}
