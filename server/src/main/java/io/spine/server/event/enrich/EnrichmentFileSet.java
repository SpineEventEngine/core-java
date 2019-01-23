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

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMultimap;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import io.spine.Resources;
import io.spine.option.OptionsProto;
import io.spine.type.KnownTypes;
import io.spine.type.TypeName;
import io.spine.type.TypeUrl;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.collect.Sets.newHashSet;
import static io.spine.io.PropertyFiles.loadAllProperties;
import static io.spine.util.Preconditions2.checkNotEmptyOrBlank;
import static java.util.stream.Collectors.toList;

/**
 * Loads enrichment information from resource files named
 * {@link Resources#ENRICHMENTS enrichments.properties}.
 */
final class EnrichmentFileSet {

    private static final char PROTO_PACKAGE_SEPARATOR = '.';

    private static final Pattern pipeSeparatorPattern = Pattern.compile("\\|");

    /**
     * Constant indicating a package qualifier.
     *
     * <p>If this string is used as a suffix to a name of the proto package,
     * it means “all messages in the package”.
     */
    private static final String PACKAGE_WILDCARD_INDICATOR = ".*";

    /** A separator between event types in the `.properties` file. */
    private static final String EVENT_TYPE_SEPARATOR = ",";

    private static final Splitter eventTypeSplitter = Splitter.on(EVENT_TYPE_SEPARATOR);

    private final Iterable<Properties> properties;
    private final ImmutableMultimap.Builder<String, String> builder;

    /**
     * Loads the enrichment map from all resource files.
     */
    static ImmutableMultimap<String, String> loadFromResources() {
        Set<Properties> files = loadAllProperties(Resources.ENRICHMENTS);
        EnrichmentFileSet builder = new EnrichmentFileSet(files);
        ImmutableMultimap<String, String> result = builder.build();
        return result;
    }

    private EnrichmentFileSet(Iterable<Properties> properties) {
        this.properties = properties;
        this.builder = ImmutableMultimap.builder();
    }

    private ImmutableMultimap<String, String> build() {
        for (Properties props : this.properties) {
            parse(props);
        }
        return builder.build();
    }

    private void parse(Properties props) {
        Set<String> enrichmentTypes = props.stringPropertyNames();
        for (String enrichmentType : enrichmentTypes) {
            String eventTypesStr = props.getProperty(enrichmentType);
            Iterable<String> eventTypes = eventTypeSplitter.split(eventTypesStr);
            put(enrichmentType, eventTypes);
        }
    }

    private void put(String enrichmentType, Iterable<String> eventQualifiers) {
        for (String eventQualifier : eventQualifiers) {
            if (isPackage(eventQualifier)) {
                putAllTypesFromPackage(enrichmentType, eventQualifier);
            } else {
                builder.put(enrichmentType, eventQualifier);
            }
        }
    }

    /**
     * Puts all the events from the given package into the map to match the
     * given enrichment type.
     *
     * @param enrichmentType type of the enrichment for the given events
     * @param eventsPackage  package qualifier representing the protobuf package containing
     *                       the event to enrich
     */
    private void putAllTypesFromPackage(String enrichmentType, String eventsPackage) {
        int lastSignificantCharPos = eventsPackage.length() -
                PACKAGE_WILDCARD_INDICATOR.length();
        String packageName = eventsPackage.substring(0, lastSignificantCharPos);
        Set<String> boundFields = getBoundFields(enrichmentType);
        Collection<TypeUrl> eventTypes = KnownTypes.instance()
                                                   .getAllFromPackage(packageName);
        for (TypeUrl type : eventTypes) {
            if (hasOneOfTargetFields(type.toName(), boundFields)) {
                String typeQualifier = type.getTypeName();
                builder.put(enrichmentType, typeQualifier);
            }
        }
    }

    private static Set<String> getBoundFields(String enrichmentType) {
        Descriptor enrichmentDescriptor = TypeName.of(enrichmentType)
                                                              .getMessageDescriptor();
        Set<String> result = newHashSet();
        for (FieldDescriptor field : enrichmentDescriptor.getFields()) {
            String extension = field.getOptions()
                                    .getExtension(OptionsProto.by);
            Collection<String> fieldNames = parseFieldNames(extension);
            result.addAll(fieldNames);
        }
        return result;
    }

    private static Collection<String> parseFieldNames(String qualifiers) {
        Collection<String> result =
                pipeSeparatorPattern.splitAsStream(qualifiers)
                                    .map(String::trim)
                                    .filter(fieldName -> !fieldName.isEmpty())
                                    .map(EnrichmentFileSet::getSimpleFieldName)
                                    .collect(toList());
        return result;
    }

    @SuppressWarnings("ConstantConditions")
    private static String getSimpleFieldName(String qualifier) {
        int startIndex = qualifier.lastIndexOf(PROTO_PACKAGE_SEPARATOR) + 1;
        startIndex = startIndex > 0 // 0 is an invalid value, see line above
                     ? startIndex
                     : 0;
        String fieldName = qualifier.substring(startIndex);
        return fieldName;
    }

    private static
    boolean hasOneOfTargetFields(TypeName eventType, Collection<String> targetFields) {
        Descriptor eventDescriptor = eventType.getMessageDescriptor();
        List<FieldDescriptor> fields = eventDescriptor.getFields();
        Set<String> fieldNames =
                fields.stream()
                      .map(FieldDescriptor::getName)
                      .collect(Collectors.toSet());
        Optional<String> found =
                targetFields.stream()
                            .filter(fieldNames::contains)
                            .findAny();
        return found.isPresent();
    }

    /**
     * Returns {@code true} if the given qualifier is a package according to the contract
     * of {@code (enrichment_for)} option notation.
     */
    private static boolean isPackage(String qualifier) {
        checkNotEmptyOrBlank(qualifier);

        int indexOfWildcardChar = qualifier.indexOf(PACKAGE_WILDCARD_INDICATOR);
        int qualifierLength = qualifier.length();

        boolean result =
                indexOfWildcardChar == (qualifierLength - PACKAGE_WILDCARD_INDICATOR.length());
        return result;
    }
}
