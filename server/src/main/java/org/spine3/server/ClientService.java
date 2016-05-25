/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.server;

import io.grpc.stub.StreamObserver;
import org.spine3.base.Command;
import org.spine3.base.Event;
import org.spine3.base.Response;
import org.spine3.client.grpc.Topic;

/**
 * The {@code ClientService} allows client applications to post commands and
 * receive updates from the application backend.
 *
 * @author Alexander Yevsyukov
 */
public class ClientService implements org.spine3.client.grpc.ClientServiceGrpc.ClientService {

    public ClientService() {
        //TODO:2016-05-25:alexander.yevsyukov: Create a new instance using a Builder instance.
        // The Builder should contain added BoundedContexts. They expose commands they can handle.
    }

    @Override
    public void post(Command request, StreamObserver<Response> responseObserver) {
        //TODO:2016-05-25:alexander.yevsyukov: Implement. Post the command to corresponding BoundedContext.
        // If there's no context for the passed command, return error.
    }

    @Override
    public void subscribe(Topic request, StreamObserver<Event> responseObserver) {
        //TODO:2016-05-25:alexander.yevsyukov: Subscribe the client to the topic in the corresponding BoundedContext.
    }

    @Override
    public void unsubscribe(Topic request, StreamObserver<Response> responseObserver) {
        //TODO:2016-05-25:alexander.yevsyukov: Unsubscribe the client from the topic in the corresponding BoundedContext.
    }
}
