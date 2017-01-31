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

package org.spine3.server.event.enrich;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMultimap;
import com.google.protobuf.Message;
import org.junit.Test;
import org.spine3.protobuf.TypeName;
import org.spine3.test.event.EnrichmentByContextFields;
import org.spine3.test.event.EnrichmentForSeveralEvents;
import org.spine3.test.event.ProjectCreated;
import org.spine3.test.event.ProjectCreatedSeparateEnrichment;
import org.spine3.test.event.ProjectStarted;
import org.spine3.test.event.TaskAdded;
import org.spine3.test.event.enrichment.EnrichmentBoundThoughFieldFqnWithFieldsWithDifferentNames;
import org.spine3.test.event.enrichment.EnrichmentBoundWithFieldsWithDifferentNames;
import org.spine3.test.event.enrichment.EnrichmentBoundWithFieldsWithDifferentNamesOfWildcardTypes;
import org.spine3.test.event.enrichment.EnrichmentBoundWithMultipleFieldsWithDifferentNames;
import org.spine3.test.event.enrichment.GranterEventsEnrichment;
import org.spine3.test.event.enrichment.MultiplePackageEnrichment;
import org.spine3.test.event.enrichment.ProjectCreatedEnrichmentAnotherPackage;
import org.spine3.test.event.enrichment.ProjectCreatedEnrichmentAnotherPackageFqn;
import org.spine3.test.event.enrichment.ProjectCreatedEnrichmentAnotherPackageFqnAndMsgOpt;
import org.spine3.test.event.enrichment.SelectiveComplexEnrichment;
import org.spine3.test.event.enrichment.UserPackageEventsEnrichment;
import org.spine3.test.event.user.UserDeletedEvent;
import org.spine3.test.event.user.UserLoggedInEvent;
import org.spine3.test.event.user.UserLoggedOutEvent;
import org.spine3.test.event.user.UserMentionedEvent;
import org.spine3.test.event.user.permission.PermissionGrantedEvent;
import org.spine3.test.event.user.permission.PermissionRevokedEvent;
import org.spine3.test.event.user.sharing.SharingRequestApproved;
import org.spine3.test.event.user.sharing.SharingRequestSent;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.spine3.test.Tests.hasPrivateUtilityConstructor;

/**
 * @author Alexander Litus
 */
@SuppressWarnings({"InstanceMethodNamingConvention", "OverlyCoupledClass"}) // Over-coupling is OK for test classes
public class EventEnrichmentsMapShould {

    @Test
    public void have_private_constructor() {
        assertTrue(hasPrivateUtilityConstructor(EventEnrichmentsMap.class));
    }

    @Test
    public void return_map_instance() {
        final ImmutableMultimap<String, String> map = EventEnrichmentsMap.getInstance();

        assertFalse(map.isEmpty());
    }

    @Test
    public void contain_ProjectCreated_by_ProjectCreatedEnrichment_type() {
        assertOnlyEventTypeByEnrichmentType(ProjectCreated.Enrichment.class, ProjectCreated.class);
    }

    @Test
    public void contain_ProjectCreated_by_ProjectCreatedSeparateEnrichment_type() {
        assertOnlyEventTypeByEnrichmentType(ProjectCreatedSeparateEnrichment.class, ProjectCreated.class);
    }

    @Test
    public void contain_ProjectCreated_by_ProjectCreatedEnrichmentAnotherPackage_type() {
        assertOnlyEventTypeByEnrichmentType(ProjectCreatedEnrichmentAnotherPackage.class, ProjectCreated.class);
    }

    @Test
    public void contain_ProjectCreated_by_ProjectCreatedEnrichmentAnotherPackageFqn_type() {
        assertOnlyEventTypeByEnrichmentType(ProjectCreatedEnrichmentAnotherPackageFqn.class, ProjectCreated.class);
    }

    @Test
    public void contain_ProjectCreated_by_ProjectCreatedEnrichmentAnotherPackageFqnAndMsgOpt_type() {
        assertOnlyEventTypeByEnrichmentType(ProjectCreatedEnrichmentAnotherPackageFqnAndMsgOpt.class, ProjectCreated.class);
    }

    @Test
    public void contain_ProjectStarted_by_ProjectStartedEnrichment_type() {
        assertOnlyEventTypeByEnrichmentType(ProjectStarted.Enrichment.class, ProjectStarted.class);
    }

