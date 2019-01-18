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

package io.spine.server.event.enrich;

import com.google.common.collect.ImmutableSet;
import io.spine.test.event.EnrichmentByContextFields;
import io.spine.test.event.EnrichmentForSeveralEvents;
import io.spine.test.event.ProjectCreated;
import io.spine.test.event.ProjectCreatedSeparateEnrichment;
import io.spine.test.event.ProjectStarted;
import io.spine.test.event.TaskAdded;
import io.spine.test.event.enrichment.EnrichmentBoundThoughFieldFqnWithFieldsWithDifferentNames;
import io.spine.test.event.enrichment.EnrichmentBoundWithFieldsSeparatedWithSpaces;
import io.spine.test.event.enrichment.EnrichmentBoundWithFieldsWithDifferentNames;
import io.spine.test.event.enrichment.EnrichmentBoundWithFieldsWithDifferentNamesOfWildcardTypes;
import io.spine.test.event.enrichment.EnrichmentBoundWithMultipleFieldsWithDifferentNames;
import io.spine.test.event.enrichment.GranterEventsEnrichment;
import io.spine.test.event.enrichment.MultiplePackageEnrichment;
import io.spine.test.event.enrichment.ProjectCreatedEnrichmentAnotherPackage;
import io.spine.test.event.enrichment.ProjectCreatedEnrichmentAnotherPackageFqn;
import io.spine.test.event.enrichment.ProjectCreatedEnrichmentAnotherPackageFqnAndMsgOpt;
import io.spine.test.event.enrichment.SelectiveComplexEnrichment;
import io.spine.test.event.enrichment.UserPackageEventsEnrichment;
import io.spine.test.event.user.UserDeletedEvent;
import io.spine.test.event.user.UserLoggedInEvent;
import io.spine.test.event.user.UserLoggedOutEvent;
import io.spine.test.event.user.UserMentionedEvent;
import io.spine.test.event.user.permission.PermissionGrantedEvent;
import io.spine.test.event.user.permission.PermissionRevokedEvent;
import io.spine.test.event.user.sharing.SharingRequestApproved;
import io.spine.test.event.user.sharing.SharingRequestSent;
import io.spine.type.TypeName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.server.event.enrich.EnrichmentAssertion._assert;

@DisplayName("EnrichmentsMap should")
class EnrichmentMapTest {

    @Test
    @DisplayName("return map instance")
    void returnMapInstance() {
        ImmutableSet<TypeName> typeNames = EnrichmentMap.load()
                                                        .enrichmentTypes();
        assertThat(typeNames).isNotEmpty();
    }

    @Nested
    @DisplayName("contain ProjectCreated by")
    class ContainProjectCreated {

        @Test
        @DisplayName("ProjectCreatedEnrichment")
        void byProjectCreatedEnrichment() {
            _assert(ProjectCreated.Enrichment.class)
                    .enrichesOnly(ProjectCreated.class);
        }

        @Test
        @DisplayName("ProjectCreatedSeparateEnrichment")
        void byProjectCreatedSeparateEnrichment() {
            _assert(ProjectCreatedSeparateEnrichment.class)
                    .enrichesOnly(ProjectCreated.class);
        }

        @Test
        @DisplayName("ProjectCreatedEnrichmentAnotherPackage")
        void byProjectCreatedEnrichmentAnotherPackage() {
            _assert(ProjectCreatedEnrichmentAnotherPackage.class)
                    .enrichesOnly(ProjectCreated.class);
        }

        @Test
        @DisplayName("ProjectCreatedEnrichmentAnotherPackageFqn")
        void byProjectCreatedEnrichmentAnotherPackageFqn() {
            _assert(ProjectCreatedEnrichmentAnotherPackageFqn.class)
                    .enrichesOnly(ProjectCreated.class);
        }

        @Test
        @DisplayName("ProjectCreatedEnrichmentAnotherPackageFqnAndMsgOpt")
        void byProjectCreatedEnrichmentAnotherPackageFqnAndMsgOpt() {
            _assert(ProjectCreatedEnrichmentAnotherPackageFqnAndMsgOpt.class)
                    .enrichesOnly(ProjectCreated.class);
        }

