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

package org.spine3.server.integration;

import io.grpc.stub.StreamObserver;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.Response;
import org.spine3.base.Responses;
import org.spine3.server.integration.grpc.IntegrationEventSubscriberGrpc.IntegrationEventSubscriberImplBase;
import org.spine3.test.integration.event.ProjectCreated;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Litus
 */
@SuppressWarnings("InstanceMethodNamingConvention")
public class IntegrationEventBusShould {

    private IntegrationEventBus eventBus;
    private TestIntEventSubscriber subscriber;

    private final IntegrationEvent event = Given.IntegrationEvent.projectCreated();

    @Before
    public void setUp() {
        eventBus = IntegrationEventBus.getInstance();
        subscriber = new TestIntEventSubscriber();
        eventBus.subscribe(subscriber, ProjectCreated.class);
    }

    @Test
    public void return_instance() {
        assertNotNull(eventBus);
    }
    
    @Test
    public void post_event_and_notify_subscriber() {
        eventBus.post(event);

        assertEquals(event, subscriber.eventHandled);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_if_no_subscribers_for_event() {
        eventBus.post(Given.IntegrationEvent.taskAdded());
    }

    @Test
    public void post_event_and_use_passed_response_observer() {
        final TestResponseObserver responseObserver = new TestResponseObserver();
        eventBus.setResponseObserver(responseObserver);

        eventBus.post(event);

        assertTrue(responseObserver.isCompleted);
    }

    @Test
    public void have_default_response_observer() {
        final StreamObserver<Response> observer = eventBus.getResponseObserver();
        assertNotNull(observer);
        eventBus.post(event);
    }

    private static class TestIntEventSubscriber extends IntegrationEventSubscriberImplBase {

        private IntegrationEvent eventHandled;

        @SuppressWarnings("RefusedBequest") // we override `unimplemented` default behaviour.
        @Override
        public void notify(IntegrationEvent event, StreamObserver<Response> observer) {
            this.eventHandled = event;
            observer.onNext(Responses.ok());
            observer.onError(new RuntimeException(""));
            observer.onCompleted();
        }
    }

    private static class TestResponseObserver implements StreamObserver<Response> {

        private boolean isCompleted = false;

        @Override
        public void onNext(Response response) {
        }

        @Override
        public void onError(Throwable error) {
        }

        @Override
        public void onCompleted() {
            this.isCompleted = true;
        }
    }
}
