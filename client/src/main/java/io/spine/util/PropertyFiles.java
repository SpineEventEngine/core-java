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

package io.spine.util;

import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.IoUtil.close;

/**
 * Utilities for working with property files.
 *
 * @author Alexander Litus
 * @author Alexander Yevsyukov
 */
public class PropertyFiles {

    private PropertyFiles() {
        // Prevent instantiation of this utility class.
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

    /**
     * Loads all data from {@code .properties} file(s) into memory.
     *
     * <p>Logs {@link IOException} if it occurs.
     *
     * @param propsFilePath the path of the {@code .properties} file to load
     */
    public static ImmutableSet<Properties> loadAllProperties(String propsFilePath) {
        checkNotNull(propsFilePath);

        final ImmutableSet.Builder<Properties> result = ImmutableSet.builder();
        final Enumeration<URL> resources = getResources(propsFilePath);
        if (resources == null) {
            return result.build();
        }
        while (resources.hasMoreElements()) {
            final URL resourceUrl = resources.nextElement();
            final Properties properties = loadPropertiesFile(resourceUrl);
            result.add(properties);
        }
        return result.build();
    }

    private static Enumeration<URL> getResources(String propsFilePath) {
        final ClassLoader classLoader = getContextClassLoader();
        Enumeration<URL> resources = null;
        try {
            resources = classLoader.getResources(propsFilePath);
        } catch (IOException e) {
            if (log().isWarnEnabled()) {
                log().warn("Failed to load resources: " + propsFilePath, e);
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
                log().warn("Failed to load properties file from: " + resourceUrl, e);
            }
        } finally {
            close(inputStream);
        }
        return properties;
    }

    private static ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(PropertyFiles.class);
    }
}
