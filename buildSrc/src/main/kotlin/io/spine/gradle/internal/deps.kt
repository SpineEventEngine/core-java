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

package io.spine.gradle.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.dsl.RepositoryHandler
import java.net.URI

/*
 * This file describes shared dependencies of Spine sub-projects.
 *
 * Inspired by dependency management of the Uber's NullAway project:
 *  https://github.com/uber/NullAway/blob/master/gradle/dependencies.gradle
 */

data class Repository(val releases: String,
                      val snapshots: String,
                      val credentials: String)

/**
 * Repositories to which we may publish. Normally, only one repository will be used.
 *
 * See `publish.gradle` for details of the publishing process.
 */
object PublishingRepos {
    val mavenTeamDev = Repository(
            releases = "http://maven.teamdev.com/repository/spine",
            snapshots = "http://maven.teamdev.com/repository/spine-snapshots",
            credentials = "credentials.properties"
    )
    val cloudRepo = Repository(
            releases = "https://spine.mycloudrepo.io/public/repositories/releases",
            snapshots = "https://spine.mycloudrepo.io/public/repositories/snapshots",
            credentials = "cloudrepo.properties"
    )
}

// Specific repositories.
object Repos {
    val oldSpine: String = PublishingRepos.mavenTeamDev.releases
    val oldSpineSnapshots: String = PublishingRepos.mavenTeamDev.snapshots

    val spine: String = PublishingRepos.cloudRepo.releases
    val spineSnapshots: String = PublishingRepos.cloudRepo.snapshots

    val sonatypeSnapshots: String = "https://oss.sonatype.org/content/repositories/snapshots"
    val gradlePlugins = "https://plugins.gradle.org/m2/"
}

object Versions {
    val checkerFramework = "3.3.0"
    val errorProne       = "2.3.4"
    val errorProneJavac  = "9+181-r4173-1" // taken from here: https://github.com/tbroyer/gradle-errorprone-plugin/blob/v0.8/build.gradle.kts
    val errorPronePlugin = "1.2.1"
    val pmd              = "6.24.0"
    val checkstyle       = "8.29"
    val protobufPlugin   = "0.8.12"
    val appengineApi     = "1.9.79"
    val appenginePlugin  = "2.2.0"
    val findBugs         = "3.0.2"
    val guava            = "29.0-jre"
    val protobuf         = "3.11.4"
    val grpc             = "1.28.1"
    val flogger          = "0.5.1"
    val junit4           = "4.12"
    val junit5           = "5.6.2"
    val junitPlatform    = "1.6.2"
    val junitPioneer     = "0.4.2"
    val truth            = "1.0.1"
    val httpClient       = "1.34.2"
    val apacheHttpClient = "2.1.2"
    val firebaseAdmin    = "6.12.2"
    val roaster          = "2.21.2.Final"
    val licensePlugin    = "1.13"
    val javaPoet         = "1.12.1"
    val autoService      = "1.0-rc6"
    val autoCommon       = "0.10"
    val jackson          = "2.9.10.5"
    val animalSniffer    = "1.18"
    val apiguardian      = "1.1.0"
    val javaxAnnotation  = "1.3.2"
    val klaxon           = "5.4"
    val ouathJwt         = "3.10.3"
    val bouncyCastlePkcs = "1.66"
    val assertK          = "0.22"

    /**
     * Version of the SLF4J library.
     *
     * Spine used to log with SLF4J. Now we use Flogger. Whenever a choice comes up, we recommend to
     * use the latter.
     *
     * Some third-party libraries may clash with different versions of the library. Thus, we specify
     * this version and force it via [forceConfiguration(..)][DependencyResolution.forceConfiguration].
     */
    @Deprecated("Use Flogger over SLF4J.", replaceWith = ReplaceWith("flogger"))
    val slf4j            = "1.7.29"
}

object GradlePlugins {
    val errorProne      = "net.ltgt.gradle:gradle-errorprone-plugin:${Versions.errorPronePlugin}"
    val protobuf        = "com.google.protobuf:protobuf-gradle-plugin:${Versions.protobufPlugin}"
    val appengine       = "com.google.cloud.tools:appengine-gradle-plugin:${Versions.appenginePlugin}"
    val licenseReport   = "com.github.jk1:gradle-license-report:${Versions.licensePlugin}"
}

