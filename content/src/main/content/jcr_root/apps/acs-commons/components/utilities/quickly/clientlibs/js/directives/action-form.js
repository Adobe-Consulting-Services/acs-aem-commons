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

quickly.directive('actionForm', ['$timeout', function ($timeout) {
    return {
        restrict: 'E',
        scope: {
            hide: '&',
            action: '=data'
        },
        template: '<form action="{{ action.uri }}" method="{{ action.method }}" target="{{ action.target }}" ng-onsubmit="{{ action.script }}" ng-hide="true">' +
            '<input ng-repeat="(name, value) in action.params" name="{{ name }}" value="{{ value }}" type="hidden"/>' +
        '</form>',
        replace: true,
        link: function (scope, element) {
            scope.$watch('action', function(action) {
                if (!_.isEmpty(action) && !action.empty) {
                    // Submit form
                    // _.defer to ensure the template has rendered before submitting the form
                    _.defer(function() {
                        element.submit();
                        scope.hide();
                    });

                }
            });
        }
    };
}]);

