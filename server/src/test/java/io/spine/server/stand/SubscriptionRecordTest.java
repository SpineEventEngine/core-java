/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.server.stand;

import io.spine.base.EventMessage;
import io.spine.client.CompositeFilter;
import io.spine.client.EntityStateUpdate;
import io.spine.client.Filter;
import io.spine.client.Filters;
import io.spine.client.Subscription;
import io.spine.client.SubscriptionId;
import io.spine.client.SubscriptionUpdate;
import io.spine.client.Subscriptions;
import io.spine.client.Target;
import io.spine.core.Event;
import io.spine.core.EventId;
import io.spine.protobuf.AnyPacker;
import io.spine.server.type.EventEnvelope;
import io.spine.test.aggregate.Project;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.event.EvTeamId;
import io.spine.test.event.ProjectCreated;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.google.common.truth.Truth8.assertThat;
import static io.spine.client.Targets.composeTarget;
import static io.spine.server.stand.given.SubscriptionRecordTestEnv.OTHER_TYPE;
import static io.spine.server.stand.given.SubscriptionRecordTestEnv.projectCreatedEnvelope;
import static io.spine.server.stand.given.SubscriptionRecordTestEnv.projectId;
import static io.spine.server.stand.given.SubscriptionRecordTestEnv.projectWithName;
import static io.spine.server.stand.given.SubscriptionRecordTestEnv.stateChangedEnvelope;
import static io.spine.server.stand.given.SubscriptionRecordTestEnv.subscription;
import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SubscriptionRecord should")
class SubscriptionRecordTest {

    private static final String TARGET_ID = "target-ID";
    private static final Project EMPTY_PRJ = Project.getDefaultInstance();

    @Test
    @DisplayName("detect an update according to type")
    void notMatchImproperType() {
        SubscriptionRecord record = SubscriptionRecord.of(subscription());
        ProjectId id = ProjectId.getDefaultInstance();

        EventEnvelope matches = stateChangedEnvelope(id, EMPTY_PRJ, EMPTY_PRJ);
        assertThat(record.detectUpdate(matches)).isPresent();

        EventEnvelope notMatches = stateChangedEnvelope(id, EMPTY_PRJ, EMPTY_PRJ, OTHER_TYPE);
        assertThat(record.detectUpdate(notMatches)).isEmpty();
    }

    @Nested
    @DisplayName("detect an update by comparing IDs")
    class MatchById {

        @Test
        @DisplayName("in case of entity subscription")
        void entitySubscription() {
            ProjectId targetId = projectId(TARGET_ID);
            Project state = Project.getDefaultInstance();
            SubscriptionRecord record = SubscriptionRecord.of(subscription(targetId));

            EventEnvelope envelope = stateChangedEnvelope(targetId, state, state);
            assertThat(record.detectUpdate(envelope)).isPresent();

            ProjectId otherId = projectId("some-other-ID");
            EventEnvelope envelope2 = stateChangedEnvelope(otherId, state, state);
            assertThat(record.detectUpdate(envelope2)).isEmpty();
        }

        @Test
        @DisplayName("in case of event subscription")
        void eventSubscription() {
            EventId targetId = EventId
                    .newBuilder()
                    .setValue(TARGET_ID)
                    .build();
            Target target = composeTarget(ProjectCreated.class, singleton(targetId), null);
            Subscription subscription = subscription(target);
            SubscriptionRecord record = SubscriptionRecord.of(subscription);

            EventEnvelope envelope = projectCreatedEnvelope(targetId);
            assertThat(record.detectUpdate(envelope)).isPresent();

            EventId otherId = EventId
                    .newBuilder()
                    .setValue("other-event-ID")
                    .build();
            EventEnvelope nonMatching = projectCreatedEnvelope(otherId);
            assertThat(record.detectUpdate(nonMatching)).isEmpty();
        }
    }

    @Nested
    @DisplayName("detect an update by analyzing state")
    class MatchByState {

        @Test
        @DisplayName("in case of entity subscription")
        void entitySubscription() {
            String targetName = "super-project";
            ProjectId targetId = projectId(TARGET_ID);
            SubscriptionRecord record = projectRecord(targetId,
                                                      Filters.eq("name", targetName));
            Project matching = projectWithName(targetName);
            EventEnvelope envelope = stateChangedEnvelope(targetId, EMPTY_PRJ, matching);
            Optional<SubscriptionUpdate> maybeUpdate = record.detectUpdate(envelope);
            assertThat(maybeUpdate).isPresent();
            EntityStateUpdate entityUpdate = firstEntityUpdate(maybeUpdate);
            assertEquals(matching, AnyPacker.unpack(entityUpdate.getState()));

            Project nonMatching = projectWithName("some-other-name");
            EventEnvelope envelope2 = stateChangedEnvelope(targetId, EMPTY_PRJ, nonMatching);
            assertThat(record.detectUpdate(envelope2)).isEmpty();
        }