object Build {
    val errorProneJavac        = "com.google.errorprone:javac:${Versions.errorProneJavac}"
    val errorProneAnnotations = listOf(
            "com.google.errorprone:error_prone_annotations:${Versions.errorProne}",
            "com.google.errorprone:error_prone_type_annotations:${Versions.errorProne}"
    )
    val errorProneCheckApi     = "com.google.errorprone:error_prone_check_api:${Versions.errorProne}"
    val errorProneCore         = "com.google.errorprone:error_prone_core:${Versions.errorProne}"
    val errorProneTestHelpers  = "com.google.errorprone:error_prone_test_helpers:${Versions.errorProne}"
    val checkerAnnotations     = "org.checkerframework:checker-qual:${Versions.checkerFramework}"
    val checkerDataflow        = listOf(
            "org.checkerframework:dataflow:${Versions.checkerFramework}",
            "org.checkerframework:javacutil:${Versions.checkerFramework}"
    )
    val autoCommon             = "com.google.auto:auto-common:${Versions.autoCommon}"
    val autoService            = AutoService
    val jsr305Annotations      = "com.google.code.findbugs:jsr305:${Versions.findBugs}"
    val guava                  = "com.google.guava:guava:${Versions.guava}"
    val flogger                = "com.google.flogger:flogger:${Versions.flogger}"
    val protobuf = listOf(
            "com.google.protobuf:protobuf-java:${Versions.protobuf}",
            "com.google.protobuf:protobuf-java-util:${Versions.protobuf}"
    )
    val protoc                 = "com.google.protobuf:protoc:${Versions.protobuf}"
    val googleHttpClient       = "com.google.http-client:google-http-client:${Versions.httpClient}"
    val googleHttpClientApache = "com.google.http-client:google-http-client-apache:${Versions.apacheHttpClient}"
    val appengineApi           = "com.google.appengine:appengine-api-1.0-sdk:${Versions.appengineApi}"
    val firebaseAdmin          = "com.google.firebase:firebase-admin:${Versions.firebaseAdmin}"
    val jacksonDatabind        = "com.fasterxml.jackson.core:jackson-databind:${Versions.jackson}"
    val roasterApi             = "org.jboss.forge.roaster:roaster-api:${Versions.roaster}"
    val roasterJdt             = "org.jboss.forge.roaster:roaster-jdt:${Versions.roaster}"
    val animalSniffer          = "org.codehaus.mojo:animal-sniffer-annotations:${Versions.animalSniffer}"
    val ci = "true".equals(System.getenv("CI"))
    val gradlePlugins = GradlePlugins
    @Deprecated("Use Flogger over SLF4J.", replaceWith = ReplaceWith("flogger"))
    @Suppress("DEPRECATION") // Version of SLF4J.
    val slf4j                  = "org.slf4j:slf4j-api:${Versions.slf4j}"

    object AutoService {
        val annotations = "com.google.auto.service:auto-service-annotations:${Versions.autoService}"
        val processor   = "com.google.auto.service:auto-service:${Versions.autoService}"
    }
}

object Gen {
    val javaPoet        = "com.squareup:javapoet:${Versions.javaPoet}"
    val javaxAnnotation = "javax.annotation:javax.annotation-api:${Versions.javaxAnnotation}"
}

object Publishing {
    val klaxon           = "com.beust:klaxon:${Versions.klaxon}"
    val oauthJwt         = "com.auth0:java-jwt:${Versions.ouathJwt}"
    val bouncyCastlePkcs = "org.bouncycastle:bcpkix-jdk15on:${Versions.bouncyCastlePkcs}"
    val assertK          = "com.willowtreeapps.assertk:assertk-jvm:${Versions.assertK}"
}

