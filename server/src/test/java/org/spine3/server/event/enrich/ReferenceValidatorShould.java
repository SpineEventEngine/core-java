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

import com.google.common.base.Optional;
import com.google.common.collect.Multimap;
import com.google.protobuf.Descriptors.FieldDescriptor;
import org.junit.Test;
import org.spine3.server.event.Given;
import org.spine3.server.event.enrich.ReferenceValidator.ValidationResult;
import org.spine3.test.event.ProjectCreated;
import org.spine3.test.event.enrichment.EnrichmentBoundWithMultipleFieldsWithDifferentNames;
import org.spine3.test.event.enrichment.GranterEventsEnrichment;
import org.spine3.test.event.enrichment.ProjectCreatedEnrichmentAnotherPackage;
import org.spine3.test.event.user.UserDeletedEvent;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.spine3.test.Verify.assertEmpty;
import static org.spine3.test.Verify.assertSize;

/**
 * @author Dmytro Dashenkov
 */
public class ReferenceValidatorShould {

    private final EventEnricher eventEnricher = Given.Enrichment.newEventEnricher();

    @Test
    public void initialize_with_valid_enricher() {
        final ReferenceValidator validator = new ReferenceValidator(eventEnricher,
                                                                    ProjectCreated.class,
                                                                    ProjectCreatedEnrichmentAnotherPackage.class);
        assertNotNull(validator);
    }

    @Test
    public void store_valid_map_of_enrichment_fields_after_validation() {
        final ReferenceValidator validator
                = new ReferenceValidator(eventEnricher,
                                         UserDeletedEvent.class,
                                         EnrichmentBoundWithMultipleFieldsWithDifferentNames.class);
        final ValidationResult result = validator.validate();
        final Multimap<FieldDescriptor, FieldDescriptor> fieldMap = result.getFieldMap();
        assertNotNull(fieldMap);
        assertFalse(fieldMap.isEmpty());
        assertSize(1, fieldMap);

        final Iterator<? extends Map.Entry<?, ? extends Collection<?>>> fieldsIterator = fieldMap.asMap()
                                                                                                 .entrySet()
                                                                                                 .iterator();
        assertTrue(fieldsIterator.hasNext());
        final Map.Entry<?, ? extends Collection<?>> entry = fieldsIterator.next();

        @SuppressWarnings("unchecked")
        final Map.Entry<FieldDescriptor, Collection<FieldDescriptor>> fieldEntry
                = (Map.Entry<FieldDescriptor, Collection<FieldDescriptor>>) entry;

        final FieldDescriptor eventField = fieldEntry.getKey();
        final String eventFieldName = eventField.getName();
        assertEquals("deleted_uid", eventFieldName);

        final Collection<FieldDescriptor> enrichmentFields = fieldEntry.getValue();
        assertFalse(enrichmentFields.isEmpty());
        assertSize(1, enrichmentFields);

        final Iterator<FieldDescriptor> enrichmentFieldIterator = enrichmentFields.iterator();
        assertTrue(enrichmentFieldIterator.hasNext());

        final FieldDescriptor enrichmentField = enrichmentFieldIterator.next();
        final String enrichmentFieldName = enrichmentField.getName();
        assertEquals("user_google_uid", enrichmentFieldName);
    }

    @Test(expected = IllegalStateException.class)
    public void fail_validation_if_enrichment_is_not_declared() {
        final ReferenceValidator validator = new ReferenceValidator(eventEnricher,
                                                                    UserDeletedEvent.class,
                                                                    GranterEventsEnrichment.class);
        validator.validate();
    }

    @Test
    public void skip_mapping_if_no_mapping_function_is_defined() {
        final EventEnricher mockEnricher = mock(EventEnricher.class);
        when(mockEnricher.functionFor(any(Class.class), any(Class.class)))
                .thenReturn(Optional.<EnrichmentFunction<?, ?>>absent());
        final ReferenceValidator validator
                = new ReferenceValidator(mockEnricher,
                                         UserDeletedEvent.class,
                                         EnrichmentBoundWithMultipleFieldsWithDifferentNames.class);
        final ValidationResult result = validator.validate();
        final List<EnrichmentFunction<?, ?>> functions = result.getFunctions();
        assertTrue(functions.isEmpty());
        final Multimap<FieldDescriptor, FieldDescriptor> fields = result.getFieldMap();
        assertEmpty(fields);
    }
}
