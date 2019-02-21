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

package io.spine.model.verify;

import com.google.common.io.Files;
import io.spine.model.verify.ProjectClassLoader.GetDestinationDir;
import org.gradle.api.Project;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Function;

import static io.spine.tools.gradle.TaskName.COMPILE_JAVA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

@DisplayName("ProjectClassLoader should")
class ProjectClassLoaderTest {

    @Test
    @DisplayName("retrieve class loader for a Gradle project")
    void retrieveClassLoader() {
        ProjectClassLoader loader = new ProjectClassLoader(project());
        ClassLoader classLoader = loader.get();
        assertNotNull(classLoader);
    }

    @Test
    @DisplayName("retrieve compilation destination directory from task")
    void getCompilationDestDir() throws MalformedURLException {
        JavaCompile compileTask = project().getTasks()
                                           .withType(JavaCompile.class)
                                           .getByName(COMPILE_JAVA.getValue());
        File dest = Files.createTempDir();
        compileTask.setDestinationDir(dest);
        Function<JavaCompile, URL> func = GetDestinationDir.FUNCTION;
        URL destUrl = dest.toURI()
                          .toURL();
        assertEquals(destUrl, func.apply(compileTask));
    }

    @Test
    @DisplayName("retrieve null if destination directory is null")
    void getNullDestDir() {
        JavaCompile compileTask = mock(JavaCompile.class);
        Function<JavaCompile, URL> func = GetDestinationDir.FUNCTION;
        assertNull(func.apply(compileTask));
    }

    private static Project project() {
        Project result = ProjectBuilder.builder()
                                       .build();
        result.getPluginManager()
              .apply("java");
        return result;
    }
}