object Grpc {
    val core        = "io.grpc:grpc-core:${Versions.grpc}"
    val stub        = "io.grpc:grpc-stub:${Versions.grpc}"
    val okHttp      = "io.grpc:grpc-okhttp:${Versions.grpc}"
    val protobuf    = "io.grpc:grpc-protobuf:${Versions.grpc}"
    val netty       = "io.grpc:grpc-netty:${Versions.grpc}"
    val nettyShaded = "io.grpc:grpc-netty-shaded:${Versions.grpc}"
    val context     = "io.grpc:grpc-context:${Versions.grpc}"

    @Deprecated("Use the shorter form.", replaceWith = ReplaceWith("core"))
    val grpcCore = core
    @Deprecated("Use the shorter form.", replaceWith = ReplaceWith("stub"))
    val grpcStub = stub
    @Deprecated("Use the shorter form.", replaceWith = ReplaceWith("okHttp"))
    val grpcOkHttp = okHttp
    @Deprecated("Use the shorter form.", replaceWith = ReplaceWith("protobuf"))
    val grpcProtobuf = protobuf
    @Deprecated("Use the shorter form.", replaceWith = ReplaceWith("netty"))
    val grpcNetty = netty
    @Deprecated("Use the shorter form.", replaceWith = ReplaceWith("nettyShaded"))
    val grpcNettyShaded = nettyShaded
    @Deprecated("Use the shorter form.", replaceWith = ReplaceWith("context"))
    val grpcContext = context
}

object Runtime {

    val flogger = Flogger

    object Flogger {
        val systemBackend = "com.google.flogger:flogger-system-backend:${Versions.flogger}"
        val log4J         = "com.google.flogger:flogger-log4j:${Versions.flogger}"
        val slf4J         = "com.google.flogger:slf4j-backend-factory:${Versions.flogger}"
    }

    @Deprecated("Use the `flogger` object.", replaceWith = ReplaceWith("flogger.systemBackend"))
    val floggerSystemBackend = flogger.systemBackend
    @Deprecated("Use the `flogger` object.", replaceWith = ReplaceWith("flogger.log4J"))
    val floggerLog4J = flogger.log4J
    @Deprecated("Use the `flogger` object.", replaceWith = ReplaceWith("flogger.slf4J"))
    val floggerSlf4J = flogger.slf4J
}

object Test {
    val junit4        = "junit:junit:${Versions.junit4}"
    val junit5Api = listOf(
            "org.junit.jupiter:junit-jupiter-api:${Versions.junit5}",
            "org.junit.jupiter:junit-jupiter-params:${Versions.junit5}",
            "org.apiguardian:apiguardian-api:${Versions.apiguardian}"
    )
    val junit5Runner  = "org.junit.jupiter:junit-jupiter-engine:${Versions.junit5}"
    val junitPioneer  = "org.junit-pioneer:junit-pioneer:${Versions.junitPioneer}"
    val guavaTestlib  = "com.google.guava:guava-testlib:${Versions.guava}"
    val mockito       = "org.mockito:mockito-core:2.12.0"
    val hamcrest      = "org.hamcrest:hamcrest-all:1.3"
    val truth = listOf(
            "com.google.truth:truth:${Versions.truth}",
            "com.google.truth.extensions:truth-java8-extension:${Versions.truth}",
            "com.google.truth.extensions:truth-proto-extension:${Versions.truth}"
    )
    @Deprecated("Use Flogger over SLF4J.",
            replaceWith = ReplaceWith("Deps.runtime.floggerSystemBackend"))
    @Suppress("DEPRECATION") // Version of SLF4J.
    val slf4j         = "org.slf4j:slf4j-jdk14:${Versions.slf4j}"
}

object Scripts {

    private const val COMMON_PATH = "/config/gradle/"

