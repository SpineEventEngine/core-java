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

package org.spine3.gradle

import org.gradle.api.*

class ProtoLookupPlugin implements Plugin<Project> {

    //TODO:2015-09-09:mikhail.mikhaylov: Find a way to read it from gradle properties.
    static final String PROPERTIES_PATH_SUFFIX = "resources/protos/properties";

    static final String CURRENT_PATH = new File(".").getCanonicalPath();

    @Override
    void apply(Project target) {

        String javaSuffix = ".java"
        String orBuilderSuffix = "OrBuilder" + javaSuffix

        for (String rootDirPath : ["generated/main", "generated/test"]) {
            final String srcFolder = rootDirPath + "/java";

            File rootDir = new File(srcFolder)
            if (!rootDir.exists()) {
                return
            }

            File propsFileFolder = new File("generated/main/" + PROPERTIES_PATH_SUFFIX)
            if (!propsFileFolder.exists()) {
                propsFileFolder.mkdirs();
            }
            Properties props = new Properties() {
                @Override
                public synchronized Enumeration<Object> keys() {
                    return Collections.enumeration(new TreeSet<Object>(super.keySet()));
                }
            };
            File propsFile = null;
            try {
                propsFile = new File(getProtoPropertiesFilePath(rootDirPath))
            } catch (FileNotFoundException ignored) {
            }
            if (propsFile.exists()) {
                props.load(propsFile.newDataInputStream())
                // as Properties API does not support saving default table values, we have to rewrite them all
                // Probably we should use Apache property API
                Set<String> names = props.stringPropertyNames();
                for (Iterator<String> i = names.iterator(); i.hasNext();) {
                    String propName = i.next();
                    props.setProperty(propName, props.getProperty(propName));
                }
            } else {
                propsFile.parentFile.mkdirs();
                propsFile.createNewFile();
            }
            rootDir.listFiles().each {
                String prefixName = it.name
                target.fileTree(it).each {
                    if (it.name.endsWith(javaSuffix) && !it.name.endsWith(orBuilderSuffix)) {
                        String protoPath = it.path.substring((target.projectDir.absolutePath
                                + srcFolder + prefixName).length() + 3)
                        protoPath = protoPath.substring(0, protoPath.length() - javaSuffix.length())
                        protoPath = replaceFileSeparatorWithDot(protoPath)
                        String className = replaceFileSeparatorWithDot(prefixName) + "." + protoPath

                        // 'Spine3' is the abbreviation for Spine Event Engine.
                        // We have 'org.spine3' package name for Java because other 'spine' in 'org' or 'io'
                        // were occupied.
                        // We have 'spine' package for Protobuf (without '3') because it reads better.
                        String protoType = protoPath.replace("spine3", "spine")

                        props.setProperty(protoType, className)
                    }
                }
            }
            BufferedWriter writer = propsFile.newWriter();
            props.store(writer, null)
            writer.close()
        }
    }

    static String getProtoPropertiesFilePath(String rootDirPath) {
        return CURRENT_PATH + "/" + rootDirPath + "/" + PROPERTIES_PATH_SUFFIX + "/" +
                "proto.properties";
    }

    static String replaceFileSeparatorWithDot(String filePath) {
        return filePath.replace((char) File.separatorChar, (char) '.');
    }
}
