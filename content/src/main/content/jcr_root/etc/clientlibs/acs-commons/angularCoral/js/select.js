/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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
/*global angular: false, CUI: false */
angular.module('acsCoral')
    .directive("acsCoralSelect", function() {
        return {
            template: '<span class="coral-Select">' +
                        '<button type="button" class="coral-Select-button coral-MinimalButton">' +
                        '   <span class="coral-Select-button-text">{{ options[0].text }}</span>' +
                        '</button>' +
                        '<select name="{{name}}" ng-model="selectedValue" class="coral-Select-select">' +
                        '<option ng-repeat="option in options" value="{{ option.value }}">{{ option.text }}</option>' +
                        '</select>' +
                      '</span>',
            replace: false,
            restrict: 'A',
            scope: {
                options: '=',
                selectedValue: '=',
                name: '@'
            },
            link : function(scope, $element, attrs) {
                scope.$watchCollection('options', function(options) {
                    if (options && options.length > 0) {
                        $timeout(function() {
                            CUI.Select.init($element.find('.coral-Select'), null);
                        });

                    }
                });
            }
        };
    });