/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.command;

import com.google.common.collect.ImmutableList;
import io.grpc.stub.StreamObserver;
import org.spine3.base.Command;
import org.spine3.base.Response;
import org.spine3.base.Responses;

import java.util.List;

import static com.google.common.collect.Lists.newLinkedList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The observer for the result of {@link CommandBus#post(Command, StreamObserver) CommandBus.post()}.
 */
class TestResponseObserver implements StreamObserver<Response> {

    private final List<Response> responses = newLinkedList();
    private Throwable throwable;
    private boolean completed = false;

    void assertResponseOkAndCompleted() {
        final List<Response> responses = getResponses();
        assertEquals(1, responses.size());
        assertEquals(Responses.ok(), responses.get(0));
        assertTrue(isCompleted());
        assertNull(getThrowable());
    }

    @Override
    public void onNext(Response response) {
        responses.add(response);
    }

    @Override
    public void onError(Throwable throwable) {
        this.throwable = throwable;
    }

    @Override
    public void onCompleted() {
        this.completed = true;
    }

    List<Response> getResponses() {
        return ImmutableList.copyOf(responses);
    }

    Throwable getThrowable() {
        return throwable;
    }

    boolean isCompleted() {
        return this.completed;
    }

    boolean isError() {
        return throwable != null;
    }
}
