/*
 * Copyright 2018, TeamDev. All rights reserved.
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
package io.spine.server.delivery;

import io.spine.core.BoundedContextName;
import io.spine.server.BoundedContext;
import io.spine.server.delivery.given.ShardedStreamTestEnv.TaskAggregateRepository;
import io.spine.server.model.ModelTests;
import io.spine.server.transport.TransportFactory;
import io.spine.test.Tests;
import io.spine.test.aggregate.ProjectId;
import org.junit.Before;
import org.junit.Test;

import static io.spine.server.delivery.given.ShardedStreamTestEnv.builder;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * @author Alex Tymchenko
 */
@SuppressWarnings("unchecked")  // the numerous generic parameters are omitted to simplify tests.
public class ShardedStreamBuilderShould {

    @Before
    public void setUp() {
        // as long as we refer to the Model in delivery tag initialization.
        ModelTests.clearModel();
    }

    @Test(expected = NullPointerException.class)
    public void not_accept_null_boundedContextName() {
        builder().setBoundedContextName(Tests.<BoundedContextName>nullRef());
    }

    @Test
    public void return_set_boundedContextName() {
        final BoundedContextName value = BoundedContext.newName("ShardedStreams");
        assertEquals(value, builder().setBoundedContextName(value)
                                     .getBoundedContextName());
    }

    @Test(expected = NullPointerException.class)
    public void not_accept_null_key() {
        builder().setKey(Tests.<ShardingKey>nullRef());
    }

    @Test
    public void return_set_key() {
        final ShardingKey value = mock(ShardingKey.class);
        assertEquals(value, builder().setKey(value)
                                     .getKey());
    }

    @Test(expected = NullPointerException.class)
    public void not_accept_null_tag() {
        builder().setTag(Tests.<DeliveryTag>nullRef());
    }

    @Test
    public void return_set_tag() {
        final DeliveryTag value = DeliveryTag.forCommandsOf(new TaskAggregateRepository());
        assertEquals(value, builder().setTag(value)
                                     .getTag());
    }

    @Test(expected = NullPointerException.class)
    public void not_accept_null_targetIdClass() {
        builder().setTargetIdClass(Tests.<Class>nullRef());
    }

    @Test
    public void return_set_targetIdClass() {
        final Class value = ProjectId.class;
        assertEquals(value, builder().setTargetIdClass(value)
                                     .getTargetIdClass());
    }

    @Test(expected = NullPointerException.class)
    public void not_accept_null_consumer() {
        builder().setConsumer(Tests.<Consumer>nullRef());
    }

    @Test
    public void return_set_consumer() {
        final Consumer value = mock(Consumer.class);
        assertEquals(value, builder().setConsumer(value)
                                     .getConsumer());
    }

    @Test(expected = NullPointerException.class)
    public void not_accept_null_transportFactory() {
        builder().build(Tests.<TransportFactory>nullRef());
    }
}
