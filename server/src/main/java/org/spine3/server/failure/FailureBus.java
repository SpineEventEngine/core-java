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
package org.spine3.server.failure;

import io.grpc.stub.StreamObserver;
import org.spine3.base.Failure;
import org.spine3.base.Response;
import org.spine3.server.bus.Bus;
import org.spine3.server.bus.DispatcherRegistry;

/**
 * Dispatches the business failures that occur during the command processing
 * to the corresponding subscriber.
 *
 * @author Alexander Yevsyuov
 * @author Alex Tymchenko
 * @see org.spine3.base.FailureThrowable
 * @see org.spine3.server.event.Subscribe Subscribe @Subscribe
 */
public class FailureBus extends Bus<Failure, FailureEnvelope, FailureClass, FailureDispatcher> {
    @Override
    public void post(Failure message, StreamObserver<Response> responseObserver) {

    }

    @Override
    protected DispatcherRegistry<FailureClass, FailureDispatcher> createRegistry() {
        return null;
    }

    @Override
    public void close() throws Exception {

    }
}
