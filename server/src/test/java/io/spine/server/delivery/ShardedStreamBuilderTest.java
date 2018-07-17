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
import io.spine.test.aggregate.ProjectId;
import io.spine.testing.Tests;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.spine.server.delivery.given.ShardedStreamTestEnv.builder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * @author Alex Tymchenko
 */
@SuppressWarnings({"DuplicateStringLiteralInspection", /* Common test display names. */
        "InnerClassMayBeStatic", "ClassCanBeStatic" /* JUnit nested classes cannot be static. */,
        "unchecked" /* The numerous generic parameters are omitted to simplify tests. */})
@DisplayName("ShardedStream Builder should")
class ShardedStreamBuilderTest {

    @BeforeEach
    void setUp() {
        // As long as we refer to the Model in delivery tag initialization.
        ModelTests.clearModel();
    }

    @Nested
    @DisplayName("not accept null")
    class NotAcceptNull {

        @Test
        @DisplayName("BoundedContext name")
        void boundedContextName() {
            assertThrows(NullPointerException.class,
                         () -> builder().setBoundedContextName(Tests.nullRef()));
        }

        @Test
        @DisplayName("key")
        void key() {
            assertThrows(NullPointerException.class, () -> builder().setKey(Tests.nullRef()));
        }

        @Test
        @DisplayName("tag")
        void tag() {
            assertThrows(NullPointerException.class, () -> builder().setTag(Tests.nullRef()));
        }

        @Test
        @DisplayName("target ID class")
        void targetIdClass() {
            assertThrows(NullPointerException.class,
                         () -> builder().setTargetIdClass(Tests.nullRef()));
        }

        @Test
        @DisplayName("consumer")
        void consumer() {
            assertThrows(NullPointerException.class,
                         () -> builder().setConsumer(Tests.<Consumer>nullRef()));
        }

        @Test
        @DisplayName("transport factory")
        void transportFactory() {
            assertThrows(NullPointerException.class, () -> builder().build(Tests.nullRef()));
        }
    }

    @Nested
    @DisplayName("return set")
    class ReturnSet {

        @Test
        @DisplayName("BoundedContext name")
        void boundedContextName() {
            BoundedContextName value = BoundedContext.newName("ShardedStreams");
            assertEquals(value, builder().setBoundedContextName(value)
                                         .getBoundedContextName());
        }

        @Test
        @DisplayName("key")
        void key() {
            ShardingKey value = mock(ShardingKey.class);
            assertEquals(value, builder().setKey(value)
                                         .getKey());
        }

        @Test
        @DisplayName("tag")
        void tag() {
            DeliveryTag value = DeliveryTag.forCommandsOf(new TaskAggregateRepository());
            assertEquals(value, builder().setTag(value)
                                         .getTag());
        }

        @Test
        @DisplayName("target ID class")
        void targetIdClass() {
            Class value = ProjectId.class;
            assertEquals(value, builder().setTargetIdClass(value)
                                         .getTargetIdClass());
        }

        @Test
        @DisplayName("consumer")
        void consumer() {
            Consumer value = mock(Consumer.class);
            assertEquals(value, builder().setConsumer(value)
                                         .getConsumer());
        }
    }
}
