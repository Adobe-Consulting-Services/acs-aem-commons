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

/*global quickly: false, angular: false, _: false */

quickly.directive('commandInput', ['$interval', function ($interval) {
    return {
        restrict: 'A',
        link: function (scope, element) {

            scope.$watch('app.visible', function (visible) {
                var interval;

                if (visible) {
                    element.focus();

                    interval = $interval(function () {
                        if (element.get(0) !== document.activeElement) {
                            element.focus();
                        } else {
                            $interval.cancel(interval);
                        }
                    }, 200);

                    scope.$on('$destroy', function () {
                        $interval.cancel(interval);
                    });
                }
            });

            scope.$on('autocomplete', function (event, args) {
                _.defer(function () {
                    element.get(0).scrollLeft = element.get(0).scrollWidth;
                });
            });
        }
    };
}]);

