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

package io.spine.system.server;

import com.google.protobuf.Any;
import com.google.protobuf.Timestamp;
import io.spine.client.Query;
import io.spine.client.QueryFactory;
import io.spine.server.BoundedContext;
import io.spine.server.aggregate.Aggregate;
import io.spine.testing.client.TestActorRequestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Iterator;

import static io.spine.system.server.SystemBoundedContexts.systemOf;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Dmytro Dashenkov
 */
@DisplayName("MirrorRepository should")
class MirrorRepositoryTest {

    private MirrorRepository repository;
    private QueryFactory queries;

    @BeforeEach
    void setUp() {
        BoundedContext domainContext = BoundedContext.newBuilder().build();
        BoundedContext systemContext = systemOf(domainContext);
        repository = (MirrorRepository) systemContext
                .findRepository(Mirror.class)
                .orElseGet(() -> fail("MirrorRepository must be registered."));
        queries = TestActorRequestFactory.newInstance(MirrorRepositoryTest.class).query();
    }

    @Nested
    @DisplayName("on an aggregate query")
    class ExecuteQueries {

        @Nested
        @DisplayName("for a known type")
        class Known {

            @Test
            @DisplayName("find all instances")
            void includeAll() {

            }
        }

        @Test
        @DisplayName("for an unknown type")
        void unknown() {
            Query query = queries.all(Timestamp.class);

            Iterator<Any> result = repository.execute(query);
            assertFalse(result.hasNext());
        }
    }

    private void prepareAggreagtes(Aggregate<?, ?, ?>... aggregates) {

    }
}
