#!/bin/bash

# script for updating javadoc. assumes that there's a directory named
# adobe-consulting-services.github.io at the same level as the cwd

mvn clean javadoc:aggregate -Pskip-dependency-check
rm -rf ../adobe-consulting-services.github.io/acs-aem-commons/apidocs
cp -R target/site/apidocs ../adobe-consulting-services.github.io/acs-aem-commons/