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

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Any;
import com.google.protobuf.Duration;
import com.google.protobuf.Message;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.ClassName;
import org.spine3.TypeName;
import org.spine3.util.IoUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;

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

    private static final String GOOGLE_PROTOBUF_PACKAGE = "com.google.protobuf";

    @SuppressWarnings("DuplicateStringLiteralInspection") // OK in this case
    private static final String PROTOBUF_VALUE_CLASS_SUFFIX = "Value";

    //TODO:2015-09-09:mikhail.mikhaylov: Find a way to read it from gradle properties.
    /**
     * File, containing Protobuf messages' typeUrls and their appropriate class names.
     * Is generated with Gradle during build process.
     */
    private static final String PROPERTIES_FILE_PATH = "proto_to_java_class.properties";

    //TODO:2015-09-17:alexander.yevsyukov:  @mikhail.mikhaylov: Have immutable instance here.
    // Transform static methods into inner Builder class
    // that would populate its internal structure and then emits it to be stored in this field.
    /**
     * Proto type url on java class name map. Example:
     * spine.base.EventId - org.spine3.base.EventId
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
     */
    public static ClassName get(TypeName protoType) {

        if (!NAMES_MAP.containsKey(protoType)) {
            final ClassName className = searchAsSubclass(protoType);
            NAMES_MAP.put(protoType, className);
        }
        final ClassName result = NAMES_MAP.get(protoType);
        return result;
    }

    private static ClassName searchAsSubclass(TypeName lookupTypeName) {

        String lookupType = lookupTypeName.value();
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
            throw new UnknownTypeInAnyException(lookupTypeName.value());
        }

        className = ClassName.of(className.value() + suffix);
        try {
            Class.forName(className.value());
        } catch (ClassNotFoundException e) {
            //noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
            throw new UnknownTypeInAnyException(lookupTypeName.value());
        }
        return className;
    }

    private static Map<TypeName, ClassName> buildNamesMap() {

        final Map<TypeName, ClassName> result = loadNamesFromProperties();
        final Map<TypeName, ClassName> protobufNames = loadProtobufNames();
        result.putAll(protobufNames);
        if (log().isDebugEnabled()) {
            log().debug("Total classes in TypeToClassMap: " + result.size());
        }
        return result;
    }

    private static Map<TypeName, ClassName> loadNamesFromProperties() {

        final Map<TypeName, ClassName> result = newHashMap();
        final Set<Properties> propertiesSet = loadAllProperties();
        for (Properties properties : propertiesSet) {
            putTo(result, properties);
        }
        return result;
    }

    private static Map<TypeName, ClassName> loadProtobufNames() {

        final Map<TypeName, ClassName> result = newHashMap();
        final ImmutableSet<Class<? extends Message>> protobufClasses = loadProtobufClasses();

        for (Class<? extends Message> clazz : protobufClasses) {
            final TypeName typeName = TypeName.of(clazz);
            final ClassName className = ClassName.of(clazz);
            result.put(typeName, className);
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
     * Loads all data from property file(s) into memory. Properties file should contain proto type urls and
     * appropriate java class names.
     */
    private static Set<Properties> loadAllProperties() {

        final Enumeration<URL> resources = getResources();
        if (resources == null) {
            return newHashSet();
        }
        final Set<Properties> propertiesSet = newHashSet();
        while (resources.hasMoreElements()) {
            final URL resourceUrl = resources.nextElement();
            final Properties properties = loadPropertiesFile(resourceUrl);
            propertiesSet.add(properties);
        }
        return propertiesSet;
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

    /**
     * Returns filtered classes from the {@link #GOOGLE_PROTOBUF_PACKAGE}.
     */
    private static ImmutableSet<Class<? extends Message>> loadProtobufClasses() {

        final ConfigurationBuilder configuration = new ConfigurationBuilder()
                .forPackages(GOOGLE_PROTOBUF_PACKAGE)
                .addClassLoader(getContextClassLoader());
        final Reflections reflections = new Reflections(configuration);
        final Set<Class<? extends Message>> messageClasses = reflections.getSubTypesOf(Message.class);
        return filterProtobufClasses(messageClasses);
    }

    private static ImmutableSet<Class<? extends Message>> filterProtobufClasses(Iterable<Class<? extends Message>> classes) {

        final ImmutableSet.Builder<Class<? extends Message>> result = ImmutableSet.builder();
        for (Class<? extends Message> clazz : classes) {
            if (protobufClassMatches(clazz)) {
                result.add(clazz);
            }
        }
        return result.build();
    }

    /**
     * Returns true if class name ends with {@link #PROTOBUF_VALUE_CLASS_SUFFIX} or class is {@link Duration} class.
     */
    private static boolean protobufClassMatches(Class<? extends Message> clazz) {

        final String name = clazz.getSimpleName();
        final boolean isValueClass =
                name.endsWith(PROTOBUF_VALUE_CLASS_SUFFIX) &&
                (name.length() > PROTOBUF_VALUE_CLASS_SUFFIX.length());
        return isValueClass || clazz.equals(Duration.class);
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