    fun testArtifacts(p: Project)          = p.script("test-artifacts.gradle")
    fun testOutput(p: Project)             = p.script("test-output.gradle")
    fun slowTests(p: Project)              = p.script("slow-tests.gradle")
    fun javadocOptions(p: Project)         = p.script("javadoc-options.gradle")
    fun filterInternalJavadocs(p: Project) = p.script("filter-internal-javadoc.gradle")
    fun jacoco(p: Project)                 = p.script("jacoco.gradle")
    fun publish(p: Project)                = p.script("publish.gradle")
    fun publishProto(p: Project)           = p.script("publish-proto.gradle")
    fun javacArgs(p: Project)              = p.script("javac-args.gradle")
    fun jsBuildTasks(p: Project)           = p.script("js/build-tasks.gradle")
    fun jsConfigureProto(p: Project)       = p.script("js/configure-proto.gradle")
    fun npmPublishTasks(p: Project)        = p.script("js/npm-publish-tasks.gradle")
    fun npmCli(p: Project)                 = p.script("js/npm-cli.gradle")
    fun updatePackageVersion(p: Project)   = p.script("js/update-package-version.gradle")
    fun dartBuildTasks(p: Project)         = p.script("dart/build-tasks.gradle")
    fun pubPublishTasks(p: Project)        = p.script("dart/pub-publish-tasks.gradle")
    fun pmd(p: Project)                    = p.script("pmd.gradle")
    fun checkstyle(p: Project)             = p.script("checkstyle.gradle")
    fun runBuild(p: Project)               = p.script("run-build.gradle")
    fun modelCompiler(p: Project)          = p.script("model-compiler.gradle")
    fun licenseReportCommon(p: Project)    = p.script("license-report-common.gradle")
    fun projectLicenseReport(p: Project)   = p.script("license-report-project.gradle")
    fun repoLicenseReport(p: Project)      = p.script("license-report-repo.gradle")
    fun generatePom(p: Project)            = p.script("generate-pom.gradle")
    fun updateGitHubPages(p: Project)      = p.script("update-gh-pages.gradle")

    private fun Project.script(name: String) = "${rootDir}$COMMON_PATH$name"
}

object Deps {
    val build = Build
    val grpc = Grpc
    val gen = Gen
    val runtime = Runtime
    val test = Test
    val versions = Versions
    val scripts = Scripts
    val publishing = Publishing
}

object DependencyResolution {

    fun forceConfiguration(configurations: ConfigurationContainer) {
        configurations.all {
            resolutionStrategy {
                failOnVersionConflict()
                cacheChangingModulesFor(0, "seconds")
                @Suppress("DEPRECATION") // Force SLF4J version.
                force(
                        Deps.build.slf4j,
                        Deps.build.errorProneAnnotations,
                        Deps.build.jsr305Annotations,
                        Deps.build.checkerAnnotations,
                        Deps.build.autoCommon,
                        Deps.build.guava,
                        Deps.build.animalSniffer,
                        Deps.build.protobuf,
                        Deps.test.guavaTestlib,
                        Deps.test.truth,
                        Deps.test.junit5Api,
                        Deps.test.junit4,

                        // Transitive dependencies of 3rd party components that we don't use directly.
                        "com.google.code.gson:gson:2.8.6",
                        "com.google.j2objc:j2objc-annotations:1.3",
                        "org.codehaus.plexus:plexus-utils:3.3.0",
                        "com.squareup.okio:okio:1.17.5", // Last version before next major.
                        "commons-cli:commons-cli:1.4",

                        // Force discontinued transitive dependency until everybody migrates off it.
                        "org.checkerframework:checker-compat-qual:2.5.5",

                        "commons-logging:commons-logging:1.2",

                        // Force the Gradle Protobuf plugin version.
                        Deps.build.gradlePlugins.protobuf
                )
            }
        }
    }

    fun excludeProtobufLite(configurations: ConfigurationContainer) {
        excludeProtoLite(configurations, "runtime")
        excludeProtoLite(configurations, "testRuntime")
    }

    private fun excludeProtoLite(configurations: ConfigurationContainer,
                                 configurationName: String) {
        configurations.named(configurationName).get()
                .exclude(mapOf("group" to "com.google.protobuf", "module" to "protobuf-lite"))
    }

    fun defaultRepositories(repositories: RepositoryHandler) {
        repositories.mavenLocal()
        repositories.maven {
            url = URI(Repos.spine)
            content {
                includeGroup("io.spine")
                includeGroup("io.spine.tools")
                includeGroup("io.spine.gcloud")
            }
        }
        repositories.jcenter()
        repositories.maven {
            url = URI(Repos.gradlePlugins)
        }
    }
}
