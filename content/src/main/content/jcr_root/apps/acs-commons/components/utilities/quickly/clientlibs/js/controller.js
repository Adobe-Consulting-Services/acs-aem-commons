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

/*global quickly: false, angular: false, console: false */

quickly.controller('QuicklyCtrl',
        ['$scope', '$http', '$timeout', 'Action', 'Command', 'Init', 'Operations', 'Results', 'UI',
        function($scope, $http, $timeout, Action, Command, Init, Operations, Results, UI){

    $scope.app = {
        visible: false,
        timeout: 0,
        timeoutThrottle: 500
    };

    $scope.cmd = '';
    $scope.results = [];

    /* Watchers */
    $scope.$watch('cmd', function(newValue, oldValue) {

        if($scope.app.visible) {
            // Cancel any existing promises of result retrieval
            $timeout.cancel($scope.app.timeout);

            $scope.app.timeout = $timeout(function() {
                    $scope.app.getResults($scope.cmd);
                },
                $scope.app.timeoutThrottle
            );
        }
    });

    $scope.app.getResults = function(cmd) {
        Results.getResults(cmd).then(function(results) {
            $scope.results = results || [];
            UI.resetResultScroll();
        });
    };

    $scope.app.toggle = function() {
        if(UI.isResetOnToggle()) {
            $scope.cmd = '';
            $scope.results = [];
        }

        $scope.app.visible = !$scope.app.visible;
        if($scope.app.visible) {
            UI.focusCommand();
        }
    };

    /* Navigation */

    $scope.app.up = function() {
        Results.up($scope.results);
    };

    $scope.app.down = function() {
        Results.down($scope.results);
    };

    $scope.app.over = function(result) {
        Results.select(result, $scope.results);
    };

    /* Working Actions */

    $scope.app.right = function() {
       $scope.cmd = Action.right($scope.cmd, Results.getSelected($scope.results));
    };

    $scope.app.select = function() {
        if(!Action.select($scope.cmd, Results.getSelected($scope.results), $scope.results)) {
            $scope.app.toggle();
        }
    };

    /* Initialization */

    var init = function() {
        // Prevent flickering onload
        UI.init();

        // Initialize the app
        Init.init().then(function() {
            // Init all operations
            Operations.init();
        });

    };

    init();
}]);
