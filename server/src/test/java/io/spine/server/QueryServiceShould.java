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
package io.spine.server;

import com.google.common.collect.Sets;
import io.spine.client.Query;
import io.spine.client.QueryResponse;
import io.spine.core.BoundedContextName;
import io.spine.core.EventContext;
import io.spine.core.Responses;
import io.spine.core.Subscribe;
import io.spine.grpc.MemoizingObserver;
import io.spine.grpc.StreamObservers;
import io.spine.server.model.ModelTests;
import io.spine.server.projection.Projection;
import io.spine.server.projection.ProjectionRepository;
import io.spine.server.stand.Stand;
import io.spine.test.Spy;
import io.spine.test.bc.event.BcProjectCreated;
import io.spine.test.commandservice.ProjectId;
import io.spine.test.projection.Project;
import io.spine.test.projection.ProjectVBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Alex Tymchenko
 */
public class QueryServiceShould {

    private static final String PROJECTS_CONTEXT_NAME = "Projects";

    private final Set<BoundedContext> boundedContexts = Sets.newHashSet();

    private QueryService service;

    private BoundedContext projectsContext;

    private BoundedContext customersContext;

    private final MemoizingObserver<QueryResponse> responseObserver =
            StreamObservers.memoizingObserver();
    private ProjectDetailsRepository projectDetailsRepository;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        ModelTests.clearModel();
        // Create Projects Bounded Context with one repository and one projection.
        projectsContext = BoundedContext.newBuilder()
                                        .setName(PROJECTS_CONTEXT_NAME)
                                        .build();

        // Inject spy, which will be obtained later via getStand().
        Spy.ofClass(Stand.class)
           .on(projectsContext);

        final Given.ProjectAggregateRepository projectRepo =
                new Given.ProjectAggregateRepository();
        projectsContext.register(projectRepo);
        projectDetailsRepository = spy(new ProjectDetailsRepository());
        projectsContext.register(projectDetailsRepository);

        boundedContexts.add(projectsContext);

        // Create Customers Bounded Context with one repository.
        customersContext = BoundedContext.newBuilder()
                                         .setName("Customers")
                                         .build();

        // Inject spy, which will be obtained later via getStand().
        Spy.ofClass(Stand.class)
           .on(customersContext);

        final Given.CustomerAggregateRepository customerRepo =
                new Given.CustomerAggregateRepository();
        customersContext.register(customerRepo);
        boundedContexts.add(customersContext);

        final QueryService.Builder builder = QueryService.newBuilder();

        for (BoundedContext context : boundedContexts) {
            builder.add(context);
        }

        service = spy(builder.build());
    }

    @After
    public void tearDown() throws Exception {
        for (BoundedContext boundedContext : boundedContexts) {
            boundedContext.close();
        }
    }

    @Test
    public void execute_queries() {
        final Query query = Given.AQuery.readAllProjects();
        service.read(query, responseObserver);
        checkOkResponse(responseObserver);
    }

    @Test
    public void dispatch_queries_to_proper_bounded_context() {
        final Query query = Given.AQuery.readAllProjects();
        final Stand stand = projectsContext.getStand();
        service.read(query, responseObserver);

        checkOkResponse(responseObserver);
        verify(stand).execute(query, responseObserver);

        verify(customersContext.getStand(), never()).execute(query, responseObserver);
    }

    @Test
    public void fail_to_create_with_removed_bounded_context_from_builder() {
        final BoundedContext boundedContext = BoundedContext.newBuilder()
                                                            .build();

        final QueryService.Builder builder = QueryService.newBuilder();

        thrown.expect(IllegalStateException.class);
        builder.add(boundedContext)
               .remove(boundedContext)
               .build();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void fail_to_create_with_no_bounded_context() {
        thrown.expect(IllegalStateException.class);
        QueryService.newBuilder()
                    .build();
    }

    @Test
    public void return_error_if_query_failed_to_execute() {
        when(projectDetailsRepository.loadAll()).thenThrow(RuntimeException.class);
        final Query query = Given.AQuery.readAllProjects();
        service.read(query, responseObserver);
        checkFailureResponse(responseObserver);
    }

    private static void checkOkResponse(MemoizingObserver<QueryResponse> responseObserver) {
        final QueryResponse responseHandled = responseObserver.firstResponse();
        assertNotNull(responseHandled);
        assertEquals(Responses.ok(), responseHandled.getResponse());
        assertTrue(responseObserver.isCompleted());
        assertNull(responseObserver.getError());
    }

    private static void checkFailureResponse(MemoizingObserver<QueryResponse> responseObserver) {
        assertTrue(responseObserver.responses().isEmpty());
        assertFalse(responseObserver.isCompleted());
        assertNotNull(responseObserver.getError());
    }

    /*
     * Stub repositories and projections
     ***************************************************/

    private static class ProjectDetailsRepository
            extends ProjectionRepository<ProjectId, ProjectDetails, Project> {

        /**
         * {@inheritDoc}
         *
         * This method is overridden to overcome the Mockito restrictions, since Mockit does not
         * propagate all the changes into the spied object (and {@code ProjectDetailsRepository}
         * instance is spied within this test suite). In turn that leads to the failures in
         * delivery initialization, since it requires non-{@code null} bounded context name.
         *
         * @return the name of the bounded context for this repository
         */
        @Override
        public BoundedContextName getBoundedContextName() {
            return BoundedContext.newName(PROJECTS_CONTEXT_NAME);
        }
    }

    private static class ProjectDetails
            extends Projection<ProjectId, Project, ProjectVBuilder> {

        private ProjectDetails(ProjectId id) {
            super(id);
        }

        @SuppressWarnings("UnusedParameters") // OK for test method.
        @Subscribe
        public void on(BcProjectCreated event, EventContext context) {
            // Do nothing.
        }
    }
}
