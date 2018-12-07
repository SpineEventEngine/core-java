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

package io.spine.server.event;

import com.google.common.collect.ImmutableMultimap;
import com.google.protobuf.Message;
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

import java.util.Collection;

import static io.spine.server.event.EnrichmentsMap.getEventTypes;
import static io.spine.testing.DisplayNames.HAVE_PARAMETERLESS_CTOR;
import static io.spine.testing.Tests.assertHasPrivateParameterlessCtor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("EnrichmentsMap should")
class EnrichmentsMapTest {

    @Test
    @DisplayName(HAVE_PARAMETERLESS_CTOR)
    void haveParameterlessCtor() {
        assertHasPrivateParameterlessCtor(EnrichmentsMap.class);
    }

    @Test
    @DisplayName("return map instance")
    void returnMapInstance() {
        ImmutableMultimap<String, String> map = EnrichmentsMap.getInstance();

        assertFalse(map.isEmpty());
    }

    @Nested
    @DisplayName("contain ProjectCreated by")
    class ContainProjectCreated {

        @Test
        @DisplayName("ProjectCreatedEnrichment")
        void byProjectCreatedEnrichment() {
            assertEnrichmentIsUsedOnlyInEvents(ProjectCreated.Enrichment.class,
                                               ProjectCreated.class);
        }

        @Test
        @DisplayName("ProjectCreatedSeparateEnrichment")
        void byProjectCreatedSeparateEnrichment() {
            assertEnrichmentIsUsedOnlyInEvents(ProjectCreatedSeparateEnrichment.class,
                                               ProjectCreated.class);
        }

        @Test
        @DisplayName("ProjectCreatedEnrichmentAnotherPackage")
        void byProjectCreatedEnrichmentAnotherPackage() {
            assertEnrichmentIsUsedOnlyInEvents(ProjectCreatedEnrichmentAnotherPackage.class,
                                               ProjectCreated.class);
        }

        @Test
        @DisplayName("ProjectCreatedEnrichmentAnotherPackageFqn")
        void byProjectCreatedEnrichmentAnotherPackageFqn() {
            assertEnrichmentIsUsedOnlyInEvents(ProjectCreatedEnrichmentAnotherPackageFqn.class,
                                               ProjectCreated.class);
        }

        @Test
        @DisplayName("ProjectCreatedEnrichmentAnotherPackageFqnAndMsgOpt")
        void byProjectCreatedEnrichmentAnotherPackageFqnAndMsgOpt() {
            assertEnrichmentIsUsedOnlyInEvents(
                    ProjectCreatedEnrichmentAnotherPackageFqnAndMsgOpt.class,
                    ProjectCreated.class);
        }

        @Test
        @DisplayName("ProjectStartedEnrichment")
        void byProjectStartedEnrichment() {
            assertEnrichmentIsUsedOnlyInEvents(ProjectStarted.Enrichment.class,
                                               // Event classe
                                               ProjectStarted.class);
        }

        @Test
        @DisplayName("EnrichmentByContextFields")
        void byEnrichmentByContextFields() {
            assertEnrichmentIsUsedOnlyInEvents(EnrichmentByContextFields.class,
                                               ProjectCreated.class);
        }
    }

    @Nested
    @DisplayName("contain events")
    class ContainEvents {

        @Test
        @DisplayName("by EnrichmentForSeveralEvents")
        void byEnrichmentForSeveralEvents() {
            assertEnrichmentIsUsedOnlyInEvents(EnrichmentForSeveralEvents.class,
                                               // Event classes
                                               ProjectStarted.class,
                                               ProjectCreated.class,
                                               TaskAdded.class);
        }

