# Welcome to Spine Event Engine
 [![Build Status](https://travis-ci.com/SpineEventEngine/core-java.svg?branch=master)](https://travis-ci.com/SpineEventEngine/core-java) &nbsp;
 [![codecov.io](https://codecov.io/github/SpineEventEngine/core-java/coverage.svg?branch=master)](https://codecov.io/github/SpineEventEngine/core-java?branch=master) &nbsp;
 [![Codacy Badge](https://api.codacy.com/project/badge/Grade/dc09a913cbe544dba54a21116d3f5fc7)](https://www.codacy.com/app/SpineEventEngine/core-java?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=SpineEventEngine/core-java&amp;utm_campaign=Badge_Grade) &nbsp;
[![license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)

Spine Event Engine is a Java framework for building Event Sourcing and CQRS applications that are accessed by
clients built with JavaScript, Java Nano (Android), Objective-C, and Java.

Requires Java 8 or higher.

## Pre-release
The project is under active ongoing development. At this stage, we do not recommend using the framework for production purposes.
You are welcome to experiment and [provide your feedback][email-developers].

The latest stable version is [0.10.0][latest-release].

Please track our release announcement to be informed about the production version (1.0.0) release.  

## Gradle project dependencies

In order to add Spine to your project, please add the following code to your `build.gradle`:

```groovy
buildscript{
    ext {
        spineVersion = '0.10.0'
        spineModelCompilerVersion = '0.9.41-SNAPSHOT'

        protobufGradlePluginVersion = '0.8.3'

        spineRepository = 'http://maven.teamdev.com/repository/spine'
        spineSnapshotsRepository = 'http://maven.teamdev.com/repository/spine-snapshots'
    }
    
    // Repositories for plug-ins.
    repositories {
        jcenter()

        // Release repository
        maven { url = spineRepository }
        
        // Snapshots repository
        maven { url = spineSnapshotsRepository }
    }
        
    dependencies {
        // ...
        classpath group: 'com.google.protobuf', name:'protobuf-gradle-plugin', version: protobufGradlePluginVersion        
        classpath group: 'io.spine.tools', name: 'spine-model-compiler', version: spineModelCompilerVersion

    }
}

apply plugin: 'java'
apply plugin: 'com.google.protobuf'
apply plugin: 'io.spine.tools.spine-model-compiler'

repositories {
    jcenter()

    maven { url = spineRepository }
    maven { url = spineSnapshotsRepository }
}

dependencies {
        
    // ...
    
    // Client-side and shared API. 
    compile group: 'io.spine', name: 'spine-client', version: spineVersion
    
    // Add this only for server-side code. 
    compile group: 'io.spine', name: 'spine-server', version: spineVersion
}
```
There is no Maven support at the moment. 

## Links
* [Getting Started Guide][getting-started]
* [Documentation in GitBook][spine-git-book]
* [The framework site][spine-site]

If you plan to contribute to the project please visit these pages:
* [Java Code Style][java-code-style]
* [Wiki home][wiki-home]

## Important Warnings
* The code annotated with `@Internal` are not parts of public API of the framework, therefore should
not be used from outside of the framework.
* The public API marked as `@Experimental` may be used at own risk; it can change at any time, 
and has no guarantee of API stability or backward-compatibility.
* The API annotated with `@SPI` is for those who intend to extend the framework, 
or provide custom storage implementations. 

If you need to use API with one of these annotations, please [contact us][email-developers].

[email-developers]: mailto:spine-developers@teamdev.com
[latest-release]: https://github.com/SpineEventEngine/core-java/releases/tag/0.10.0 
[spine-site]: https://spine.io/
[spine-git-book]: https://docs.spine3.org/
[wiki-home]: https://github.com/SpineEventEngine/core-java/wiki
[java-code-style]: https://github.com/SpineEventEngine/core-java/wiki/Java-Code-Style 
[getting-started]: https://github.com/SpineEventEngine/documentation/blob/master/getting-started/index.md
