# Welcome to Spine Event Engine
[![Build Status](https://travis-ci.com/SpineEventEngine/core-java.svg?branch=master)](https://travis-ci.com/SpineEventEngine/core-java) &nbsp;
[![codecov.io](https://codecov.io/github/SpineEventEngine/core-java/coverage.svg?branch=master)](https://codecov.io/github/SpineEventEngine/core-java?branch=master) &nbsp;
[![license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)
[![Gitter chat](https://badges.gitter.im/SpineEventEngine.png)](https://gitter.im/SpineEventEngine)

[Spine Event Engine][spine-site] is a Java framework for building Event Sourcing and CQRS
applications that are accessed by clients built with JavaScript, Java Nano (Android), Dart, and Java.

## Releases

The project is under active ongoing development. 
You are welcome to experiment and [provide your feedback][email-developers].

The latest stable version is [1.7.0][latest-release].

## Contents

This repository contains the code of:
 - core types;
 - client API;
 - server API;
 - testing utilities for the client- and server-side code.
  

## Quick start and examples

Please see the [“Quick Start” guide][quick-start] which goes through a Hello World project showing
how to create a Spine-based project.
 
More introductory application examples are available from
the [Spine Examples][spine-examples] GitHub organization.

## Building from sources

Building Spine modules and libraries requires JDK 8. 

The support of JDK 11 is planned for the [version 3.0][v3] of the framework.

Gradle is used as a build and dependency management system.

This repository uses [configuration files][config] shared across several Spine libs. They are
plugged in as a Git sub-module. Please use the following command to initialize it:

```sh
git submodule update --init --recursive
```  

Also, a `pull` script is located in the root of `core-java` repository. Use it to update to the 
latest version of the configuration files.

## Important warnings
* The code annotated with `@Internal` are not parts of public API of the framework, therefore should
  not be used from outside of the framework.

* The public API marked as `@Experimental` may be used at own risk; it can change at any time, 
  and has no guarantee of API stability or backward-compatibility.

* The API annotated with `@SPI` is for those who intend to extend the framework, 
  or provide custom storage implementations. 

If you need to use API with one of these annotations, please [contact us][email-developers].

[email-developers]: mailto:developers@spine.io
[latest-release]: https://github.com/SpineEventEngine/core-java/releases/tag/v1.7.0
[spine-site]: https://spine.io/
[quick-start]: https://spine.io/docs/quick-start
[spine-examples]: https://github.com/spine-examples
[todo-list]: https://github.com/spine-examples/todo-list
[v3]: https://github.com/orgs/SpineEventEngine/projects/11
[config]: https://github.com/SpineEventEngine/config/
