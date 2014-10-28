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

/*global angular: false, quickly: false */
quickly.factory('UI', ['$timeout', function($timeout) {
    var RESET_ON_TOGGLE = true;

    /* Service Object */

    return  {

        init: function() {
            $timeout(function() { angular.element('#acs-commons-quickly').css('display', 'block'); }, 1000);
        },

        isResetOnToggle: function() {
            return RESET_ON_TOGGLE;
        },

        focusCommand: function () {
            var interval,
                el = angular.element('#acs-commons-quickly-cmd');

            el.focus();

            interval = setInterval(function() {
                if(el.get(0) !== document.activeElement) {
                    el.focus();
                } else {
                    clearInterval(interval);
                }
            }, 200);
        },

        scrollCommandInputToEnd: function () {
            var input = angular.element('#acs-commons-quickly-cmd');
            if (input) {
                $timeout(function () {
                    input.scrollLeft = input.scrollWidth;
                });
            }
        },

        injectForm: function (form) {
            var formWrapper = angular.element('#quickly-result-form');

            // Clear any existing form and replace w new form
            formWrapper.html('').append(form);

            return form;
        },

        resetResultScroll: function () {
            angular.element('#acs-commons-quickly-app .quickly-results').scrollTop(0);
        },

        scrollResults: function () {
            var container = angular.element('#acs-commons-quickly-app .quickly-results'),
                selected = angular.element('#acs-commons-quickly-app .quickly-result.selected'),
                containerHeight,
                containerTop,
                containerBottom,
                selectedHeight,
                selectedTop,
                selectedBottom;

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
        }
    };
}]);