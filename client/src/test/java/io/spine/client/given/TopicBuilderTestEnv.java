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

package io.spine.client.given;

import com.google.protobuf.Message;
import io.spine.client.Filter;
import io.spine.test.client.TestEntity;
import io.spine.test.client.TestEntityId;
import io.spine.type.TypeUrl;

import static java.lang.String.format;
import static java.util.concurrent.ThreadLocalRandom.current;
import static org.junit.Assert.fail;

public class TopicBuilderTestEnv {

    public static final Class<? extends Message> TEST_ENTITY_TYPE = TestEntity.class;
    public static final TypeUrl TEST_ENTITY_TYPE_URL = TypeUrl.of(TEST_ENTITY_TYPE);

    /** Prevents instantiation of this test environment class. */
    private TopicBuilderTestEnv() {
    }

    public static TestEntityId newMessageId() {
        return TestEntityId.newBuilder()
                           .setValue(current().nextInt(-1000, 1000))
                           .build();
    }

    public static Filter findByName(Iterable<Filter> filters, String name) {
        for (Filter filter : filters) {
            if (filter.getFieldPath()
                      .getFieldName(0)
                      .equals(name)) {
                return filter;
            }
        }
        fail(format("No Filter found for %s.", name));
        // avoid returning `null`
        throw new RuntimeException("never happens unless JUnit is broken");
    }
}
