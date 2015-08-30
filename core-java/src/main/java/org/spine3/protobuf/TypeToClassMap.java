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

import com.google.common.base.Preconditions;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.spine3.ClassName;
import org.spine3.TypeName;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Utility class for reading real proto class names from properties file.
 *
 * @author Mikhail Mikhaylov
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("UtilityClass")
public class TypeToClassMap {

    private static final char CLASS_PACKAGE_DELIMITER = '.';

    /**
     * File, containing Protobuf messages' typeUrls and their appropriate class names.
     * Is generated with Gradle during build process.
     */
    private static final String PROPERTIES_FILE_NAME = "protos.properties";

    private static final Map<TypeName, ClassName> namesMap = new HashMap<>();

    static {
        Properties properties = new Properties();

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream resourceStream = classLoader.getResourceAsStream(PROPERTIES_FILE_NAME);

        Preconditions.checkState(resourceStream != null, "Unable to load resource: " + PROPERTIES_FILE_NAME);
        try {
            properties.load(resourceStream);
        } catch (IOException e) {
            //NOP
        }

        for (String key : properties.stringPropertyNames()) {
            final TypeName typeName = TypeName.of(key);
            final ClassName className = ClassName.of(properties.getProperty(key));
            namesMap.put(typeName, className);
        }
    }

    private TypeToClassMap() {
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

    private static ClassName searchClassNameRecursively(String lookupType, StringBuilder currentSuffix) {
        final int lastDotPosition = lookupType.lastIndexOf(CLASS_PACKAGE_DELIMITER);
        if (lastDotPosition == -1) {
            return null;
        }
        String rootType = lookupType.substring(0, lastDotPosition);
        currentSuffix.insert(0, lookupType.substring(lastDotPosition));
        final TypeName rootTypeName = TypeName.of(rootType);
        if (namesMap.get(rootTypeName) == null) {
            return searchClassNameRecursively(lookupType, currentSuffix);
        }
        return ClassName.of(rootType + currentSuffix);
    }
}
