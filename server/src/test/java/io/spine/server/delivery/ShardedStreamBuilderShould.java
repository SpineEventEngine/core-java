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
import io.spine.test.Tests;
import io.spine.test.aggregate.ProjectId;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static io.spine.server.delivery.given.ShardedStreamTestEnv.builder;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * @author Alex Tymchenko
 */
@SuppressWarnings("unchecked")  // the numerous generic parameters are omitted to simplify tests.
public class ShardedStreamBuilderShould {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        // as long as we refer to the Model in delivery tag initialization.
        ModelTests.clearModel();
    }

    @Test
    public void not_accept_null_boundedContextName() {
        thrown.expect(NullPointerException.class);
        builder().setBoundedContextName(Tests.nullRef());
    }

    @Test
    public void return_set_boundedContextName() {
        BoundedContextName value = BoundedContext.newName("ShardedStreams");
        assertEquals(value, builder().setBoundedContextName(value)
                                     .getBoundedContextName());
    }

    @Test
    public void not_accept_null_key() {
        thrown.expect(NullPointerException.class);
        builder().setKey(Tests.nullRef());
    }

    @Test
    public void return_set_key() {
        ShardingKey value = mock(ShardingKey.class);
        assertEquals(value, builder().setKey(value)
                                     .getKey());
    }

    @Test
    public void not_accept_null_tag() {
        thrown.expect(NullPointerException.class);
        builder().setTag(Tests.nullRef());
    }

    @Test
    public void return_set_tag() {
        DeliveryTag value = DeliveryTag.forCommandsOf(new TaskAggregateRepository());
        assertEquals(value, builder().setTag(value)
                                     .getTag());
    }

    @Test
    public void not_accept_null_targetIdClass() {
        thrown.expect(NullPointerException.class);
        builder().setTargetIdClass(Tests.nullRef());
    }

    @Test
    public void return_set_targetIdClass() {
        Class value = ProjectId.class;
        assertEquals(value, builder().setTargetIdClass(value)
                                     .getTargetIdClass());
    }

    @Test
    public void not_accept_null_consumer() {
        thrown.expect(NullPointerException.class);
        builder().setConsumer(Tests.<Consumer>nullRef());
    }

    @Test
    public void return_set_consumer() {
        Consumer value = mock(Consumer.class);
        assertEquals(value, builder().setConsumer(value)
                                     .getConsumer());
    }

    @Test
    public void not_accept_null_transportFactory() {
        thrown.expect(NullPointerException.class);
        builder().build(Tests.nullRef());
    }
}
