# Spine model verification tools

This directory contains modules that implement compile time verification of the Spine model.

To enable the verification in a project, apply the following Gradle config to subprojects that may 
contain Spine model elements:
 ```groovy
 buildscript {
     dependencies {
         classpath "io.spine.tools:spine-model-verifier:${spineVersion}"
     }
 }
 
 apply plugin: 'io.spine.tools.spine-model-verifier'
 
 compileJava {
     options.compilerArgs += ["-processor", "io.spine.model.assemble.AssignLookup", "-AspineDirRoot=${rootDir}"]
 }
 
 dependencies {
     compileOnly group: 'io.spine.tools', name: 'spine-model-assembler', version: spineVersion
 }
 ``` 
The Spine model elements are `Entity` classes, `.proto` definitions, etc.
