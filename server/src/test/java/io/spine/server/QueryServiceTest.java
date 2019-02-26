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
package io.spine.server;

import com.google.common.truth.ThrowableSubject;
import io.spine.client.Query;
import io.spine.client.QueryResponse;
import io.spine.core.Responses;
import io.spine.grpc.MemoizingObserver;
import io.spine.server.Given.ProjectDetailsRepository;
import io.spine.server.model.UnknownEntityTypeException;
import io.spine.testing.logging.MuteLogging;
import io.spine.testing.server.model.ModelTests;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.truth.Truth.assertThat;
import static io.spine.client.Queries.typeOf;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.server.Given.PROJECTS_CONTEXT_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@DisplayName("QueryService should")
class QueryServiceTest {

    private Set<BoundedContext> boundedContexts;
    private QueryService service;
    private MemoizingObserver<QueryResponse> responseObserver;
    private ProjectDetailsRepository projectDetailsRepository;

    @SuppressWarnings("CheckReturnValue") // Calling builder.
    @BeforeEach
    void setUp() {
        ModelTests.dropAllModels();

        boundedContexts = newHashSet();
        responseObserver = memoizingObserver();
        // Create Projects Bounded Context with one repository and one projection.
        BoundedContext projectsContext = BoundedContext
                .newBuilder()
                .setName(PROJECTS_CONTEXT_NAME)
                .build();
        Given.ProjectAggregateRepository projectRepo = new Given.ProjectAggregateRepository();
        projectsContext.register(projectRepo);
        projectDetailsRepository = spy(new ProjectDetailsRepository());
        projectsContext.register(projectDetailsRepository);

        boundedContexts.add(projectsContext);

        // Create Customers Bounded Context with one repository.
        BoundedContext customersContext = BoundedContext
                .newBuilder()
                .setName("Customers")
                .build();
        Given.CustomerAggregateRepository customerRepo = new Given.CustomerAggregateRepository();
        customersContext.register(customerRepo);
        boundedContexts.add(customersContext);

        QueryService.Builder queryService = QueryService.newBuilder();
        for (BoundedContext context : boundedContexts) {
            queryService.add(context);
        }

        service = spy(queryService.build());
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
    @DisplayName("dispatch queries to proper Bounded Context")
    void dispatchQueriesToBc() {
        Query query = Given.AQuery.readAllProjects();
        service.read(query, responseObserver);

        checkOkResponse(responseObserver);
    }

    @Test
    @DisplayName("fail to create with Bounded Context removed from builder")
    void notCreateWithRemovedBc() {
        BoundedContext boundedContext = BoundedContext.newBuilder()
                                                      .build();

        QueryService.Builder builder = QueryService.newBuilder();

        assertThrows(IllegalStateException.class, () -> builder.add(boundedContext)
                                                               .remove(boundedContext)
                                                               .build());
    }

    @Test
    @DisplayName("fail to create with no Bounded Context")
    void notCreateWithNoBc() {
        assertThrows(IllegalStateException.class, () -> QueryService.newBuilder()
                                                                    .build());
    }

    @Test
    @MuteLogging
    @DisplayName("return error if query failed to execute")
    void returnErrorOnQueryFail() {
        when(projectDetailsRepository.loadAllRecords()).thenThrow(RuntimeException.class);
        Query query = Given.AQuery.readAllProjects();
        service.read(query, responseObserver);
        checkFailureResponse(responseObserver);
    }

    @Test
    @MuteLogging
    @DisplayName("throw an IllegalStateException if the requested entity type is unknown")
    void failOnUnknownType() {
        Query query = Given.AQuery.readUnknownType();
        service.read(query, responseObserver);
        Throwable error = responseObserver.getError();
        ThrowableSubject assertError = assertThat(error);
        assertError.isNotNull();
        assertError.isInstanceOf(UnknownEntityTypeException.class);
        String unknownTypeUrl = typeOf(query).value();
        assertError.hasMessageThat().contains(unknownTypeUrl);
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
