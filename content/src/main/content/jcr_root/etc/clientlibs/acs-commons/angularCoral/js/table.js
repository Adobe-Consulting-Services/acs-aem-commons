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
.directive("acsCoralTable", function() {
    
    return {
        restrict: 'A',
        scope: {
            tableStyle: '@'
        },
        link : function(scope, $element, attrs) {
            var tableStyleClass = scope.tableStyle ? 'coral-Table--' + scope.tableStyle : null;

            if ($element.prop('tagName') === 'table') {
                $element.addClass('coral-Table ' + tableStyleClass);
            } else {
                $element.find('table').addClass('coral-Table ' + tableStyleClass);
            }

            $element.find('thead tr').addClass('coral-Table-row');

            $element.find('thead tr th').addClass('coral-Table-headerCell');
            $element.find('tbody tr td').addClass('coral-Table-cell');
        }
    };
});