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

package io.spine.experiment;

import io.spine.server.BoundedContext;
import io.spine.server.ServerEnvironment;
import io.spine.server.transport.TransportFactory;
import io.spine.server.transport.memory.InMemoryTransportFactory;
import io.spine.testing.client.TestActorRequestFactory;
import org.junit.jupiter.api.Test;

import static io.spine.grpc.StreamObservers.noOpObserver;

class Experiment {

    private static final TestActorRequestFactory requests =
            new TestActorRequestFactory(Experiment.class);

    @Test
    void a() {
        TransportFactory transport = InMemoryTransportFactory.newInstance();
        ServerEnvironment
                .instance()
                .configureTransport(transport);
        BoundedContext billing = BoundedContext
                .singleTenant("billing")
                .add(BillingAggregate.class)
                .build();
        BoundedContext photos = BoundedContext
                .singleTenant("photos")
                .add(PhotosProcMan.class)
                .build();
        photos.commandBus()
              .post(requests.createCommand(UploadPhotos.generate()), noOpObserver());

    }
}
