/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server.entity.given;

import com.google.protobuf.DoubleValue;
import com.google.protobuf.StringValue;
import io.spine.server.entity.AbstractEntity;
import io.spine.server.entity.DefaultRecordBasedRepository;

public class DefaultEntityFactoryTestEnv {

    /** Prevents instantiation of this utility class. */
    private DefaultEntityFactoryTestEnv() {
    }

    /** A test entity class which is not versionable. */
    public static class TestEntity1 extends AbstractEntity<Long, StringValue> {
        private TestEntity1(Long id) {
            super(id);
        }
    }

    /** A test repository. */
    public static class TestRepository1
            extends DefaultRecordBasedRepository<Long, TestEntity1, StringValue> {
    }

    /** Another entity with the same ID and different state. */
    public static class TestEntity2 extends AbstractEntity<Long, DoubleValue> {
        protected TestEntity2(Long id) {
            super(id);
        }
    }

    /** A repository for {@link TestEntity2}. */
    public static class TestRepository2
            extends DefaultRecordBasedRepository<Long, TestEntity2, DoubleValue> {
    }
}
