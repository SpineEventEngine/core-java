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

import com.google.protobuf.Message;
import org.spine3.base.CommandContext;
import org.spine3.base.EventContext;
import org.spine3.sample.order.OrderId;
import org.spine3.sample.order.event.OrderCreated;
import org.spine3.sample.order.event.OrderLineAdded;
import org.spine3.sample.order.event.OrderPaid;
import org.spine3.sample.saga.command.TestSagaCommand;
import org.spine3.server.saga.SagaRepository;
import org.spine3.util.Identifiers;

import java.util.Map;

import static com.google.common.collect.Maps.newConcurrentMap;

/**
 * @author Alexander Litus
 */
@SuppressWarnings("TypeMayBeWeakened")
public class SagaRepositorySample extends SagaRepository<String, SagaSample, SagaState> {

    /**
     * The map from order IDs to saga IDs.
     */
    private final Map<OrderId, String> identifiers = newConcurrentMap();

    @Override
    protected String getSagaIdOnCommand(Message command, CommandContext context) {
        if (command instanceof TestSagaCommand) {
            final TestSagaCommand testCmd = (TestSagaCommand) command;
            final OrderId orderId = testCmd.getOrderId();
            return identifiers.get(orderId);
        } else {
            throw newIllegalArgumentException(command);
        }
    }

    @Override
    @SuppressWarnings({"IfStatementWithTooManyBranches", "ChainOfInstanceofChecks"})
    protected String getSagaIdOnEvent(Message event, EventContext context) {
        if (event instanceof OrderCreated) {
            return getSagaId((OrderCreated) event);
        } else if (event instanceof OrderLineAdded) {
            return getSagaId((OrderLineAdded) event);
        } else if (event instanceof OrderPaid) {
            return getSagaId((OrderPaid) event);
        } else {
            throw newIllegalArgumentException(event);
        }
    }

    private String getSagaId(OrderCreated event) {
        final String sagaId = Identifiers.newUuid();
        identifiers.put(event.getOrderId(), sagaId);
        return sagaId;
    }

    private String getSagaId(OrderLineAdded event) {
        final String sagaId = identifiers.get(event.getOrderId());
        return sagaId;
    }

    private String getSagaId(OrderPaid event) {
        final String sagaId = identifiers.get(event.getOrderId());
        return sagaId;
    }

    private static IllegalArgumentException newIllegalArgumentException(Message command) {
        throw new IllegalArgumentException("Unknown message: " + command.getClass().getName());
    }
}
