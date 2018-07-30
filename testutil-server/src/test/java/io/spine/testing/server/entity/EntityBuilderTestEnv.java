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

package io.spine.testing.server.entity;

import com.google.protobuf.StringValue;
import io.spine.core.Version;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.entity.AbstractVersionableEntity;
import io.spine.testing.server.User;
import io.spine.testing.server.UserVBuilder;

/**
 * @author Alexander Yevsyukov
 * @author Dmytro Kuzmin
 */
public class EntityBuilderTestEnv {

    /**
     * The test environment aggregate for testing validation during aggregate state transition.
     */
    static class UserAggregate extends Aggregate<String, User, UserVBuilder> {
        private UserAggregate(String id) {
            super(id);
        }
    }

    static class TestEntity extends AbstractVersionableEntity<Long, StringValue> {
        protected TestEntity(Long id) {
            super(id);
        }
    }

    static class TestEntityBuilder extends EntityBuilder<TestEntity, Long, StringValue> {

        @Override
        protected void setState(TestEntity result, StringValue state, Version version) {
            // NoOp.
        }
    };
}
