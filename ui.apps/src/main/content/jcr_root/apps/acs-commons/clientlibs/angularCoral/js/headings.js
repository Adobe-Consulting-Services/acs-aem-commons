/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
/*global angular: false */
angular.module('acsCoral')
.directive("acsCoralHeading", function() {
    var classesMap = {
            H1 : "coral-Heading coral-Heading--1",
            H2 : "coral-Heading coral-Heading--2",
            H3 : "coral-Heading coral-Heading--3",
            H4 : "coral-Heading coral-Heading--4",
            H5 : "coral-Heading coral-Heading--5"
    };

    return {
        restrict: 'A',
        link : function(scope, $element, attrs) {
            var tagName = $element.prop("tagName");
            $element.addClass(classesMap[tagName]);
        }
    };
});