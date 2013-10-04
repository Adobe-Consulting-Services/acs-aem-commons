#!/bin/bash

# script for updating javadoc. assumes that there's a directory named
# acs-aem-commons-gh-pages at the same level as the cwd

mvn clean javadoc:javadoc
cp -R bundle/target/site/apidocs ../acs-aem-commons-gh-pages/