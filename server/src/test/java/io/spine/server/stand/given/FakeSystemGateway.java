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

package io.spine.server.stand.given;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.client.Query;
import io.spine.system.server.NoOpSystemGateway;
import io.spine.system.server.SystemGateway;

/**
 * @author Dmytro Dashenkov
 */
final class FakeSystemGateway implements SystemGateway {

    private final Iterable<Any> queryResult;

    FakeSystemGateway(Iterable<Any> result) {
        this.queryResult = result;
    }

    @Override
    public void postCommand(Message systemCommand) {
        NoOpSystemGateway.INSTANCE.postCommand(systemCommand);
    }

    @Override
    public void postEvent(Message systemEvent) {
        NoOpSystemGateway.INSTANCE.postCommand(systemEvent);
    }

    @Override
    public Iterable<Any> read(Query query) {
        return queryResult;
    }
}
