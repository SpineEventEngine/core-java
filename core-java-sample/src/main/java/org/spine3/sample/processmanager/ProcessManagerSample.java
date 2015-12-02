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

package org.spine3.sample.processmanager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.CommandContext;
import org.spine3.base.EventContext;
import org.spine3.eventbus.Subscribe;
import org.spine3.sample.order.OrderId;
import org.spine3.sample.order.event.OrderCreated;
import org.spine3.sample.order.event.OrderLineAdded;
import org.spine3.sample.order.event.OrderPaid;
import org.spine3.sample.processmanager.ProcessManagerState.State;
import org.spine3.sample.processmanager.command.ProcessManagerSampleCommand;
import org.spine3.server.Assign;
import org.spine3.server.process.ProcessManager;

import static com.google.common.base.Preconditions.checkState;
import static org.spine3.sample.processmanager.ProcessManagerState.State.DONE;
import static org.spine3.sample.processmanager.ProcessManagerState.State.IN_PROGRESS;

/**
 * @author Alexander Litus
 */
@SuppressWarnings({"InstanceMethodNamingConvention", "TypeMayBeWeakened"})
public class ProcessManagerSample extends ProcessManager<String, ProcessManagerState> {

    private static final String NEW_LINE = System.lineSeparator();

    public ProcessManagerSample(String id) {
        super(id);
    }

    @Override
    protected ProcessManagerState getDefaultState() {
        return ProcessManagerState.getDefaultInstance();
    }

    @Subscribe
    public void on(OrderCreated event, EventContext context) {
        logMessage(event.getClass().getSimpleName(), event.getOrderId());

        final State state = getState().getState();
        checkState(state == State.NEW, state);
    }

    @Subscribe
    public void on(OrderLineAdded event, EventContext context) {
        logMessage(event.getClass().getSimpleName(), event.getOrderId());

        final State state = getState().getState();
        checkState(state == State.NEW, state);

        final ProcessManagerState newState = ProcessManagerState.newBuilder().setState(IN_PROGRESS).build();
        incrementState(newState);
    }

    @Subscribe
    public void on(OrderPaid event, EventContext context) {
        logMessage(event.getClass().getSimpleName(), event.getOrderId());

        final State state = getState().getState();
        checkState(state == IN_PROGRESS, state);

        final ProcessManagerState newState = ProcessManagerState.newBuilder().setState(DONE).build();
        incrementState(newState);
    }

    @Assign
    public void handle(ProcessManagerSampleCommand command, CommandContext ctx) {
        logMessage(command.getClass().getSimpleName(), command.getOrderId());

        final State state = getState().getState();
        checkState(state == DONE, state);
    }

    private static void logMessage(String messageName, OrderId orderId) {
        log().info("Process manager handled '" + messageName + "', order ID: " + orderId.getValue() + NEW_LINE);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }
    
    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(ProcessManagerSample.class);
    }
}
