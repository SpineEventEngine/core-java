/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.server.aggregate;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Message;
import io.spine.server.BoundedContext;
import io.spine.server.aggregate.given.AggregateRootTestEnv;
import io.spine.server.aggregate.given.AggregateRootTestEnv.AnAggregateRoot;
import io.spine.server.aggregate.given.AggregateRootTestEnv.ProjectDefinitionRepository;
import io.spine.server.aggregate.given.AggregateRootTestEnv.ProjectLifeCycleRepository;
import io.spine.server.model.ModelTests;
import io.spine.test.aggregate.ProjectDefinition;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.ProjectLifecycle;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Constructor;

import static io.spine.Identifier.newUuid;
import static org.junit.Assert.assertNotNull;

public class AggregateRootShould {

    private AggregateRootTestEnv.ProjectRoot aggregateRoot;
    private BoundedContext boundedContext;

    @Before
    public void setUp() {
        ModelTests.clearModel();
        boundedContext = BoundedContext.newBuilder()
                                       .build();
        ProjectId projectId = ProjectId.newBuilder()
                                       .setId(newUuid())
                                       .build();
        aggregateRoot = new AggregateRootTestEnv.ProjectRoot(boundedContext, projectId);
        boundedContext.register(new ProjectDefinitionRepository());
        boundedContext.register(new ProjectLifeCycleRepository());
    }

    @Test
    public void pass_null_tolerance_test() throws NoSuchMethodException {
        final Constructor<AnAggregateRoot> ctor =
                AnAggregateRoot.class.getDeclaredConstructor(BoundedContext.class, String.class);
        new NullPointerTester()
                .setDefault(Constructor.class, ctor)
                .setDefault(BoundedContext.class, boundedContext)
                .testStaticMethods(AggregateRoot.class, NullPointerTester.Visibility.PACKAGE);
    }

    @Test
    public void return_part_state_by_class() {
        final Message definitionPart = aggregateRoot.getPartState(ProjectDefinition.class);
        assertNotNull(definitionPart);

        final Message lifeCyclePart = aggregateRoot.getPartState(ProjectLifecycle.class);
        assertNotNull(lifeCyclePart);
    }
}
