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
import com.google.common.collect.Maps;
import com.google.common.reflect.ClassPath;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.ClassName;
import org.spine3.TypeName;
import org.spine3.util.IoUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.reflect.ClassPath.ClassInfo;

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
    private static final Map<TypeName, ClassName> namesMap = Maps.newHashMap();

    private static final List<URL> readResourcesUrls = newLinkedList();

    static {
        loadClasses();
    }

    private TypeToClassMap() {}

    /**
     * @return immutable set of Protobuf types known to the application
     */
    public static ImmutableSet<TypeName> knownTypes() {

        final Set<TypeName> result = namesMap.keySet();
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

        if (!namesMap.containsKey(protoType)) {
            loadClasses();
        }
        if (!namesMap.containsKey(protoType)) {
            final ClassName className = searchAsSubclass(protoType);
            namesMap.put(protoType, className);
        }
        final ClassName result = namesMap.get(protoType);
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

            className = namesMap.get(typeName);

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

    private static void loadClasses() {

        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        loadClassesFromPropertiesFile(classLoader);
        loadDefaultProtobufClasses(classLoader);
        if (log().isDebugEnabled()) {
            log().debug("Total classes in TypeToClassMap: " + namesMap.size());
        }
    }

    private static void loadClassesFromPropertiesFile(ClassLoader classLoader) {

        Enumeration<URL> resources = null;
        try {
            resources = classLoader.getResources(PROPERTIES_FILE_PATH);
        } catch (IOException ignored) {
        }
        if (resources == null) {
            return;
        }
        while (resources.hasMoreElements()) {
            final URL resourceUrl = resources.nextElement();
            if (!readResourcesUrls.contains(resourceUrl)) {
                final Properties properties = loadProperties(resourceUrl);
                addToNamesMap(properties);
                readResourcesUrls.add(resourceUrl);
            }
        }
    }

    private static Properties loadProperties(URL resourceUrl) {

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
     * Adds all data from properties file into memory. Properties file should contain proto type urls and
     * appropriate java class names.
     *
     * @param properties Properties file to read params from
     */
    private static void addToNamesMap(Properties properties) {

        for (String key : properties.stringPropertyNames()) {
            final TypeName typeName = TypeName.of(key);
            final ClassName className = ClassName.of(properties.getProperty(key));
            namesMap.put(typeName, className);
        }
    }

    private static void loadDefaultProtobufClasses(ClassLoader classLoader) {

        final ClassPath classPath;

        try {
            classPath = ClassPath.from(classLoader);
        } catch (IOException e) {
            if (log().isWarnEnabled()) {
                log().warn("Failed to read protobuf classes.", e);
            }
            return;
        }

        final ImmutableSet<ClassPath.ClassInfo> protobufClasses = getFilteredProtobufClasses(classPath);

        for (ClassInfo classInfo : protobufClasses) {
            final TypeName typeName = getTypeNameOf(classInfo);
            final ClassName className = ClassName.of(classInfo.load());
            namesMap.put(typeName, className);
        }
    }

    private static TypeName getTypeNameOf(ClassInfo classInfo) {

        final String fullQualifiedName = classInfo.getName();
        final int dotIndex = fullQualifiedName.indexOf('.');
        // name that matches Protobuf conventions
        final String protoClassName = fullQualifiedName.substring(dotIndex + 1);
        return TypeName.of(protoClassName);
    }

    private static ImmutableSet<ClassPath.ClassInfo> getFilteredProtobufClasses(ClassPath classPath) {

        final ImmutableSet<ClassPath.ClassInfo> infos = classPath.getTopLevelClasses(GOOGLE_PROTOBUF_PACKAGE);
        final ImmutableSet.Builder<ClassPath.ClassInfo> result = ImmutableSet.builder();

        for (ClassInfo info : infos) {
            if (protobufClassMatches(info)) {
                result.add(info);
            }
        }

        return result.build();
    }

    private static boolean protobufClassMatches(ClassInfo info) {

        final String name = info.getSimpleName();
        final boolean isValueClass =
                name.endsWith(PROTOBUF_VALUE_CLASS_SUFFIX) &&
                (name.length() > PROTOBUF_VALUE_CLASS_SUFFIX.length());
        return isValueClass || name.equals("Duration");
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