        @Test
        @DisplayName("ProjectStartedEnrichment")
        void byProjectStartedEnrichment() {
            _assert(ProjectStarted.Enrichment.class)
                    .enrichesOnly(ProjectStarted.class);
        }

        @Test
        @DisplayName("EnrichmentByContextFields")
        void byEnrichmentByContextFields() {
            _assert(EnrichmentByContextFields.class)
                    .enrichesOnly(ProjectCreated.class);
        }
    }

    @Nested
    @DisplayName("contain events")
    class ContainEvents {

        @Test
        @DisplayName("by EnrichmentForSeveralEvents")
        void byEnrichmentForSeveralEvents() {
            _assert(EnrichmentForSeveralEvents.class)
                    .enrichesOnly(ProjectStarted.class,
                                  ProjectCreated.class,
                                  TaskAdded.class);
        }

        @Test
        @DisplayName("from package by one enrichment")
        void fromPackage() {
            _assert(UserPackageEventsEnrichment.class)
                    .enrichesOnly(UserLoggedInEvent.class,
                                  UserMentionedEvent.class,
                                  UserLoggedOutEvent.class,
                                  PermissionGrantedEvent.class,
                                  PermissionRevokedEvent.class,
                                  SharingRequestSent.class,
                                  SharingRequestApproved.class);
        }

        @Test
        @DisplayName("from subpackage by enrichment applied to root package")
        void fromSubpackage() {
            _assert(UserPackageEventsEnrichment.class)
                    .enriches(PermissionRevokedEvent.class,
                              PermissionGrantedEvent.class);
        }

        @Test
        @DisplayName("from package and standalone event")
        void fromPackageAndStandaloneEvent() {
            _assert(SelectiveComplexEnrichment.class)
                    .enrichesOnly(PermissionGrantedEvent.class,
                                  PermissionRevokedEvent.class,
                                  UserLoggedInEvent.class);
        }

        @Test
        @DisplayName("from multiple packages")
        void fromMultiplePackages() {
            _assert(MultiplePackageEnrichment.class)
                    .enrichesOnly(PermissionGrantedEvent.class,
                                  PermissionRevokedEvent.class,
                                  SharingRequestSent.class,
                                  SharingRequestApproved.class);
        }
    }

    @Test
    @DisplayName("contain only events with target field if declared through package")
    void containOnlyWithTargetField() {
        _assert(GranterEventsEnrichment.class)
                .enrichesOnly(PermissionGrantedEvent.class);
    }

    @Nested
    @DisplayName("contain enrichments defined with `by`")
    class ContainEnrichments {

        @Test
        @DisplayName("with two arguments")
        void withTwoArgs() {
            _assert(EnrichmentBoundWithFieldsWithDifferentNames.class)
                    .enrichesOnly(SharingRequestApproved.class,
                                  PermissionGrantedEvent.class);
        }

        @Test
        @DisplayName("with two fqn arguments")
        void withTwoFqnArgs() {
            _assert(EnrichmentBoundThoughFieldFqnWithFieldsWithDifferentNames.class)
                    .enrichesOnly(SharingRequestApproved.class,
                                  PermissionGrantedEvent.class);
        }

        @Test
        @DisplayName("with multiple arguments")
        void withMultipleArgs() {
            _assert(EnrichmentBoundWithMultipleFieldsWithDifferentNames.class)
                    .enrichesOnly(SharingRequestApproved.class,
                                  PermissionGrantedEvent.class,
                                  UserDeletedEvent.class);
        }

        @Test
        @DisplayName("with multiple arguments using wildcard")
        void withMultipleArgsThroughWildcard() {
            _assert(EnrichmentBoundWithFieldsWithDifferentNamesOfWildcardTypes.class)
                    .enrichesOnly(SharingRequestApproved.class,
                                  PermissionGrantedEvent.class,
                                  PermissionRevokedEvent.class);
        }

        @Test
        @DisplayName("containing separating spaces")
        void containingSeparatingSpaces() {
            _assert(EnrichmentBoundWithFieldsSeparatedWithSpaces.class)
                    .enrichesOnly(TaskAdded.class,
                                  PermissionGrantedEvent.class);
        }
    }
}
