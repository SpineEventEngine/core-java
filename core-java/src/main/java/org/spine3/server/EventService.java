/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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
import org.spine3.Internal;
import org.spine3.base.EventRecord;
import org.spine3.client.ClientRequest;
import org.spine3.client.Connection;
import org.spine3.client.EventServiceGrpc;

/**
 * @author Alexander Yevsyukov
 */
public class EventService {

    private final GrpcImpl impl;

    public EventService() {
        this.impl = new GrpcImpl();
    }

    @Internal
    public EventServiceGrpc.EventService getGrpcImpl() {
        return this.impl;
    }


    public static class GrpcImpl implements EventServiceGrpc.EventService {
        @Override
        public void connect(ClientRequest request, StreamObserver<Connection> responseObserver) {
            //TODO:2015-12-16:alexander.yevsyukov: Implement
        }

        @Override
        public void open(Connection request, StreamObserver<EventRecord> responseObserver) {
            //TODO:2015-12-16:alexander.yevsyukov: Implement
        }
    }

}
