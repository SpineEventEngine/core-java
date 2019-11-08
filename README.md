# Welcome to Spine Event Engine
[![Build Status](https://travis-ci.com/SpineEventEngine/core-java.svg?branch=master)](https://travis-ci.com/SpineEventEngine/core-java) &nbsp;
[![codecov.io](https://codecov.io/github/SpineEventEngine/core-java/coverage.svg?branch=master)](https://codecov.io/github/SpineEventEngine/core-java?branch=master) &nbsp;
[![license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)
[![Gitter chat](https://badges.gitter.im/SpineEventEngine.png)](https://gitter.im/SpineEventEngine)

Spine Event Engine is a Java framework for building Event Sourcing and CQRS applications that are accessed by
clients built with JavaScript, Java Nano (Android), C++, and Java.

Requires Java 8 or higher.

Gradle is used as a build and dependency management system. 

## Releases
The project is under active ongoing development. You are welcome to experiment and [provide your feedback][email-developers].

The latest stable version is [1.2.0][latest-release].  

## Quickstart and Examples

There is a [template][server-quickstart] for a server application built in Spine. 

To add Spine into an existing project consider using [Spine Bootstrap Gradle plugin](https://github.com/SpineEventEngine/bootstrap/).

The introductory application examples are available from the [Spine Examples][spine-examples] GitHub organization.

For more advanced example including a Spine server, a console client, and a web application, see [ToDo List app][todo-list].

## Building from Sources

At the moment Spine modules require JDK 8 at build-time. 

The support of recent JDK versions is planned for future releases.

## Links
* [Getting Started with Spine in Java](https://spine.io/docs/quickstart/java.html)
* [The framework site][spine-site]

If you plan to contribute to the project please visit these pages:
* [Wiki home][wiki-home]
* [Java Code Style][java-code-style]

## Important Warnings
* The code annotated with `@Internal` are not parts of public API of the framework, therefore should
not be used from outside of the framework.
* The public API marked as `@Experimental` may be used at own risk; it can change at any time, 
and has no guarantee of API stability or backward-compatibility.
* The API annotated with `@SPI` is for those who intend to extend the framework, 
or provide custom storage implementations. 

If you need to use API with one of these annotations, please [contact us][email-developers].

[email-developers]: mailto:spine-developers@teamdev.com
[latest-release]: https://github.com/SpineEventEngine/core-java/releases/tag/v1.2.0-release
[spine-site]: https://spine.io/
[wiki-home]: https://github.com/SpineEventEngine/core-java/wiki
[java-code-style]: https://github.com/SpineEventEngine/core-java/wiki/Java-Code-Style 
[getting-started]: https://github.com/SpineEventEngine/documentation/blob/master/getting-started/index.md
[server-quickstart]: https://github.com/spine-examples/server-quickstart
[spine-examples]: https://github.com/spine-examples
[todo-list]: https://github.com/spine-examples/todo-list