        @Test
        @DisplayName("in case of event subscription")
        void eventSubscription() {
            EventId targetId = EventId
                    .newBuilder()
                    .setValue(TARGET_ID)
                    .build();
            String targetTeamId = "target-team-ID";

            Filter filter = Filters.eq("team_id.id", targetTeamId);
            CompositeFilter compositeFilter = Filters.all(filter);
            Set<CompositeFilter> filters = singleton(compositeFilter);
            Target target = composeTarget(ProjectCreated.class, singleton(targetId), filters);

            Subscription subscription = subscription(target);
            SubscriptionRecord record = SubscriptionRecord.of(subscription);

            EvTeamId matchingTeamId = EvTeamId
                    .newBuilder()
                    .setId(targetTeamId)
                    .build();
            ProjectCreated matching = ProjectCreated
                    .newBuilder()
                    .setTeamId(matchingTeamId)
                    .build();
            EventEnvelope envelope = projectCreatedEnvelope(targetId, matching);
            Optional<SubscriptionUpdate> maybeUpdate = record.detectUpdate(envelope);
            assertThat(maybeUpdate).isPresent();

            Event event = firstEventUpdate(maybeUpdate);
            EventMessage message = EventEnvelope.of(event)
                                                .message();
            assertEquals(matching, message);

            EvTeamId otherTeamId = EvTeamId
                    .newBuilder()
                    .setId("some-other-team-ID")
                    .build();
            ProjectCreated other = ProjectCreated
                    .newBuilder()
                    .setTeamId(otherTeamId)
                    .build();
            EventEnvelope nonMatching = projectCreatedEnvelope(targetId, other);
            assertThat(record.detectUpdate(nonMatching)).isEmpty();
        }
    }

    @Test
    @DisplayName("detect that the entity state stopped matching the subscription criteria")
    void tellNoLongerMatching() {
        ProjectId targetId = projectId(TARGET_ID);
        String targetName = "previously-matching-project";

        SubscriptionRecord record = projectRecord(targetId,
                                                  Filters.eq("name", targetName));

        Project matching = projectWithName(targetName);
        Project nonMatching = projectWithName("not-matching-anymore");

        EventEnvelope envelope = stateChangedEnvelope(targetId, matching, nonMatching);
        Optional<SubscriptionUpdate> maybeUpdate = record.detectUpdate(envelope);
        assertThat(maybeUpdate).isPresent();

        EntityStateUpdate entityUpdate = firstEntityUpdate(maybeUpdate);
        assertTrue(entityUpdate.getNoLongerMatching());
    }

    @Test
    @DisplayName("be equal only to SubscriptionRecord that has same subscription")
    void beEqualToSame() {
        Subscription oneSubscription = subscription();
        SubscriptionId breakingId = Subscriptions.newId("breaking-id");
        Subscription otherSubscription = oneSubscription
                .toBuilder()
                .setId(breakingId)
                .build();
        @SuppressWarnings("QuestionableName")
        SubscriptionRecord one = SubscriptionRecord.of(oneSubscription);
        SubscriptionRecord similar = SubscriptionRecord.of(otherSubscription);
        SubscriptionRecord same = SubscriptionRecord.of(oneSubscription);
        assertNotEquals(one, similar);
        assertEquals(one, same);
    }

    /**
     * Creates a {@code SubscriptionRecord} with the target referring to the passed {@code TargetId}
     * and with the passed {@code filter} applied.
     *
     * @implNote This method accesses the package-private API, hence it isn't moved to the
     *         corresponding test environment.
     */
    private static SubscriptionRecord projectRecord(ProjectId targetId, Filter filter) {
        CompositeFilter compositeFilter = Filters.all(filter);
        Set<CompositeFilter> filters = singleton(compositeFilter);
        Target target = composeTarget(Project.class, singleton(targetId), filters);

        Subscription subscription = subscription(target);
        return SubscriptionRecord.of(subscription);
    }


    @SuppressWarnings("OptionalUsedAsFieldOrParameterType") // OK for tests.
    private static EntityStateUpdate firstEntityUpdate(Optional<SubscriptionUpdate> maybeUpdate) {
        @SuppressWarnings("OptionalGetWithoutIsPresent")    // Checked by `Truth8.assertThat(..)`.
                SubscriptionUpdate update = maybeUpdate.get();
        List<EntityStateUpdate> updateList = update.getEntityUpdates()
                                                   .getUpdateList();
        assertEquals(1, updateList.size());

        return updateList.get(0);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType") // OK for tests.
    private static Event firstEventUpdate(Optional<SubscriptionUpdate> maybeUpdate) {
        @SuppressWarnings("OptionalGetWithoutIsPresent")    // Checked by `Truth8.assertThat(..)`.
                SubscriptionUpdate update = maybeUpdate.get();
        List<Event> updateList = update.getEventUpdates()
                                       .getEventList();
        assertEquals(1, updateList.size());

        return updateList.get(0);
    }
}
