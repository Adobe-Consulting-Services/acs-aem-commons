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
.directive("acsCoralToolsHeader", function() {
    
    return {
        restrict: 'A',
        link : function(scope, $element, attrs) {
            var contextPath = attrs.contextPath,
                $nav = $("<nav></nav>").addClass("endor-Crumbs").appendTo($element),
                $slashLink = $("<a></a>").addClass("endor-Crumbs-item").appendTo($nav);

            if (contextPath === undefined) {
                contextPath = "";
            }
            $slashLink.attr("href", contextPath + "/");
            $("<i></i>").addClass("endor-Crumbs-item-icon coral-Icon coral-Icon--adobeExperienceManager coral-Icon--sizeM").appendTo($slashLink);
            $("<a>Tools</a>").addClass("endor-Crumbs-item").attr("href", contextPath + "/miscadmin").appendTo($nav);
            $("<a>" + attrs.title + "</a>").addClass("endor-Crumbs-item").attr("href", contextPath + attrs.pagePath).appendTo($nav);

            $element.addClass("endor-Panel-header endor-BreadcrumbBar is-closed");
        }
    };
});