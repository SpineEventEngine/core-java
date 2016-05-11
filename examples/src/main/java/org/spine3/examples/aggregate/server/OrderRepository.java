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
package org.spine3.examples.aggregate.server;

import com.google.common.base.Function;
import org.spine3.base.Identifiers;
import org.spine3.examples.aggregate.OrderId;
import org.spine3.server.BoundedContext;
import org.spine3.server.aggregate.AggregateRepository;

import javax.annotation.Nullable;

import static org.spine3.base.Identifiers.NULL_ID;

/**
 * @author Mikhail Melnik
 */
public class OrderRepository extends AggregateRepository<OrderId, OrderAggregate> {

    public OrderRepository(BoundedContext boundedContext) {
        super(boundedContext);

        // Register id converters
        Identifiers.ConverterRegistry.getInstance().register(OrderId.class, new OrderIdToStringConverter());
    }

    private static class OrderIdToStringConverter implements Function<OrderId, String> {

        @Override
        public String apply(@Nullable OrderId orderId) {
            if (orderId == null) {
                return NULL_ID;
            }
            final String value = orderId.getValue();
            return value;
        }
    }
}
