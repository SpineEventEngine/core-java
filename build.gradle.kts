/*
 * Copyright 2020, TeamDev. All rights reserved.
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

import io.spine.gradle.internal.DependencyResolution
import io.spine.gradle.internal.Deps
import io.spine.gradle.internal.PublishingRepos

buildscript {

    apply(from = "version.gradle.kts")
    apply(from = "$rootDir/config/gradle/dependencies.gradle")

    @Suppress("RemoveRedundantQualifierName") // Cannot use imports here.
    val dependencyResolution = io.spine.gradle.internal.DependencyResolution

    val spineBaseVersion: String by extra
    val spineTimeVersion: String by extra

    dependencyResolution.defaultRepositories(repositories)
    dependencies {
        classpath("io.spine.tools:spine-model-compiler:$spineBaseVersion")
    }

    dependencyResolution.forceConfiguration(configurations)

    configurations.all {
        resolutionStrategy {
            force(
                    "io.spine:spine-base:$spineBaseVersion",
                    "io.spine:spine-time:$spineTimeVersion"
            )
        }
    }
}

plugins {
    java
    idea
    @Suppress("RemoveRedundantQualifierName") // Cannot use imports here.
    id("com.google.protobuf").version(io.spine.gradle.internal.Deps.versions.protobufPlugin)
    @Suppress("RemoveRedundantQualifierName") // Cannot use imports here.
    id("net.ltgt.errorprone").version(io.spine.gradle.internal.Deps.versions.errorPronePlugin)
}

apply(from = "version.gradle.kts")
val spineBaseVersion: String by extra
val spineTimeVersion: String by extra

extra["projectsToPublish"] = listOf(
        "core",
        "client",
        "server",
        "testutil-core",
        "testutil-client",
        "testutil-server",
        "model-assembler",
        "model-verifier"
)
extra["credentialsPropertyFile"] = PublishingRepos.cloudRepo.credentials

allprojects {
    apply {
        plugin("jacoco")
        plugin("idea")
        plugin("project-report")

        from("$rootDir/version.gradle.kts")
    }

    group = "io.spine"
    version = extra["versionToPublish"]!!
}

subprojects {
    
    apply {
        plugin("java-library")
        plugin("com.google.protobuf")
        plugin("net.ltgt.errorprone")
        plugin("pmd")
        plugin("io.spine.tools.spine-model-compiler")

        from(Deps.scripts.javacArgs(project))
        from(Deps.scripts.modelCompiler(project))
        from(Deps.scripts.projectLicenseReport(project))
    }

    val isTravis = System.getenv("TRAVIS") == "true"
    if (isTravis) {
        tasks.javadoc {
            val opt = options
            if (opt is CoreJavadocOptions) {
                opt.addStringOption("Xmaxwarns", "1")
            }
        }
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    DependencyResolution.defaultRepositories(repositories)

    dependencies {
        errorprone(Deps.build.errorProneCore)
        errorproneJavac(Deps.build.errorProneJavac)

        implementation(Deps.build.guava)
        implementation(Deps.build.jsr305Annotations)
        implementation(Deps.build.checkerAnnotations)
        Deps.build.errorProneAnnotations.forEach { implementation(it) }

        testImplementation(Deps.test.guavaTestlib)
        Deps.test.junit5Api.forEach { testImplementation(it) }
        testImplementation(Deps.test.junit5Runner)
        testImplementation("io.spine.tools:spine-mute-logging:$spineBaseVersion")
    }

    DependencyResolution.forceConfiguration(configurations)
    configurations {
        all {
            resolutionStrategy {
                force(
                        "io.spine:spine-base:$spineBaseVersion",
                        "io.spine:spine-time:$spineTimeVersion"
                )
            }
        }
    }
    DependencyResolution.excludeProtobufLite(configurations)

    val sourcesRootDir = "$projectDir/src"
    val generatedRootDir = "$projectDir/generated"
    val generatedJavaDir = "$generatedRootDir/main/java"
    val generatedTestJavaDir = "$generatedRootDir/test/java"
    val generatedGrpcDir = "$generatedRootDir/main/grpc"
    val generatedTestGrpcDir = "$generatedRootDir/test/grpc"
    val generatedSpineDir = "$generatedRootDir/main/spine"
    val generatedTestSpineDir = "$generatedRootDir/test/spine"

    sourceSets {
        main {
            java.srcDirs(generatedJavaDir, "$sourcesRootDir/main/java", generatedSpineDir)
            resources.srcDirs("$sourcesRootDir/main/resources", "$generatedRootDir/main/resources")
            proto.srcDirs("$sourcesRootDir/main/proto")
        }
        test {
            java.srcDirs(generatedTestJavaDir, "$sourcesRootDir/test/java", generatedTestSpineDir)
            resources.srcDirs("$sourcesRootDir/test/resources", "$generatedRootDir/test/resources")
            proto.srcDirs("$sourcesRootDir/test/proto")
        }
    }

    tasks.test {
        useJUnitPlatform {
            includeEngines("junit-jupiter")
        }
    }

    apply {
        from(Deps.scripts.slowTests(project))
        from(Deps.scripts.testOutput(project))
        from(Deps.scripts.javadocOptions(project))
    }

    tasks.register("sourceJar", Jar::class) {
        from(sourceSets.main.get().allJava)
        archiveClassifier.set("sources")
    }

    tasks.register("testOutputJar", Jar::class) {
        from(sourceSets.test.get().allJava)
        archiveClassifier.set("test")
    }

    tasks.register("javadocJar", Jar::class) {
        from("$projectDir/build/docs/javadoc")
        archiveClassifier.set("javadoc")

        dependsOn("javadoc")
    }

    idea {
        module {
            generatedSourceDirs.add(file(generatedJavaDir))
            generatedSourceDirs.add(file(generatedGrpcDir))
            generatedSourceDirs.add(file(generatedSpineDir))
            generatedSourceDirs.add(file(generatedTestJavaDir))
            generatedSourceDirs.add(file(generatedTestGrpcDir))
            generatedSourceDirs.add(file(generatedTestSpineDir))

            testSourceDirs.add(file(generatedTestJavaDir))

            isDownloadJavadoc = true
            isDownloadSources = true
        }
    }

    /**
     * Determines whether this project should expose its Javadoc to `SpineEventEngine.github.io`
     * website.
     *
     * Currently, the `testutil` projects are excluded from publishing, as well as the modules
     * that perform the model compile-time checks.
     *
     * @return `true` is the project Javadoc should be published, `false` otherwise
     */
    fun shouldPublishJavadoc() =
            !project.name.startsWith("testutil") &&
            !project.name.startsWith("model")

    // Apply the Javadoc publishing plugin.
    // This plugin *must* be applied here, not in the module `build.gradle` files.
    //
    if (shouldPublishJavadoc()) {
        apply(from = Deps.scripts.updateGitHubPages(project))
        afterEvaluate {
            tasks.getByName("publish").dependsOn("updateGitHubPages")
        }
    }

    apply(from = Deps.scripts.pmd(project))
}

apply {
    from(Deps.scripts.publish(project))

    // Aggregated coverage report across all subprojects.
    from(Deps.scripts.jacoco(project))
    // Generate a repository-wide report of 3rd-party dependencies and their licenses.
    from(Deps.scripts.repoLicenseReport(project))
    // Generate a `pom.xml` file containing first-level dependency of all projects in the repository.
    from(Deps.scripts.generatePom(project))
}
