/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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

package org.spine3.protobuf;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.ClassName;
import org.spine3.TypeName;
import org.spine3.protobuf.error.UnknownTypeInAnyException;
import org.spine3.util.IoUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static com.google.common.collect.Maps.newHashMap;

/**
 * Utility class for reading real proto class names from properties file.
 *
 * @author Mikhail Mikhaylov
 * @author Alexander Yevsyukov
 * @author Alexander Litus
 */
@SuppressWarnings("UtilityClass")
public class TypeToClassMap {

    private static final char CLASS_PACKAGE_DELIMITER = '.';

    //TODO:2015-09-09:mikhail.mikhaylov: Find a way to read it from gradle properties.
    /**
     * File, containing Protobuf messages' typeUrls and their appropriate class names.
     * Is generated with Gradle during build process.
     */
    private static final String PROPERTIES_FILE_PATH = "proto_to_java_class.properties";

    /**
     * A map from Protobuf type name to Java class name.
     *
     * <p>Example:
     * <p>{@code spine.base.EventId} - {@code org.spine3.base.EventId}
     */
    private static final Map<TypeName, ClassName> NAMES_MAP = buildNamesMap();


    private TypeToClassMap() {}

    /**
     * Retrieves Protobuf types known to the application.
     *
     * @return immutable set of Protobuf types known to the application
     */
    public static ImmutableSet<TypeName> knownTypes() {

        final Set<TypeName> result = NAMES_MAP.keySet();
        return ImmutableSet.copyOf(result);
    }

    /**
     * Retrieves compiled proto's java class name by proto type url
     * to be used to parse {@link Message} from {@link Any}.
     *
     * @param protoType {@link Any} type url
     * @return Java class name
     * @throws UnknownTypeInAnyException if there is no such type known to the application
     */
    public static ClassName get(TypeName protoType) {

        if (!NAMES_MAP.containsKey(protoType)) {
            final ClassName className = searchInnerMessageClass(protoType);
            NAMES_MAP.put(protoType, className);
        }
        final ClassName result = NAMES_MAP.get(protoType);
        return result;
    }

    /**
     * Attempts to find a {@link ClassName} for the passed inner Protobuf type.
     *
     * <p>For example, com.package.OuterClass.InnerClass class name.
     *
     * @param type {@link TypeName} of the class to find
     * @return the found class name
     * @throws UnknownTypeInAnyException if there is no such type known to the application
     */
    private static ClassName searchInnerMessageClass(TypeName type) {

        String lookupType = type.value();
        ClassName className = null;
        final StringBuilder suffix = new StringBuilder(lookupType.length());
        int lastDotPosition = lookupType.lastIndexOf(CLASS_PACKAGE_DELIMITER);
        while (className == null && lastDotPosition != -1) {
            suffix.insert(0, lookupType.substring(lastDotPosition));
            lookupType = lookupType.substring(0, lastDotPosition);
            final TypeName typeName = TypeName.of(lookupType);
            className = NAMES_MAP.get(typeName);
            lastDotPosition = lookupType.lastIndexOf(CLASS_PACKAGE_DELIMITER);
        }
        if (className == null) {
            throw new UnknownTypeInAnyException(type.value());
        }
        className = ClassName.of(className.value() + suffix);
        try {
            Class.forName(className.value());
        } catch (ClassNotFoundException e) {
            //noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
            throw new UnknownTypeInAnyException(type.value());
        }
        return className;
    }

    private static Map<TypeName, ClassName> buildNamesMap() {

        final Map<TypeName, ClassName> result = loadNamesFromProperties();
        final ImmutableMap<TypeName, ClassName> protobufNames = buildProtobufNamesMap();
        result.putAll(protobufNames);
        if (log().isDebugEnabled()) {
            log().debug("Total classes in TypeToClassMap: " + result.size());
        }
        return result;
    }

    /**
     * Returns needed classes from the {@code com.google.protobuf} package.
     * Every class name ends with {@code Value} (except {@link Duration} class).
     * Other classes from this package are unnecessary.
     */
    @SuppressWarnings("TypeMayBeWeakened") // Not in this case
    private static ImmutableMap<TypeName, ClassName> buildProtobufNamesMap() {

        return ImmutableMap.<TypeName, ClassName>builder()
                .put(TypeName.of(ListValue.getDescriptor()), ClassName.of(ListValue.class))
                .put(TypeName.of(Int64Value.getDescriptor()), ClassName.of(Int64Value.class))
                .put(TypeName.of(Int32Value.getDescriptor()), ClassName.of(Int32Value.class))
                .put(TypeName.of(UInt64Value.getDescriptor()), ClassName.of(UInt64Value.class))
                .put(TypeName.of(UInt32Value.getDescriptor()), ClassName.of(UInt32Value.class))
                .put(TypeName.of(BytesValue.getDescriptor()), ClassName.of(BytesValue.class))
                .put(TypeName.of(StringValue.getDescriptor()), ClassName.of(StringValue.class))
                .put(TypeName.of(DoubleValue.getDescriptor()), ClassName.of(DoubleValue.class))
                .put(TypeName.of(BoolValue.getDescriptor()), ClassName.of(BoolValue.class))
                .put(TypeName.of(EnumValue.getDescriptor()), ClassName.of(EnumValue.class))
                .put(TypeName.of(FloatValue.getDescriptor()), ClassName.of(FloatValue.class))
                .put(TypeName.of(Duration.getDescriptor()), ClassName.of(Duration.class))
                .build();
    }

    private static Map<TypeName, ClassName> loadNamesFromProperties() {

        final Map<TypeName, ClassName> result = newHashMap();
        final Set<Properties> propertiesSet = loadAllProperties();
        for (Properties properties : propertiesSet) {
            putTo(result, properties);
        }
        return result;
    }

    private static void putTo(Map<TypeName, ClassName> result, Properties properties) {

        for (String key : properties.stringPropertyNames()) {
            final TypeName typeName = TypeName.of(key);
            final ClassName className = ClassName.of(properties.getProperty(key));
            result.put(typeName, className);
        }
    }

    /**
     * Loads all data from property file(s) into memory. The property file should contain proto type urls and
     * appropriate java class names.
     */
    @SuppressWarnings("TypeMayBeWeakened") // Not in this case
    private static ImmutableSet<Properties> loadAllProperties() {

        final Enumeration<URL> resources = getResources();
        if (resources == null) {
            return ImmutableSet.<Properties>builder().build();
        }
        final ImmutableSet.Builder<Properties> result = ImmutableSet.builder();
        while (resources.hasMoreElements()) {
            final URL resourceUrl = resources.nextElement();
            final Properties properties = loadPropertiesFile(resourceUrl);
            result.add(properties);
        }
        return result.build();
    }

    private static Enumeration<URL> getResources() {

        Enumeration<URL> resources = null;
        try {
            resources = getContextClassLoader().getResources(PROPERTIES_FILE_PATH);
        } catch (IOException e) {
            if (log().isWarnEnabled()) {
                log().warn("Failed to load resources: " + PROPERTIES_FILE_PATH, e);
            }
        }
        return resources;
    }

    private static Properties loadPropertiesFile(URL resourceUrl) {

        final Properties properties = new Properties();
        InputStream inputStream = null;
        try {
            inputStream = resourceUrl.openStream();
            properties.load(inputStream);
        } catch (IOException e) {
            if (log().isWarnEnabled()) {
                log().warn("Failed to load properties file.", e);
            }
        } finally {
            IoUtil.closeSilently(inputStream);
        }
        return properties;
    }

    private static ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(TypeToClassMap.class);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }
}
