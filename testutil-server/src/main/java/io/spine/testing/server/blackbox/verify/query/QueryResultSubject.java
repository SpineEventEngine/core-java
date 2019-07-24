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
import io.spine.testing.server.entity.EntityVersionSubject;
import io.spine.testing.server.entity.IterableEntityVersionSubject;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import static com.google.common.truth.Fact.simpleFact;
import static com.google.common.truth.Truth.assertAbout;
import static com.google.common.truth.extensions.proto.ProtoTruth.protos;
import static io.spine.testing.server.blackbox.verify.query.ResponseStatusSubject.assertResponseStatus;
import static io.spine.testing.server.entity.IterableEntityVersionSubject.assertEntityVersions;
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

/**
 * ...
 */
@VisibleForTesting
public final class QueryResultSubject
        extends IterableOfProtosSubject<QueryResultSubject, Message, Iterable<Message>> {

    /**
     * ...
     *
     * <p>Is effectively {@code final}.
     */
    private ResponseStatusSubject statusSubject;

    /**
     * ...
     *
     * <p>Is effectively {@code final}.
     */
    private IterableEntityVersionSubject versionsSubject;

    /**
     * ...
     *
     * <p>The {@code entityStates} is never actually {@code null}, but may be an empty
     * {@code Iterable}.
     */
    private QueryResultSubject(FailureMetadata failureMetadata,
                               Iterable<Message> entityStates) {
        super(failureMetadata, entityStates);
    }

    private void initChildSubjects(QueryResponse queryResponse) {
        Status status = extractStatus(queryResponse);
        statusSubject = assertResponseStatus(status);

        Iterable<Version> versions = extractEntityVersions(queryResponse);
        versionsSubject = assertEntityVersions(versions);
    }

    public static
    QueryResultSubject assertQueryResponse(@Nullable QueryResponse queryResponse) {
        Iterable<Message> entityStates = extractEntityStates(queryResponse);
        QueryResultSubject subject = assertAbout(queryResult()).that(entityStates);
        if (queryResponse == null) {
            subject.failWithoutActual(simpleFact("`QueryResponse` must never be `null`"));
            return subject;
        }
        subject.initChildSubjects(queryResponse);
        return subject;
    }

    public ResponseStatusSubject hasStatusThat() {
        return statusSubject;
    }

    public void hasStatus(StatusCase status) {
        statusSubject.hasStatusCase(status);
    }

    public ProtoSubject<?, Message> containsSingleEntityStateThat() {
        assertContainsSingleItem();
        Message entityState = actual().iterator()
                                      .next();
        ProtoSubject<?, Message> subject =
                check("iterator().next()").about(protos())
                                          .that(entityState);
        return subject;
    }

    public EntityVersionSubject containsSingleEntityVersionThat() {
        return versionsSubject.containsSingleEntityVersionThat();
    }

    public IterableEntityVersionSubject containsEntityVersionsSuchThat() {
        return versionsSubject;
    }

    // todo try to parameterize the class instead of this ugliness
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

    private void assertContainsSingleItem() {
        hasSize(1);
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

    static
    Subject.Factory<QueryResultSubject, Iterable<Message>> queryResult() {
        return QueryResultSubject::new;
    }
}
