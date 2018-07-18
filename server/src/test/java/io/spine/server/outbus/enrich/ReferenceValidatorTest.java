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

package io.spine.server.outbus.enrich;

import com.google.common.collect.Multimap;
import com.google.protobuf.Descriptors.FieldDescriptor;
import io.spine.server.outbus.enrich.ReferenceValidator.ValidationResult;
import io.spine.server.outbus.enrich.given.ReferenceValidatorTestEnv.Enrichment;
import io.spine.test.event.ProjectCreated;
import io.spine.test.event.TaskAdded;
import io.spine.test.event.enrichment.EnrichmentBoundWithFieldsSeparatedWithSpaces;
import io.spine.test.event.enrichment.EnrichmentBoundWithMultipleFieldsWithDifferentNames;
import io.spine.test.event.enrichment.GranterEventsEnrichment;
import io.spine.test.event.enrichment.ProjectCreatedEnrichmentAnotherPackage;
import io.spine.test.event.user.UserDeletedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.spine.test.Verify.assertEmpty;
import static io.spine.test.Verify.assertSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Dmytro Dashenkov
 */
@DisplayName("ReferenceValidator should")
class ReferenceValidatorTest {

    private static final String USER_GOOGLE_UID_FIELD = "user_google_uid";
    private final Enricher enricher = Enrichment.newEventEnricher();

    @Test
    @DisplayName("initialize with valid enricher")
    void initWithValidEnricher() {
        ReferenceValidator validator =
                new ReferenceValidator(enricher,
                                       ProjectCreated.class,
                                       ProjectCreatedEnrichmentAnotherPackage.class);
        assertNotNull(validator);
    }

    @Test
    @DisplayName("store valid map of enrichment fields after validation")
    void storeFieldsAfterValidation() {
        ReferenceValidator validator
                = new ReferenceValidator(enricher,
                                         UserDeletedEvent.class,
                                         EnrichmentBoundWithMultipleFieldsWithDifferentNames.class);
        ValidationResult result = validator.validate();
        Multimap<FieldDescriptor, FieldDescriptor> fieldMap = result.getFieldMap();
        assertNotNull(fieldMap);
        assertFalse(fieldMap.isEmpty());
        assertSize(1, fieldMap);

        Iterator<? extends Map.Entry<?, ? extends Collection<?>>> fieldsIterator =
                fieldMap.asMap()
                        .entrySet()
                        .iterator();
        assertTrue(fieldsIterator.hasNext());
        Map.Entry<?, ? extends Collection<?>> entry = fieldsIterator.next();

        @SuppressWarnings("unchecked")
        Map.Entry<FieldDescriptor, Collection<FieldDescriptor>> fieldEntry
                = (Map.Entry<FieldDescriptor, Collection<FieldDescriptor>>) entry;

        FieldDescriptor eventField = fieldEntry.getKey();
        String eventFieldName = eventField.getName();
        assertEquals("deleted_uid", eventFieldName);

        Collection<FieldDescriptor> enrichmentFields = fieldEntry.getValue();
        assertFalse(enrichmentFields.isEmpty());
        assertSize(1, enrichmentFields);

        Iterator<FieldDescriptor> enrichmentFieldIterator = enrichmentFields.iterator();
        assertTrue(enrichmentFieldIterator.hasNext());

        FieldDescriptor enrichmentField = enrichmentFieldIterator.next();
        String enrichmentFieldName = enrichmentField.getName();
        assertEquals(USER_GOOGLE_UID_FIELD, enrichmentFieldName);
    }

    @Test
    @DisplayName("fail validation if enrichment is not declared")
    void failIfEnrichmentNotDeclared() {
        ReferenceValidator validator = new ReferenceValidator(enricher,
                                                              UserDeletedEvent.class,
                                                              GranterEventsEnrichment.class);
        assertThrows(IllegalStateException.class, validator::validate);
    }

    @Test
    @DisplayName("skip mapping if no mapping function is defined")
    void skipMappingIfNoFuncDefined() {
        Enricher<?, ?> mockEnricher = mock(Enricher.class);
        when(mockEnricher.functionFor(any(Class.class), any(Class.class)))
                .thenReturn(Optional.empty());
        ReferenceValidator validator
                = new ReferenceValidator(mockEnricher,
                                         UserDeletedEvent.class,
                                         EnrichmentBoundWithMultipleFieldsWithDifferentNames.class);
        ValidationResult result = validator.validate();
        List<EnrichmentFunction<?, ?, ?>> functions = result.getFunctions();
        assertTrue(functions.isEmpty());
        Multimap<FieldDescriptor, FieldDescriptor> fields = result.getFieldMap();
        assertEmpty(fields);
    }

    @Test
    @DisplayName("handle separator spaces in `by` argument")
    void handleSeparatorSpaces() {
        ReferenceValidator validator
                = new ReferenceValidator(enricher,
                                         TaskAdded.class,
                                         EnrichmentBoundWithFieldsSeparatedWithSpaces.class);
        ValidationResult result = validator.validate();
        Multimap<FieldDescriptor, FieldDescriptor> fieldMap = result.getFieldMap();
        assertFalse(fieldMap.isEmpty());
        assertSize(1, fieldMap);

        Iterator<Map.Entry<FieldDescriptor, Collection<FieldDescriptor>>> mapIterator =
                fieldMap.asMap()
                        .entrySet()
                        .iterator();
        assertTrue(mapIterator.hasNext());
        Map.Entry<FieldDescriptor, Collection<FieldDescriptor>> singleEntry =
                mapIterator.next();
        FieldDescriptor boundField = singleEntry.getKey();

        String boundFieldName = boundField.getName();
        assertEquals("project_id", boundFieldName);

        Collection<FieldDescriptor> targets = singleEntry.getValue();
        assertSize(1, targets);

        FieldDescriptor targetField = targets.iterator()
                                             .next();
        String targetFieldName = targetField.getName();
        assertEquals(USER_GOOGLE_UID_FIELD, targetFieldName);
    }
}
