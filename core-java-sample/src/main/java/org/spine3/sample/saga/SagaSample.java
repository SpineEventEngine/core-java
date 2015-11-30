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

package org.spine3.sample.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.CommandContext;
import org.spine3.base.EventContext;
import org.spine3.eventbus.Subscribe;
import org.spine3.sample.order.OrderId;
import org.spine3.sample.order.event.OrderCreated;
import org.spine3.sample.order.event.OrderLineAdded;
import org.spine3.sample.order.event.OrderPaid;
import org.spine3.sample.saga.command.TestSagaCommand;
import org.spine3.server.Assign;
import org.spine3.server.saga.Saga;

import static com.google.common.base.Preconditions.checkState;

/**
 * @author Alexander Litus
 */
@SuppressWarnings({"InstanceMethodNamingConvention", "TypeMayBeWeakened"})
public class SagaSample extends Saga<String, SagaState> {

    private static final String NEW_LINE = System.lineSeparator();

    public SagaSample(String id) {
        super(id);
    }

    @Override
    protected SagaState getDefaultState() {
        return SagaState.getDefaultInstance();
    }

    @Subscribe
    public void on(OrderCreated event, EventContext context) {
        logMessage(event.getClass().getSimpleName(), event.getOrderId());
        checkState(getState().getState() == SagaState.State.NEW, getState());
    }

    @Subscribe
    public void on(OrderLineAdded event, EventContext context) {
        logMessage(event.getClass().getSimpleName(), event.getOrderId());
        checkState(getState().getState() == SagaState.State.NEW, getState());
        final SagaState newState = SagaState.newBuilder().setState(SagaState.State.IN_PROGRESS).build();
        incrementState(newState);
    }

    @Subscribe
    public void on(OrderPaid event, EventContext context) {
        logMessage(event.getClass().getSimpleName(), event.getOrderId());
        checkState(getState().getState() == SagaState.State.IN_PROGRESS, getState());
        final SagaState newState = SagaState.newBuilder().setState(SagaState.State.DONE).build();
        incrementState(newState);
    }

    @Assign
    public void handle(TestSagaCommand command, CommandContext ctx) {
        logMessage(command.getClass().getSimpleName(), command.getOrderId());
        checkState(getState().getState() == SagaState.State.DONE, getState());
    }

    private static void logMessage(String messageName, OrderId orderId) {
        log().info("SagaSample handled " + messageName + ", order ID: " + orderId.getValue() + NEW_LINE);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }
    
    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(SagaSample.class);
    }
}
