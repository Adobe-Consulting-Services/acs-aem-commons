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

/*global quickly: false, angular: false, _: false, console: false */

quickly.directive('actionForm', ['$rootScope', '$q', '$timeout', function ($rootScope, $q, $timeout) {
    return {
        restrict: 'E',
        scope: {
            complete: '&',
            action: '=data'
        },
        template: '<form ng-hide="true"></form>',
        replace: true,
        link: function (scope, element) {
            scope.$watch('action', function(action) {
                if (!_.isEmpty(action) && !action.empty) {

                    // Build the form

                    // Use this method over template as template has timing issues that encourage the browser to open
                    // new windows in entire new windows instead of new tabs
                    element.attr('action', action.uri)
                            .attr('method', action.method)
                            .attr('onsubmit', action.script)
                            .attr('target', action.target);

                    element.children('input').remove();

                    angular.forEach(action.params, function(value, key) {
                        this.append(angular.element('input')
                            .attr('type', 'hidden')
                            .attr('name', key)
                            .attr('value', value));
                    }, element);

                    // Submit form
                    element.submit();

                    // $timeout combats ng redraw issues when this tab looses focus; else quickly will not disappear
                    $timeout(function() {
                        scope.complete();
                    }, 100);

                }
            });
        }
    };
}]);

