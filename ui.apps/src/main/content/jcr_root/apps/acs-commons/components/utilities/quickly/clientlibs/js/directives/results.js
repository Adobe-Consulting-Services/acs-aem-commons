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

quickly.directive('results', ['$interval', function ($interval) {
    return {
        restrict: 'A',
        link: function (scope, element) {

            scope.$watch('result', function () {

                var container,
                    selected,
                    containerHeight,
                    containerTop,
                    containerBottom,
                    selectedHeight,
                    selectedTop,
                    selectedBottom;

                // Wait for the results list to update with the new .selected element
                _.defer(function() {
                    container = element;
                    selected = element.find('.selected');

                    if (!selected || !selected.length) {
                        return;
                    }

                    containerHeight = container.height();
                    containerTop = container.scrollTop();
                    containerBottom = containerTop + containerHeight;

                    selectedHeight = selected.outerHeight(true);
                    selectedTop = selected.offset().top - container.offset().top + containerTop;
                    selectedBottom = selectedTop + selectedHeight;

                    if (selectedBottom > containerBottom) {
                        // Scroll down
                        container.scrollTop(selectedTop + selectedHeight - containerHeight);
                    } else if (selectedTop < containerTop) {
                        // Scroll Up
                        container.scrollTop(containerTop - selectedHeight);
                    }
                });
            });
        }
    };
}]);

