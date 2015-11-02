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
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.ClassName;
import org.spine3.TypeName;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

import static com.google.common.collect.Lists.newLinkedList;

/**
 * Utility class for reading real proto class names from properties file.
 *
 * @author Mikhail Mikhaylov
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("UtilityClass")
public class TypeToClassMap {

    private static final char CLASS_PACKAGE_DELIMITER = '.';
    private static final Logger LOG = LoggerFactory.getLogger(TypeToClassMap.class);

    private static final List<URL> readResourcesUrls = newLinkedList();

    private TypeToClassMap() {
    }

    //TODO:2015-09-09:mikhail.mikhaylov: Find a way to read it from gradle properties.
    /**
     * File, containing Protobuf messages' typeUrls and their appropriate class names.
     * Is generated with Gradle during build process.
     */
    private static final String PROPERTIES_FILES_PATH = "protos/properties/proto_to_java_class.properties";

    //TODO:2015-09-17:alexander.yevsyukov:  @mikhail.mikhaylov: Have immutable instance here.
    // Transform static methods into inner Builder class
    // that would populate its internal structure and then emits it to be stored in this field.
    private static final Map<TypeName, ClassName> namesMap = Maps.newHashMap();

    static {
        loadClasses();
    }

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

    private static void readPropertiesFromStream(InputStream stream) {
        final Properties properties = new Properties();

        try {
            properties.load(stream);
        } catch (IOException e) {
            //NOP
        }

        readProperties(properties);
    }

    /**
     * Adds all data from properties file into memory. Properties file should contain proto type urls and
     * appropriate java class names.
     *
     * @param properties Properties file to read params from
     */
    private static void readProperties(Properties properties) {
        for (String key : properties.stringPropertyNames()) {
            final TypeName typeName = TypeName.of(key);
            final ClassName className = ClassName.of(properties.getProperty(key));
            namesMap.put(typeName, className);
        }
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

        Enumeration<URL> resources = null;
        try {
            resources = classLoader.getResources(PROPERTIES_FILES_PATH);
        } catch (IOException ignored) {
        }

        if (resources != null) {
            while (resources.hasMoreElements()) {
                final URL resourceUrl = resources.nextElement();
                if (!readResourcesUrls.contains(resourceUrl)) {
                    try {
                        readPropertiesFromStream(resourceUrl.openStream());
                        readResourcesUrls.add(resourceUrl);
                    } catch (IOException ignored) {
                    }
                }
            }
        }

        if (LOG.isInfoEnabled()) {
            LOG.info("Total classes in TypeToClassMap: " + namesMap.size());
        }
    }

}
