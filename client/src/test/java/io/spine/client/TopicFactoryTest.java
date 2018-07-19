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
package io.spine.client;

import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import io.spine.core.ActorContext;
import io.spine.protobuf.AnyPacker;
import io.spine.test.client.TestEntity;
import io.spine.test.client.TestEntityId;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Alex Tymchenko
 */
@DisplayName("Topic factory should")
class TopicFactoryTest
        extends ActorRequestFactoryTest {

    // See {@code client_requests.proto} for declaration.
    private static final Class<TestEntity> TARGET_ENTITY_CLASS = TestEntity.class;
    private static final TypeUrl TARGET_ENTITY_TYPE_NAME = TypeUrl.of(TARGET_ENTITY_CLASS);

    @Nested
    @DisplayName("create topic")
    class CreateTopic {

        @Test
        @DisplayName("for all entities of kind")
        void forAllOfKind() {
            Topic topic = factory().topic()
                                   .allOf(TARGET_ENTITY_CLASS);

            verifyTargetAndContext(topic);

            assertEquals(0, topic.getTarget()
                                 .getFilters()
                                 .getIdFilter()
                                 .getIdsCount());
        }

        @Test
        @DisplayName("for specified entities of kind")
        void forSomeOfKind() {

            Set<TestEntityId> ids = newHashSet(entityId(1), entityId(2), entityId(3));
            Topic topic = factory().topic()
                                   .someOf(TARGET_ENTITY_CLASS, ids);

            verifyTargetAndContext(topic);

            List<EntityId> actualIds = topic.getTarget()
                                            .getFilters()
                                            .getIdFilter()
                                            .getIdsList();
            assertEquals(ids.size(), actualIds.size());
            for (EntityId actualId : actualIds) {
                Any rawId = actualId.getId();
                TestEntityId unpackedId = AnyPacker.unpack(rawId);
                assertTrue(ids.contains(unpackedId));
            }
        }

        @Test
        @DisplayName("for given target")
        void forTarget() {
            Target givenTarget = Targets.allOf(TARGET_ENTITY_CLASS);
            Topic topic = factory().topic()
                                   .forTarget(givenTarget);

            verifyTargetAndContext(topic);
        }

        private void verifyTargetAndContext(Topic topic) {
            assertNotNull(topic);
            assertNotNull(topic.getId());

            assertEquals(TARGET_ENTITY_TYPE_NAME.value(), topic.getTarget()
                                                               .getType());
            assertEquals(FieldMask.getDefaultInstance(), topic.getFieldMask());

            ActorContext actualContext = topic.getContext();
            verifyContext(actualContext);

        }

        private TestEntityId entityId(int idValue) {
            return TestEntityId.newBuilder()
                               .setValue(idValue)
                               .build();
        }
    }
}