    @Test
    public void contain_events_by_EnrichmentForSeveralEvents_type() {
        assertOnlyEventTypeByEnrichmentType(EnrichmentForSeveralEvents.class,
                                            ProjectStarted.class, ProjectCreated.class, TaskAdded.class);
    }

    @Test
    public void contain_ProjectCreated_by_EnrichmentByContextFields_type() {
        assertOnlyEventTypeByEnrichmentType(EnrichmentByContextFields.class, ProjectCreated.class);
    }

    @Test
    public void contain_all_events_from_package_by_one_enrichment() {
        assertOnlyEventTypeByEnrichmentType(UserPackageEventsEnrichment.class,
                                            UserLoggedInEvent.class,
                                            UserMentionedEvent.class,
                                            UserLoggedOutEvent.class,
                                            PermissionGrantedEvent.class,
                                            PermissionRevokedEvent.class,
                                            SharingRequestSent.class,
                                            SharingRequestApproved.class);
    }

    @Test
    public void contain_events_from_subpackage_by_enrichment_applied_to_root_package() {
        assertEventTypeByEnrichmentType(UserPackageEventsEnrichment.class,
                                        PermissionRevokedEvent.class,
                                        PermissionGrantedEvent.class);
    }

    @Test
    public void contain_only_events_with_target_field_if_declared_though_package() {
        assertOnlyEventTypeByEnrichmentType(GranterEventsEnrichment.class,
                                            PermissionGrantedEvent.class);
    }

    @Test
    public void contain_events_from_package_and_standalone_event() {
        assertOnlyEventTypeByEnrichmentType(SelectiveComplexEnrichment.class,
                                            PermissionGrantedEvent.class,
                                            PermissionRevokedEvent.class,
                                            UserLoggedInEvent.class);
    }

    @Test
    public void contain_events_from_multiple_packages() {
        assertOnlyEventTypeByEnrichmentType(MultiplePackageEnrichment.class,
                                            PermissionGrantedEvent.class,
                                            PermissionRevokedEvent.class,
                                            SharingRequestSent.class,
                                            SharingRequestApproved.class);
    }

    @Test
    public void contain_enrichments_defined_with_by_with_two_arguments() {
        assertOnlyEventTypeByEnrichmentType(EnrichmentBoundWithFieldsWithDifferentNames.class,
                                            SharingRequestApproved.class,
                                            PermissionGrantedEvent.class);
    }

    @Test
    public void contain_enrichments_defined_with_by_with_two_fqn_arguments() {
        assertOnlyEventTypeByEnrichmentType(EnrichmentBoundThoughFieldFqnWithFieldsWithDifferentNames.class,
                                            SharingRequestApproved.class,
                                            PermissionGrantedEvent.class);
    }

    @Test
    public void contain_enrichments_defined_with_by_with_multiple_arguments() {
        assertOnlyEventTypeByEnrichmentType(EnrichmentBoundWithMultipleFieldsWithDifferentNames.class,
                                            SharingRequestApproved.class,
                                            PermissionGrantedEvent.class,
                                            UserDeletedEvent.class);
    }

    @Test
    public void contain_enrichments_defined_with_by_with_multiple_arguments_using_wildcard() {
        assertOnlyEventTypeByEnrichmentType(EnrichmentBoundWithFieldsWithDifferentNamesOfWildcardTypes.class,
                                            SharingRequestApproved.class,
                                            PermissionGrantedEvent.class,
                                            PermissionRevokedEvent.class);
    }

    @SafeVarargs
    private static void assertEventTypeByEnrichmentType(
            Class<? extends Message> enrichmentClass,
            Class<? extends Message>... eventClassesExpected) {
        final Collection<String> eventTypesActual = EventEnrichmentsMap.getInstance()
                                                                       .get(TypeName.of(enrichmentClass));
        for (Class<? extends Message> expectedClass : FluentIterable.from(eventClassesExpected)) {
            final String expectedTypeName = TypeName.of(expectedClass);
            assertTrue(eventTypesActual.contains(expectedTypeName));
        }
    }

    @SafeVarargs
    private static void assertOnlyEventTypeByEnrichmentType(
            Class<? extends Message> enrichmentClass,
            Class<? extends Message>... eventClassesExpected) {
        final Collection<String> eventTypesActual = EventEnrichmentsMap.getInstance()
                                                                       .get(TypeName.of(enrichmentClass));
        assertEquals(eventClassesExpected.length, eventTypesActual.size());
        assertEventTypeByEnrichmentType(enrichmentClass, eventClassesExpected);
    }
}
