# ACS AEM Commons

[![Maven Central](https://img.shields.io/maven-central/v/com.adobe.acs/acs-aem-commons-all)](https://central.sonatype.com/artifact/com.adobe.acs/acs-aem-commons-all)
[![Join the chat at https://gitter.im/Adobe-Consulting-Services/acs-aem-commons](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/Adobe-Consulting-Services/acs-aem-commons?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Build Status](https://github.com/Adobe-Consulting-Services/acs-aem-commons/actions/workflows/maven.yml/badge.svg)](https://github.com/Adobe-Consulting-Services/acs-aem-commons/actions)
[![Code Climate Issues](https://img.shields.io/codeclimate/issues/Adobe-Consulting-Services/acs-aem-commons.svg)](https://codeclimate.com/github/Adobe-Consulting-Services/acs-aem-commons)
[![codecov](https://codecov.io/gh/Adobe-Consulting-Services/acs-aem-commons/branch/master/graph/badge.svg?token=KkCffH5xs4)](https://codecov.io/gh/Adobe-Consulting-Services/acs-aem-commons)
[![Maintainability](https://api.codeclimate.com/v1/badges/a1038e3e7f9c90dcaaa6/maintainability)](https://codeclimate.com/github/Adobe-Consulting-Services/acs-aem-commons/maintainability)

This project is a unified collection of AEM/CQ code generated by the AEM consulting practice.

## Building

This project uses Maven for building. Common commands:

From the root directory, run `mvn -PautoInstallPackage clean install` to build the bundle and content package and install to an AEM instance.

From the bundle directory, run `mvn -PautoInstallBundle clean install` to build *just* the bundle and install to a CQ instance.

The Maven profile `cloud` is used to both build and deploy the version targeted for AEMaaCS. To build and deploy the version for AEM 6.5.x use the command line option `-P \!cloud` [to deactivate the profile `cloud`](https://maven.apache.org/guides/introduction/introduction-to-profiles.html#deactivating-a-profile).

## Using with VLT

To use vlt with this project, first build and install the package to your local AEM instance as described above. Then cd to `content/src/main/content/jcr_root` and run

    vlt --credentials admin:admin checkout -f ../META-INF/vault/filter.xml --force http://localhost:4502/crx

Once the working copy is created, you can use the normal ``vlt up`` and ``vlt ci`` commands.

## Specifying AEM Host/Port

The AEM host and port can be specified on the command line with:
mvn -Dcrx.host=otherhost -Dcrx.port=5502 <goals>

## Distribution

Watch this space.

## Rules

* Spaces, not tabs.
* Provide documentation in the parent org GH project: https://github.com/Adobe-Consulting-Services/adobe-consulting-services.github.io
* Target AEM as a Cloud Service. AEM 6.5.x only features can still be contributed, but please create a Feature Review Github Issue before working on them to ensure they will accepted. Check the [compatibility table](http://adobe-consulting-services.github.io/acs-aem-commons/pages/compatibility.html) for compatibility of older versions.
* API classes and interfaces must have JavaDocs. Not necessary for implementation classes.
* Don't use author tags. This is a community project.

## Want commit rights?

* Create an issue.
