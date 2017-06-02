/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.aggregate;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Message;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.spine3.server.BoundedContext;
import org.spine3.server.aggregate.given.AggregateRootTestEnv;
import org.spine3.server.aggregate.given.AggregateRootTestEnv.AnAggregateRoot;
import org.spine3.server.aggregate.given.AggregateRootTestEnv.ProjectDefinitionRepository;
import org.spine3.server.aggregate.given.AggregateRootTestEnv.ProjectLifeCycleRepository;
import org.spine3.test.aggregate.ProjectDefinition;
import org.spine3.test.aggregate.ProjectId;
import org.spine3.test.aggregate.ProjectLifecycle;

import java.lang.reflect.Constructor;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.spine3.base.Identifiers.newUuid;

public class AggregateRootShould {

    private AggregateRootTestEnv.ProjectRoot aggregateRoot;
    private BoundedContext boundedContext;

    @Before
    public void setUp() {
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

    @SuppressWarnings("unchecked")
    // Supply a "wrong" value on purpose to cause the validation failure.
    @Test(expected = IllegalStateException.class)
    public void throw_exception_when_aggregate_root_does_not_have_appropriate_constructor() {
        AggregateRoot.create(newUuid(), boundedContext, AggregateRoot.class);
    }

    @Test
    public void create_aggregate_root_entity() {
        final AnAggregateRoot aggregateRoot =
                AggregateRoot.create(newUuid(), boundedContext, AnAggregateRoot.class);
        assertNotNull(aggregateRoot);
    }

    @Test
    public void return_part_state_by_class() {
        final Message definitionPart = aggregateRoot.getPartState(ProjectDefinition.class);
        assertNotNull(definitionPart);

        final Message lifeCyclePart = aggregateRoot.getPartState(ProjectLifecycle.class);
        assertNotNull(lifeCyclePart);
    }

    @Ignore //TODO:2017-06-02:alexander.yevsyukov: Return back when fixing Mockito
    @Test
    public void cache_repositories() {
        final AggregateRoot rootSpy = spy(aggregateRoot);
        final Class<ProjectDefinition> partClass = ProjectDefinition.class;

        rootSpy.getPartState(partClass);
        rootSpy.getPartState(partClass);

        verify(rootSpy, times(1)).lookup(partClass);
    }

    @Ignore //TODO:2017-06-02:alexander.yevsyukov: Return back when fixing Mockito
    @Test
    public void have_different_cache_for_different_instances() {
        final ProjectId projectId = ProjectId.getDefaultInstance();
        final AggregateRoot firstRoot = new AggregateRootTestEnv.ProjectRoot(boundedContext, projectId);
        final AggregateRoot secondRoot = new AggregateRootTestEnv.ProjectRoot(boundedContext, projectId);
        final AggregateRoot firstRootSpy = spy(firstRoot);
        final AggregateRoot secondRootSpy = spy(secondRoot);
        final Class<ProjectDefinition> partClass = ProjectDefinition.class;

        firstRootSpy.getPartState(partClass);
        secondRootSpy.getPartState(partClass);

        verify(firstRootSpy, times(1)).lookup(partClass);
        verify(secondRootSpy, times(1)).lookup(partClass);
    }
}