        @Test
        @DisplayName("from package by one enrichment")
        void fromPackage() {
            assertEnrichmentIsUsedOnlyInEvents(UserPackageEventsEnrichment.class,
                                               // Event classes
                                               UserLoggedInEvent.class,
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
            assertEnrichmentIsAvailableForEvents(UserPackageEventsEnrichment.class,
                                                 // Event classes
                                                 PermissionRevokedEvent.class,
                                                 PermissionGrantedEvent.class);
        }

        @Test
        @DisplayName("from package and standalone event")
        void fromPackageAndStandaloneEvent() {
            assertEnrichmentIsUsedOnlyInEvents(SelectiveComplexEnrichment.class,
                                               // Event classes
                                               PermissionGrantedEvent.class,
                                               PermissionRevokedEvent.class,
                                               UserLoggedInEvent.class);
        }

        @Test
        @DisplayName("from multiple packages")
        void fromMultiplePackages() {
            assertEnrichmentIsUsedOnlyInEvents(MultiplePackageEnrichment.class,
                                               // Event classes
                                               PermissionGrantedEvent.class,
                                               PermissionRevokedEvent.class,
                                               SharingRequestSent.class,
                                               SharingRequestApproved.class);
        }
    }

    @Test
    @DisplayName("contain only events with target field if declared through package")
    void containOnlyWithTargetField() {
        assertEnrichmentIsUsedOnlyInEvents(GranterEventsEnrichment.class,
                                           // Event class
                                           PermissionGrantedEvent.class);
    }

    @Nested
    @DisplayName("contain enrichments defined with `by`")
    class ContainEnrichments {

        @Test
        @DisplayName("with two arguments")
        void withTwoArgs() {
            assertEnrichmentIsUsedOnlyInEvents(
                    EnrichmentBoundWithFieldsWithDifferentNames.class,
                    // Event classes
                    SharingRequestApproved.class,
                    PermissionGrantedEvent.class);
        }

        @Test
        @DisplayName("with two fqn arguments")
        void withTwoFqnArgs() {
            assertEnrichmentIsUsedOnlyInEvents(
                    EnrichmentBoundThoughFieldFqnWithFieldsWithDifferentNames.class,
                    // Event classes
                    SharingRequestApproved.class,
                    PermissionGrantedEvent.class);
        }

        @Test
        @DisplayName("with multiple arguments")
        void withMultipleArgs() {
            assertEnrichmentIsUsedOnlyInEvents(
                    EnrichmentBoundWithMultipleFieldsWithDifferentNames.class,
                    // Event classes
                    SharingRequestApproved.class,
                    PermissionGrantedEvent.class,
                    UserDeletedEvent.class);
        }

        @Test
        @DisplayName("with multiple arguments using wildcard")
        void withMultipleArgsThroughWildcard() {
            assertEnrichmentIsUsedOnlyInEvents(
                    EnrichmentBoundWithFieldsWithDifferentNamesOfWildcardTypes.class,
                    // Event classes
                    SharingRequestApproved.class,
                    PermissionGrantedEvent.class,
                    PermissionRevokedEvent.class);
        }

        @Test
        @DisplayName("containing separating spaces")
        void containingSeparatingSpaces() {
            assertEnrichmentIsUsedOnlyInEvents(
                    EnrichmentBoundWithFieldsSeparatedWithSpaces.class,
                    // Event classes
                    TaskAdded.class,
                    PermissionGrantedEvent.class);
        }
    }

    @SafeVarargs
    private static void assertEnrichmentIsAvailableForEvents(
            Class<? extends Message> enrichmentClass,
            Class<? extends Message>... eventClassesExpected) {
        Collection<String> eventTypesActual = getEventTypes(enrichmentClass);

        for (Class<? extends Message> expectedClass : eventClassesExpected) {
            String expectedTypeName = TypeName.of(expectedClass)
                                              .value();
            assertTrue(eventTypesActual.contains(expectedTypeName));
        }
    }

    @SafeVarargs
    private static void assertEnrichmentIsUsedOnlyInEvents(
            Class<? extends Message> enrichmentClass,
            Class<? extends Message>... eventClassesExpected) {
        Collection<String> eventTypesActual = getEventTypes(enrichmentClass);
        assertEquals(eventClassesExpected.length, eventTypesActual.size());
        assertEnrichmentIsAvailableForEvents(enrichmentClass, eventClassesExpected);
    }
}
