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

package io.spine.testing.server.blackbox.verify.query;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import com.google.common.truth.extensions.proto.IterableOfProtosSubject;
import com.google.common.truth.extensions.proto.ProtoSubject;
import com.google.protobuf.Message;
import io.spine.client.QueryResponse;
import io.spine.core.Status;
import io.spine.core.Status.StatusCase;
import io.spine.core.Version;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import static com.google.common.truth.Fact.simpleFact;
import static com.google.common.truth.Truth.assertAbout;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.extensions.proto.ProtoTruth.protos;
import static io.spine.util.Exceptions.newIllegalArgumentException;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * assertQueryResult(query)
 *      .ignoringFieldAbsence()
 *      .containsExactly(state1, state2)
 *      or
 *      .containsElementsIn(ImmutableList.of(state1, state2, state3))
 *      or
 *      .containsSingleEntityStateThat()
 *      or
 *      .containsSingleEntityVersionThat()
 *      or
 *      .containsEntityVersionsSuchThat()
 *      .ignoringFieldAbsence()
 *      .contains(version)
 *      or
 *      .hasStatus(Status)
 */

@VisibleForTesting
public final class QueryResultSubject
        extends IterableOfProtosSubject<QueryResultSubject, Message, Iterable<Message>> {

    private final Collection<Version> entityVersions = new ArrayList<>();
    private Status status;

    private QueryResultSubject(FailureMetadata failureMetadata,
                               Iterable<Message> entityStates) {
        super(failureMetadata, entityStates);
    }

    private void setResponseData(QueryResponse queryResponse) {
        entityVersions.addAll(extractEntityVersions(queryResponse));
        status = extractStatus(queryResponse);
    }

    public static
    QueryResultSubject assertQueryResponse(@Nullable QueryResponse queryResponse) {
        Iterable<Message> entityStates = extractEntityStates(queryResponse);
        QueryResultSubject subject = assertAbout(queryResult()).that(entityStates);
        if (queryResponse == null) {
            subject.failWithoutActual(simpleFact("`QueryResult` should never be `null`"));
            return subject;
        }
        subject.setResponseData(queryResponse);
        return subject;
    }

    private static Iterable<Message> extractEntityStates(@Nullable QueryResponse queryResponse) {
        if (queryResponse == null) {
            return ImmutableList.of();
        }
        List<Message> result = queryResponse.states()
                                            .stream()
                                            .map(Message.class::cast)
                                            .collect(toList());
        return result;
    }

    private static Collection<Version> extractEntityVersions(QueryResponse queryResponse) {
        return ImmutableList.of();
    }

    private static Status extractStatus(QueryResponse queryResponse) {
        return queryResponse.getResponse()
                            .getStatus();
    }

    @SuppressWarnings("unchecked")
    // It's up to user to provide the predicate matching the stored entity states.
    public <M extends Message> void containsAllMatching(Predicate<M> predicate) {
        Iterable<Message> entityStates = actual();
        entityStates.forEach(state -> {
            try {
                M cast = (M) state;
                assertTrue(predicate.test(cast));
            } catch (ClassCastException e) {
                throw newIllegalArgumentException(
                        e, "Query response contains an entity state of type %s which doesn't " +
                                "match the specified predicate type", state.getClass()
                                                                           .getName()
                );
            }
        });
    }

    public void hasStatus(StatusCase statusCase) {
        assertThat(status.getStatusCase()).isEqualTo(statusCase);
    }

    public ProtoSubject<?, Message> hasStatusThat() {
        return check("getResponse().getStatus()").about(protos())
                                                 .that(status);
    }

    static
    Subject.Factory<QueryResultSubject, Iterable<Message>> queryResult() {
        return QueryResultSubject::new;
    }
}
