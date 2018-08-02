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
import io.spine.core.Responses;
import io.spine.grpc.MemoizingObserver;
import io.spine.grpc.StreamObservers;
import io.spine.server.Given.ProjectDetailsRepository;
import io.spine.testing.server.model.ModelTests;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static io.spine.server.Given.PROJECTS_CONTEXT_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author Alex Tymchenko
 */
@DisplayName("QueryService should")
class QueryServiceTest {

    private final Set<BoundedContext> boundedContexts = Sets.newHashSet();

    private QueryService service;

    private BoundedContext projectsContext;

    private BoundedContext customersContext;

    private final MemoizingObserver<QueryResponse> responseObserver =
            StreamObservers.memoizingObserver();
    private ProjectDetailsRepository projectDetailsRepository;

    @BeforeEach
    void setUp() {
        ModelTests.dropAllModels();
        // Create Projects Bounded Context with one repository and one projection.
        projectsContext = BoundedContext.newBuilder()
                                        .setName(PROJECTS_CONTEXT_NAME)
                                        .build();
        Given.ProjectAggregateRepository projectRepo = new Given.ProjectAggregateRepository();
        projectsContext.register(projectRepo);
        projectDetailsRepository = spy(new ProjectDetailsRepository());
        projectsContext.register(projectDetailsRepository);

        boundedContexts.add(projectsContext);

        // Create Customers Bounded Context with one repository.
        customersContext = BoundedContext.newBuilder()
                                         .setName("Customers")
                                         .build();
        Given.CustomerAggregateRepository customerRepo = new Given.CustomerAggregateRepository();
        customersContext.register(customerRepo);
        boundedContexts.add(customersContext);

        QueryService.Builder builder = QueryService.newBuilder();

        for (BoundedContext context : boundedContexts) {
            builder.add(context);
        }

        service = spy(builder.build());
    }

    @AfterEach
    void tearDown() throws Exception {
        for (BoundedContext boundedContext : boundedContexts) {
            boundedContext.close();
        }
    }

    @Test
    @DisplayName("execute queries")
    void executeQueries() {
        Query query = Given.AQuery.readAllProjects();
        service.read(query, responseObserver);
        checkOkResponse(responseObserver);
    }

    @Test
    @DisplayName("dispatch queries to proper bounded context")
    void dispatchQueriesToBc() {
        Query query = Given.AQuery.readAllProjects();
        service.read(query, responseObserver);

        checkOkResponse(responseObserver);
    }

    @Test
    @DisplayName("fail to create with bounded context removed from builder")
    void notCreateWithRemovedBc() {
        BoundedContext boundedContext = BoundedContext.newBuilder()
                                                      .build();

        QueryService.Builder builder = QueryService.newBuilder();

        assertThrows(IllegalStateException.class, () -> builder.add(boundedContext)
                                                               .remove(boundedContext)
                                                               .build());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    @DisplayName("fail to create with no bounded context")
    void notCreateWithNoBc() {
        assertThrows(IllegalStateException.class, () -> QueryService.newBuilder()
                                                                    .build());
    }

    @Test
    @DisplayName("return error if query failed to execute")
    void returnErrorOnQueryFail() {
        when(projectDetailsRepository.loadAll()).thenThrow(RuntimeException.class);
        Query query = Given.AQuery.readAllProjects();
        service.read(query, responseObserver);
        checkFailureResponse(responseObserver);
    }

    private static void checkOkResponse(MemoizingObserver<QueryResponse> responseObserver) {
        QueryResponse responseHandled = responseObserver.firstResponse();
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
}
