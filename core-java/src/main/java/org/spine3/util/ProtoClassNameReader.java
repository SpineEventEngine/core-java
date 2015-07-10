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

package org.spine3.util;

import com.google.protobuf.Any;
import com.google.protobuf.Message;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Utility class for reading real proto class names from properties file.
 *
 * @author Mikhail Mikhaylov
 */
public class ProtoClassNameReader {

    private static final String PROPERTIES_FILE_NAME = "protos.properties";

    //todo:2015-07-10:mikhail.mikhaylov: Is static context fine here?
    private static final Map<String, String> namesMap = new HashMap<String, String>();
    private static Properties properties;

    /**
     * Retrieves compiled proto's java class name by proto type url
     * to be used to parse {@link Message} from {@link Any}.
     *
     * @param protoType {@link Any} type url.
     * @return java class name.
     */
    public static String getClassNameByProtoTypeUrl(String protoType) {
        if (!namesMap.containsKey(protoType)) {
            if (properties == null) {
                properties = new Properties();
            }
            try {
                properties.load(Thread.currentThread()
                        .getContextClassLoader().getResourceAsStream(PROPERTIES_FILE_NAME));
            } catch (IOException e) {
                //todo:2015-07-10:mikhail.mikhaylov: This should never happen, but we should handle this.
            }
            namesMap.put(protoType, properties.getProperty(protoType));
        }
        return namesMap.get(protoType);
    }
}
