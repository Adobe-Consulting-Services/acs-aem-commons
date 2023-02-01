#!/bin/bash

#
# ACS AEM Commons
#
# Copyright (C) 2013 - 2023 Adobe
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# script for updating javadoc. assumes that there's a directory named
# adobe-consulting-services.github.io at the same level as the cwd

mvn clean javadoc:aggregate -Pskip-dependency-check
rm -rf ../adobe-consulting-services.github.io/acs-aem-commons/apidocs
cp -R target/site/apidocs ../adobe-consulting-services.github.io/acs-aem-commons/