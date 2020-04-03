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

quickly.directive('init', [ '$document', '$timeout', function ($document, $timeout) {

    var isToggle = function(event) {
        if(event.ctrlKey && event.which === 0) {
            // ctrl-space (Chrome/Safari OSX)
            return true;
        } else if(event.ctrlKey && event.which === 32) {
            // ctrl-space (Chrome/Safari Windows)
            return true;
        } else if (event.shiftKey && event.ctrlKey && event.which === 64) {
            // shift-ctrl-space (FireFox OSX)
            return true;
        } else if (event.shiftKey && event.ctrlKey && event.which === 32) {
            // shift-ctrl-space (FireFox Windows)
            return true;
        } else {
            return false;
        }
    },

    isDismiss = function(event) {
        // Escape key
        return event.keyCode === 27;
    };

    return function(scope, element) {

        $document.on('keypress', function(event) {
            if(isToggle(event)) {
                scope.app.toggle();
                scope.$apply();

                event.preventDefault();
            }
        });

        $document.on('keydown', function(event) {
            if(scope.app.visible && isDismiss(event)) {
                scope.app.toggle();
                scope.$apply();

                event.preventDefault();
            }
        });

        $document.on('click', function(event) {
            if(scope.app.visible && !element[0].contains(event.target)) {
                scope.app.toggle(false);
                scope.$apply();
            }
        });

        // IFrame support

        $timeout(function() {
            angular.element('iframe').load(function() {
                var iframe = angular.element('iframe').contents().find('html');

                // Bubble keypress events to parent window html
                iframe.on('keydown keypress', function(event) {

                    if(isToggle(event) || isDismiss(event)) {
                        $document.trigger(event);
                    }
                });
            });
        }, 2000);

        // Stop "flickering" on page load (mark as CSS Block only after angular JS has loaded)
        element.css('display', 'block');
    };
}]);

