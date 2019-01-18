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

package io.spine.server.event.given.bus;

import io.grpc.stub.StreamObserver;
import io.spine.base.Error;
import io.spine.core.Ack;
import io.spine.core.Status;
import io.spine.json.Json;

import static io.spine.core.EventValidationError.UNSUPPORTED_EVENT_VALUE;
import static io.spine.core.Status.StatusCase.ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class UnsupportedEventAckObserver implements StreamObserver<Ack> {

    private boolean intercepted;
    private boolean completed;

    public UnsupportedEventAckObserver() {
        this.intercepted = false;
        this.completed = false;
    }

    @Override
    public void onNext(Ack value) {
        Status status = value.getStatus();
        if (status.getStatusCase() == ERROR) {
            Error error = status.getError();
            int code = error.getCode();
            assertEquals(UNSUPPORTED_EVENT_VALUE, code);
        } else {
            fail(Json.toJson(value));
        }
        intercepted = true;
    }

    @Override
    public void onError(Throwable t) {
        fail(t);
    }

    @Override
    public void onCompleted() {
        completed = true;
    }

    public boolean isCompleted() {
        return completed;
    }

    public boolean observedUnsupportedEvent() {
        return intercepted;
    }
}
