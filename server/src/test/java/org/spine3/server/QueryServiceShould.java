/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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
package org.spine3.server;

import com.google.common.collect.Sets;
import io.grpc.stub.StreamObserver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.EventContext;
import org.spine3.base.Responses;
import org.spine3.client.Query;
import org.spine3.client.QueryResponse;
import org.spine3.server.event.Subscribe;
import org.spine3.server.projection.Projection;
import org.spine3.server.projection.ProjectionRepository;
import org.spine3.server.stand.Stand;
import org.spine3.test.bc.event.ProjectCreated;
import org.spine3.test.commandservice.ProjectId;
import org.spine3.test.projection.Project;
import org.spine3.testdata.TestStandFactory;

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
import static org.spine3.testdata.TestBoundedContextFactory.newBoundedContext;

/**
 * @author Alex Tymchenko
 */
public class QueryServiceShould {

    private QueryService service;

    private final Set<BoundedContext> boundedContexts = Sets.newHashSet();

    private BoundedContext projectsContext;
    private BoundedContext customersContext;
    private final TestQueryResponseObserver responseObserver = new TestQueryResponseObserver();
    private ProjectDetailsRepository projectDetailsRepository;

    @Before
    public void setUp() {
        // Create Projects Bounded Context with one repository and one projection.
        projectsContext = newBoundedContext(spy(TestStandFactory.create()));

        final Given.ProjectAggregateRepository projectRepo = new Given.ProjectAggregateRepository(projectsContext);
        projectsContext.register(projectRepo);
        projectDetailsRepository = spy(new ProjectDetailsRepository(projectsContext));
        projectsContext.register(projectDetailsRepository);

        boundedContexts.add(projectsContext);

        // Create Customers Bounded Context with one repository.
        customersContext = newBoundedContext(spy(TestStandFactory.create()));
        final Given.CustomerAggregateRepository customerRepo = new Given.CustomerAggregateRepository(customersContext);
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
        final Query query = Given.Query.readAllProjects();
        service.read(query, responseObserver);
        checkOkResponse(responseObserver);
    }

    @Test
    public void dispatch_queries_to_proper_bounded_context() {
        final Query query = Given.Query.readAllProjects();
        final Stand stand = projectsContext.getStand();
        service.read(query, responseObserver);

        checkOkResponse(responseObserver);
        verify(stand).execute(query, responseObserver);

        verify(customersContext.getStand(), never()).execute(query, responseObserver);
    }

    @Test(expected = IllegalStateException.class)
    public void fail_to_create_with_removed_bounded_context_from_builder() {
        final BoundedContext boundedContext = newBoundedContext(TestStandFactory.create());

        final QueryService.Builder builder = QueryService.newBuilder();
        builder.add(boundedContext)
               .remove(boundedContext)
               .build();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test(expected = IllegalStateException.class)
    public void fail_to_create_with_no_bounded_context() {
        QueryService.newBuilder()
                    .build();
    }

    @Test
    public void return_error_if_query_failed_to_execute() {
        when(projectDetailsRepository.loadAll()).thenThrow(RuntimeException.class);
        final Query query = Given.Query.readAllProjects();
        service.read(query, responseObserver);
        checkFailureResponse(responseObserver);
    }

    private static void checkOkResponse(TestQueryResponseObserver responseObserver) {
        final QueryResponse responseHandled = responseObserver.getResponseHandled();
        assertNotNull(responseHandled);
        assertEquals(Responses.ok(), responseHandled.getResponse());
        assertTrue(responseObserver.isCompleted());
        assertNull(responseObserver.getThrowable());
    }

    private static void checkFailureResponse(TestQueryResponseObserver responseObserver) {
        final QueryResponse responseHandled = responseObserver.getResponseHandled();
        assertNull(responseHandled);
        assertFalse(responseObserver.isCompleted());
        assertNotNull(responseObserver.getThrowable());
    }

    /*
     * Stub repositories and projections
     ***************************************************/

    private static class ProjectDetailsRepository extends ProjectionRepository<ProjectId, ProjectDetails, Project> {

        protected ProjectDetailsRepository(BoundedContext boundedContext) {
            super(boundedContext);
        }
    }

    private static class ProjectDetails extends Projection<ProjectId, Project> {

        /**
         * Creates a new instance.
         *
         * @param id the ID for the new instance
         * @throws IllegalArgumentException if the ID is not of one of the supported types
         */
        public ProjectDetails(ProjectId id) {
            super(id);
        }

        @SuppressWarnings("UnusedParameters") // OK for test method.
        @Subscribe
        public void on(ProjectCreated event, EventContext context) {
            // Do nothing.
        }
    }

    private static class TestQueryResponseObserver implements StreamObserver<QueryResponse> {

        private QueryResponse responseHandled;
        private Throwable throwable;
        private boolean isCompleted = false;

        @Override
        public void onNext(QueryResponse response) {
            this.responseHandled = response;
        }

        @Override
        public void onError(Throwable throwable) {
            this.throwable = throwable;
        }

        @Override
        public void onCompleted() {
            this.isCompleted = true;
        }

        QueryResponse getResponseHandled() {
            return responseHandled;
        }

        Throwable getThrowable() {
            return throwable;
        }

        boolean isCompleted() {
            return isCompleted;
        }
    }
}
